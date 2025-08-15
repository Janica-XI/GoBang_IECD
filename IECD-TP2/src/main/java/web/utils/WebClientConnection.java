package web.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import communication.client.ClientProtocol;
import jakarta.servlet.http.HttpSession;

/**
 * Manages a persistent TCP connection between a web session and the GoBang game server.
 * 
 * This class handles the lifecycle of a socket connection for each HTTP session,
 * providing a bridge between the web application and the desktop game server protocol.
 * Each web session gets its own dedicated connection to maintain game state consistency.
 * 
 * Key responsibilities:
 * - Establishing and maintaining TCP connection to game server
 * - Managing background reader thread for server-pushed messages
 * - Providing thread-safe access to the communication protocol
 * - Graceful connection cleanup on session termination
 * 
 * Connection lifecycle:
 * 1. Constructor establishes socket connection with timeouts
 * 2. After successful login, startBackgroundReader() spawns message reader thread
 * 3. On logout/session end, terminateConnection() cleans up resources
 */
public class WebClientConnection {
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5025;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int THREAD_JOIN_TIMEOUT_MS = 1000;
    
    private final String sessionIdentifier;
    private final Socket gameServerSocket;
    private final ClientProtocol communicationProtocol;
    private final WebClientHandler messageHandler;
    
    private Thread backgroundReaderThread;
    private volatile boolean connectionActive;
    
    /**
     * Establishes a new connection to the game server for the given HTTP session.
     * Creates a TCP socket with appropriate timeouts and initializes the 
     * communication protocol. The connection is ready for login attempts after 
     * construction completes successfully.
     * 
     * @param httpSession the HTTP session this connection belongs to
     * @throws Exception if socket connection fails or I/O streams cannot be established
     */
    public WebClientConnection(HttpSession httpSession) throws Exception {
        this.sessionIdentifier = httpSession.getId();
        this.gameServerSocket = createConfiguredSocket();
        this.messageHandler = new WebClientHandler(httpSession);
        
        InputStream serverInputStream = gameServerSocket.getInputStream();
        OutputStream serverOutputStream = gameServerSocket.getOutputStream();
        this.communicationProtocol = new ClientProtocol(messageHandler, serverInputStream, serverOutputStream);
        
        this.connectionActive = true;
        WebLogger.logWithType(WebLogger.LogType.CONNECTION_ESTABLISHED, sessionIdentifier, null);
    }
    
    /**
     * Creates and configures a socket connection to the game server.
     * 
     * @return configured socket connected to the game server
     * @throws Exception if connection establishment fails
     */
    private Socket createConfiguredSocket() throws Exception {
        Socket socket = new Socket();
        socket.setSoTimeout(READ_TIMEOUT_MS);
        socket.connect(new java.net.InetSocketAddress(SERVER_HOST, SERVER_PORT), CONNECT_TIMEOUT_MS);
        return socket;
    }
    
    /**
     * Starts the background thread for continuous message reading from the server.
     * This method should be called after successful player login to begin processing
     * server-pushed messages like game invitations, moves, and lobby updates. 
     * If a reader thread is already running, this method returns immediately.
     */
    public void startBackgroundReader() {
        if (isReaderThreadActive()) {
            WebLogger.logWithType(WebLogger.LogType.READER_ALREADY_RUNNING, sessionIdentifier, null);
            return;
        }
        
        backgroundReaderThread = createReaderThread();
        backgroundReaderThread.setDaemon(true);
        backgroundReaderThread.start();
    }
    
    /**
     * Creates the background reader thread for processing server messages.
     * 
     * @return configured reader thread ready to start
     */
    private Thread createReaderThread() {
        return new Thread(this::runMessageReadingLoop, "GoBangReader-" + sessionIdentifier);
    }
    
    /**
     * Main loop for the background reader thread.
     * Continuously processes messages from the server until connection is terminated.
     */
    private void runMessageReadingLoop() {
        WebLogger.logWithType(WebLogger.LogType.READER_THREAD_STARTED, sessionIdentifier, null);
        
        try {
            while (isConnectionHealthy()) {
                processIncomingMessage();
            }
        } finally {
            handleReaderThreadTermination();
        }
    }
    
    /**
     * Processes a single incoming message from the server.
     * Handles socket timeouts gracefully as they are expected during normal operation.
     */
    private void processIncomingMessage() {
        try {
            WebLogger.logWithType(WebLogger.LogType.WAITING_FOR_MESSAGE, sessionIdentifier, null);
            communicationProtocol.receiveReplies();
            WebLogger.logWithType(WebLogger.LogType.MESSAGE_PROCESSED, sessionIdentifier, null);
        } catch (SocketTimeoutException timeoutException) {
            WebLogger.logWithType(WebLogger.LogType.NORMAL_TIMEOUT, sessionIdentifier, null);
        } catch (Exception exception) {
            handleMessageProcessingError(exception);
        }
    }
    
    /**
     * Handles errors during message processing.
     * Distinguishes between wrapped timeout exceptions and genuine connection errors.
     * 
     * @param exception the exception that occurred during message processing
     */
    private void handleMessageProcessingError(Exception exception) {
        if (isWrappedTimeout(exception)) {
            WebLogger.logWithType(WebLogger.LogType.WRAPPED_TIMEOUT, sessionIdentifier, null);
            return;
        }
        
        if (connectionActive) {
            WebLogger.logWithType(WebLogger.LogType.CONNECTION_ERROR, sessionIdentifier, exception);
            if (shouldPrintStackTrace(exception)) {
                exception.printStackTrace();
            }
        }
        
        connectionActive = false;
    }
    
    /**
     * Determines if an exception is a wrapped SocketTimeoutException.
     * 
     * @param exception the exception to check
     * @return true if this is a wrapped timeout exception
     */
    private boolean isWrappedTimeout(Exception exception) {
        return exception.getCause() instanceof SocketTimeoutException;
    }
    
    /**
     * Determines if a full stack trace should be printed for the given exception.
     * Suppresses stack traces for common session-related errors to reduce log noise.
     * 
     * @param exception the exception to evaluate
     * @return true if stack trace should be printed
     */
    private boolean shouldPrintStackTrace(Exception exception) {
        String message = exception.getMessage();
        return message != null && 
               !message.contains("Session") && 
               !message.contains("invalidated");
    }
    
    /**
     * Handles cleanup when the reader thread terminates.
     */
    private void handleReaderThreadTermination() {
        WebLogger.logWithType(WebLogger.LogType.READER_THREAD_ENDING, sessionIdentifier, null);
        connectionActive = false;
    }
    
    /**
     * Terminates the connection and cleans up all associated resources.
     * This method should be called when the HTTP session ends or the user logs out.
     * It safely closes the socket connection and stops the background reader thread.
     * The method is idempotent - multiple calls are safe.
     */
    public void terminateConnection() {
        WebLogger.logWithType(WebLogger.LogType.CONNECTION_TERMINATION, sessionIdentifier, null);
        connectionActive = false;
        
        closeSocketSafely();
        stopReaderThreadSafely();
    }
    
    /**
     * Safely closes the socket connection, handling any I/O exceptions.
     */
    private void closeSocketSafely() {
        if (gameServerSocket != null && !gameServerSocket.isClosed()) {
            try {
                gameServerSocket.close();
            } catch (Exception exception) {
                WebLogger.logWithType(WebLogger.LogType.SOCKET_CLOSE_ERROR, sessionIdentifier, exception);
            }
        }
    }
    
    /**
     * Safely stops the background reader thread with a timeout.
     */
    private void stopReaderThreadSafely() {
        if (!isReaderThreadActive()) {
            return;
        }
        
        backgroundReaderThread.interrupt();
        
        try {
            backgroundReaderThread.join(THREAD_JOIN_TIMEOUT_MS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Checks if the connection is healthy and active.
     * 
     * @return true if connection is active and socket is open
     */
    private boolean isConnectionHealthy() {
        return connectionActive && gameServerSocket != null && !gameServerSocket.isClosed();
    }
    
    /**
     * Checks if the reader thread is currently active.
     * 
     * @return true if reader thread exists and is running
     */
    private boolean isReaderThreadActive() {
        return backgroundReaderThread != null && backgroundReaderThread.isAlive();
    }
    
    /**
     * Returns the communication protocol for sending messages to the server.
     * The returned protocol can be used to send login requests, game moves,
     * and other client-to-server messages.
     * 
     * @return the client protocol for server communication
     */
    public ClientProtocol getCommunicationProtocol() {
        return communicationProtocol;
    }
    
    /**
     * Returns the unique identifier of the HTTP session this connection serves.
     * 
     * @return the session ID string
     */
    public String getSessionIdentifier() {
        return sessionIdentifier;
    }
    
    /**
     * Checks if the connection is currently active and usable.
     * 
     * @return true if connection is active and socket is open
     */
    public boolean isConnectionActive() {
        return isConnectionHealthy();
    }
}
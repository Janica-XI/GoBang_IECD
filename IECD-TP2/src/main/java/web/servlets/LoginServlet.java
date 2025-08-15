package web.servlets;

import java.io.IOException;

import core.Player;
import core.client.ClientUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import web.utils.WebClientConnection;
import web.utils.WebConnectionManager;
import web.utils.WebLogger;

/**
 * Handles user authentication for the GoBang web application.
 * 
 * This servlet manages both the display of the login form and the processing
 * of login credentials. It establishes connections to the game server,
 * authenticates users, and manages session state for successful logins.
 * 
 * GET requests:
 * - Displays the login form (login.jsp)
 * 
 * POST requests:
 * - Processes login credentials against the game server
 * - Establishes WebClientConnection for authenticated users
 * - Starts background message reader threads for real-time communication
 * - Redirects to lobby on success or back to login form with errors
 * 
 * The servlet follows a secure authentication pattern:
 * 1. Validate input parameters
 * 2. Establish server connection
 * 3. Send authentication request to game server
 * 4. Process server response
 * 5. Set up session state and background communications on success
 * 6. Handle errors gracefully with user-friendly messages
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final String LOGIN_VIEW = "login.jsp";
    private static final String LOBBY_REDIRECT_PATH = "lobby";
    
    private static final String PARAM_USERNAME = "username";
    private static final String PARAM_PASSWORD = "password";
    
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_LAST_LOGIN_STATUS = "lastLoginStatus";
    private static final String ATTR_LAST_LOGIN_PROFILE = "lastLoginProfile";
    private static final String ATTR_LOGGED_IN_PLAYER = "loggedInPlayer";
    private static final String ATTR_USERNAME = "username";
    
    private static final String STATUS_ACCEPTED = "Accepted";
    private static final String DEFAULT_ERROR_MESSAGE = "Connection error. Please try again.";

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String sessionIdentifier = getSessionIdentifier(httpRequest);
        WebLogger.info("LoginServlet", sessionIdentifier, "Login form requested");
        
        forwardToLoginView(httpRequest, httpResponse);
    }

    @Override
    protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String username = httpRequest.getParameter(PARAM_USERNAME);
        String password = httpRequest.getParameter(PARAM_PASSWORD);
        String sessionIdentifier = getSessionIdentifier(httpRequest);
        
        WebLogger.info("LoginServlet", sessionIdentifier, "Login attempt for username: " + username);

        if (!areCredentialsValid(username, password)) {
            handleInvalidCredentials(httpRequest, httpResponse, sessionIdentifier);
            return;
        }

        try {
            processLoginAttempt(username, password, httpRequest, httpResponse, sessionIdentifier);
        } catch (Exception exception) {
            handleLoginException(username, sessionIdentifier, exception, httpRequest, httpResponse);
        }
    }

    /**
     * Processes a complete login attempt including server communication and session setup.
     * 
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if any step of the login process fails
     */
    private void processLoginAttempt(String username, String password, HttpServletRequest httpRequest, 
                                   HttpServletResponse httpResponse, String sessionIdentifier) 
                                   throws Exception {
        
        WebClientConnection serverConnection = establishServerConnection(httpRequest, sessionIdentifier);
        clearPreviousLoginData(httpRequest.getSession());
        sendLoginRequestToServer(serverConnection, username, password, sessionIdentifier);
        receiveServerResponse(serverConnection, username, sessionIdentifier);
        
        String loginStatus = (String) httpRequest.getSession().getAttribute(ATTR_LAST_LOGIN_STATUS);
        Player playerProfile = (Player) httpRequest.getSession().getAttribute(ATTR_LAST_LOGIN_PROFILE);
        
        if (isLoginSuccessful(loginStatus, playerProfile)) {
            handleSuccessfulLogin(username, playerProfile, httpRequest, httpResponse, sessionIdentifier);
        } else {
            handleFailedLogin(username, loginStatus, httpRequest, httpResponse, sessionIdentifier);
        }
    }

    /**
     * Establishes a connection to the game server for authentication.
     * 
     * @param httpRequest the HTTP request containing session information
     * @param sessionIdentifier the session ID for logging
     * @return the established WebClientConnection
     * @throws Exception if connection cannot be established
     */
    private WebClientConnection establishServerConnection(HttpServletRequest httpRequest, String sessionIdentifier) 
            throws Exception {
        
        WebLogger.info("LoginServlet", sessionIdentifier, "Establishing server connection");
        WebClientConnection connection = WebConnectionManager.getOrCreateConnection(httpRequest.getSession());
        
        if (!connection.isConnectionActive()) {
            throw new Exception("No connection to server");
        }
        
        WebLogger.info("LoginServlet", sessionIdentifier, "Server connection established");
        return connection;
    }

    /**
     * Clears any previous login data from the session.
     * 
     * @param userSession the user's HTTP session
     */
    private void clearPreviousLoginData(HttpSession userSession) {
        userSession.removeAttribute(ATTR_LAST_LOGIN_STATUS);
        userSession.removeAttribute(ATTR_LAST_LOGIN_PROFILE);
    }

    /**
     * Sends the login request to the game server.
     * 
     * @param connection the server connection
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if the request cannot be sent
     */
    private void sendLoginRequestToServer(WebClientConnection connection, String username, String password, 
                                        String sessionIdentifier) throws Exception {
        
        WebLogger.info("LoginServlet", sessionIdentifier, "Sending login request for: " + username);
        connection.getCommunicationProtocol().sendLoginRequest(username, password);
        WebLogger.info("LoginServlet", sessionIdentifier, "Login request sent for: " + username);
    }

    /**
     * Receives and processes the server response to the login request.
     * 
     * @param connection the server connection
     * @param username the username being authenticated
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if the response cannot be received
     */
    private void receiveServerResponse(WebClientConnection connection, String username, String sessionIdentifier) 
            throws Exception {
        
        WebLogger.info("LoginServlet", sessionIdentifier, "Waiting for server response for: " + username);
        connection.getCommunicationProtocol().receiveReplies();
        WebLogger.info("LoginServlet", sessionIdentifier, "Server response received for: " + username);
    }

    /**
     * Checks if the login attempt was successful based on server response.
     * 
     * @param loginStatus the status returned by the server
     * @param playerProfile the player profile returned by the server
     * @return true if login was successful, false otherwise
     */
    private boolean isLoginSuccessful(String loginStatus, Player playerProfile) {
        return STATUS_ACCEPTED.equals(loginStatus) && playerProfile != null;
    }

    /**
     * Handles successful login by setting up session state and starting background communication.
     * 
     * @param username the authenticated username
     * @param playerProfile the player's profile data
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if redirect fails
     */
    private void handleSuccessfulLogin(String username, Player playerProfile, HttpServletRequest httpRequest, 
                                     HttpServletResponse httpResponse, String sessionIdentifier) 
                                     throws IOException {
        
        setupAuthenticatedSession(username, playerProfile, httpRequest.getSession());
        startBackgroundCommunication(httpRequest.getSession(), sessionIdentifier);
        redirectToLobby(httpResponse, username, sessionIdentifier);
    }

    /**
     * Sets up the session state for an authenticated user.
     * 
     * @param username the authenticated username
     * @param playerProfile the player's profile data
     * @param userSession the user's HTTP session
     */
    private void setupAuthenticatedSession(String username, Player playerProfile, HttpSession userSession) {
        userSession.setAttribute(ATTR_LOGGED_IN_PLAYER, playerProfile);
        userSession.setAttribute(ATTR_USERNAME, username);
    }

    /**
     * Starts background communication threads for real-time server messaging.
     * 
     * @param userSession the user's HTTP session
     * @param sessionIdentifier the session ID for logging
     */
    private void startBackgroundCommunication(HttpSession userSession, String sessionIdentifier) {
        WebLogger.info("LoginServlet", sessionIdentifier, "Starting background message reader");
        WebConnectionManager.startBackgroundMessageReader(userSession);
    }

    /**
     * Redirects successfully authenticated users to the lobby.
     * 
     * @param httpResponse the HTTP response object
     * @param username the authenticated username
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if redirect fails
     */
    private void redirectToLobby(HttpServletResponse httpResponse, String username, String sessionIdentifier) 
            throws IOException {
        
        WebLogger.info("LoginServlet", sessionIdentifier, "Login successful for: " + username + " - redirecting to lobby");
        httpResponse.sendRedirect(LOBBY_REDIRECT_PATH);
    }

    /**
     * Handles failed login attempts by displaying appropriate error messages.
     * 
     * @param username the username that failed authentication
     * @param loginStatus the failure status from the server
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void handleFailedLogin(String username, String loginStatus, HttpServletRequest httpRequest, 
                                 HttpServletResponse httpResponse, String sessionIdentifier) 
                                 throws ServletException, IOException {
        
        WebLogger.warning("LoginServlet", sessionIdentifier, "Login failed for: " + username + " - " + loginStatus);
        
        String userFriendlyErrorMessage = createUserFriendlyErrorMessage(loginStatus);
        httpRequest.setAttribute(ATTR_ERROR, userFriendlyErrorMessage);
        
        forwardToLoginView(httpRequest, httpResponse);
    }

    /**
     * Creates a user-friendly error message from the server status.
     * 
     * @param loginStatus the status returned by the server
     * @return user-friendly error message
     */
    private String createUserFriendlyErrorMessage(String loginStatus) {
        if (loginStatus != null) {
            return ClientUtils.getFriendlyStatusMessage(loginStatus);
        }
        return "Login error";
    }

    /**
     * Handles exceptions that occur during the login process.
     * 
     * @param username the username being authenticated
     * @param sessionIdentifier the session ID for logging
     * @param exception the exception that occurred
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void handleLoginException(String username, String sessionIdentifier, Exception exception, 
                                    HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
                                    throws ServletException, IOException {
        
        WebLogger.error("LoginServlet", sessionIdentifier, "Login error for " + username, exception);
        
        cleanupFailedConnection(httpRequest.getSession(), sessionIdentifier);
        displayConnectionError(httpRequest, httpResponse);
    }

    /**
     * Cleans up connection resources after a failed login attempt.
     * 
     * @param userSession the user's HTTP session
     * @param sessionIdentifier the session ID for logging
     */
    private void cleanupFailedConnection(HttpSession userSession, String sessionIdentifier) {
        WebLogger.info("LoginServlet", sessionIdentifier, "Cleaning up failed connection");
        WebConnectionManager.removeAndTerminateConnection(userSession);
    }

    /**
     * Displays a connection error to the user.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void displayConnectionError(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws ServletException, IOException {
        
        httpRequest.setAttribute(ATTR_ERROR, DEFAULT_ERROR_MESSAGE);
        forwardToLoginView(httpRequest, httpResponse);
    }

    /**
     * Validates that the provided credentials are not null or empty.
     * 
     * @param username the username to validate
     * @param password the password to validate
     * @return true if credentials are valid, false otherwise
     */
    private boolean areCredentialsValid(String username, String password) {
        return username != null && !username.trim().isEmpty() && 
               password != null && !password.trim().isEmpty();
    }

    /**
     * Handles the case where invalid credentials were provided.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void handleInvalidCredentials(HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
                                        String sessionIdentifier) throws ServletException, IOException {
        
        WebLogger.warning("LoginServlet", sessionIdentifier, "Invalid credentials provided");
        httpRequest.setAttribute(ATTR_ERROR, "Username and password are required");
        forwardToLoginView(httpRequest, httpResponse);
    }

    /**
     * Forwards the request to the login view.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void forwardToLoginView(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws ServletException, IOException {
        
        httpRequest.getRequestDispatcher(LOGIN_VIEW).forward(httpRequest, httpResponse);
    }

    /**
     * Gets the session identifier for logging purposes.
     * 
     * @param httpRequest the HTTP request object
     * @return the session identifier
     */
    private String getSessionIdentifier(HttpServletRequest httpRequest) {
        return httpRequest.getSession().getId();
    }
}
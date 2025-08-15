package web.utils;

/**
 * Centralized logging utility for the web application.
 * Provides consistent debug output control across all web components.
 * All logging can be globally enabled/disabled via WebDebugConfig.DEBUG.
 */
public class WebLogger {
    
    /**
     * Logs an informational message if debugging is enabled.
     * 
     * @param component the component/class name generating the log
     * @param message the message to log
     */
    public static void info(String component, String message) {
        if (WebDebugConfig.DEBUG) {
            System.out.println("[" + component + "] " + message);
        }
    }
    
    /**
     * Logs an informational message with session context if debugging is enabled.
     * 
     * @param component the component/class name generating the log
     * @param sessionId the session identifier for context
     * @param message the message to log
     */
    public static void info(String component, String sessionId, String message) {
        if (WebDebugConfig.DEBUG) {
            System.out.println("[" + component + "][Session:" + sessionId + "] " + message);
        }
    }
    
    /**
     * Logs an error message if debugging is enabled.
     * 
     * @param component the component/class name generating the log
     * @param message the error message to log
     */
    public static void error(String component, String message) {
        if (WebDebugConfig.DEBUG) {
            System.err.println("[" + component + "] ERROR: " + message);
        }
    }
    
    /**
     * Logs an error message with session context if debugging is enabled.
     * 
     * @param component the component/class name generating the log
     * @param sessionId the session identifier for context
     * @param message the error message to log
     */
    public static void error(String component, String sessionId, String message) {
        if (WebDebugConfig.DEBUG) {
            System.err.println("[" + component + "][Session:" + sessionId + "] ERROR: " + message);
        }
    }
    
    /**
     * Logs an error message with exception details if debugging is enabled.
     * 
     * @param component the component/class name generating the log
     * @param message the error message to log
     * @param exception the exception that occurred
     */
    public static void error(String component, String message, Exception exception) {
        if (WebDebugConfig.DEBUG) {
            System.err.println("[" + component + "] ERROR: " + message + 
                             (exception != null ? " - " + exception.getMessage() : ""));
        }
    }
    
    /**
     * Logs an error message with session context and exception details if debugging is enabled.
     * 
     * @param component the component/class name generating the log
     * @param sessionId the session identifier for context
     * @param message the error message to log
     * @param exception the exception that occurred
     */
    public static void error(String component, String sessionId, String message, Exception exception) {
        if (WebDebugConfig.DEBUG) {
            System.err.println("[" + component + "][Session:" + sessionId + "] ERROR: " + message + 
                             (exception != null ? " - " + exception.getMessage() : ""));
        }
    }
    
    /**
     * Logs a warning message if debugging is enabled.
     * 
     * @param component the component/class name generating the log
     * @param message the warning message to log
     */
    public static void warning(String component, String message) {
        if (WebDebugConfig.DEBUG) {
            System.out.println("[" + component + "] WARNING: " + message);
        }
    }
    
    /**
     * Logs a warning message with session context if debugging is enabled.
     * 
     * @param component the component/class name generating the log
     * @param sessionId the session identifier for context
     * @param message the warning message to log
     */
    public static void warning(String component, String sessionId, String message) {
        if (WebDebugConfig.DEBUG) {
            System.out.println("[" + component + "][Session:" + sessionId + "] WARNING: " + message);
        }
    }
    
    /**
     * Logs debug information about connection events.
     * Specialized method for connection-related logging.
     * 
     * @param sessionId the session identifier
     * @param event the connection event description
     */
    public static void connection(String sessionId, String event) {
        if (WebDebugConfig.DEBUG) {
            System.out.println("[CONNECTION][Session:" + sessionId + "] " + event);
        }
    }
    
    /**
     * Logs debug information about thread events.
     * Specialized method for thread-related logging.
     * 
     * @param threadName the name of the thread
     * @param sessionId the session identifier for context
     * @param event the thread event description
     */
    public static void thread(String threadName, String sessionId, String event) {
        if (WebDebugConfig.DEBUG) {
            System.out.println("[THREAD:" + threadName + "][Session:" + sessionId + "] " + event);
        }
    }
    
    /**
     * Logs debug information about protocol messages.
     * Specialized method for protocol-related logging.
     * 
     * @param sessionId the session identifier
     * @param direction "SENT" or "RECEIVED"
     * @param messageType the type of protocol message
     */
    public static void protocol(String sessionId, String direction, String messageType) {
        if (WebDebugConfig.DEBUG) {
            System.out.println("[PROTOCOL][Session:" + sessionId + "] " + direction + " " + messageType);
        }
    }
    
    /**
     * Generic logging method that handles specific log types with context.
     * Allows components to use predefined log formats while maintaining consistency.
     * 
     * @param logType the specific type of log message
     * @param sessionId the session identifier for context
     * @param exception optional exception for error logging, can be null
     */
    public static void logWithType(LogType logType, String sessionId, Exception exception) {
        switch (logType) {
            case CONNECTION_ESTABLISHED:
                connection(sessionId, "WebClientConnection established");
                break;
            case CONNECTION_TERMINATION:
                connection(sessionId, "WebClientConnection terminating");
                break;
            case READER_ALREADY_RUNNING:
                thread("BackgroundReader", sessionId, "Already active");
                break;
            case READER_THREAD_STARTED:
                thread("BackgroundReader", sessionId, "Started");
                break;
            case READER_THREAD_ENDING:
                thread("BackgroundReader", sessionId, "Ending");
                break;
            case WAITING_FOR_MESSAGE:
                thread("BackgroundReader", sessionId, "Waiting for message");
                break;
            case MESSAGE_PROCESSED:
                thread("BackgroundReader", sessionId, "Message processed");
                break;
            case NORMAL_TIMEOUT:
                thread("BackgroundReader", sessionId, "Timeout (normal)");
                break;
            case WRAPPED_TIMEOUT:
                thread("BackgroundReader", sessionId, "Timeout (wrapped)");
                break;
            case CONNECTION_ERROR:
                error("WebClientConnection", sessionId, "Background reader error", exception);
                break;
            case SOCKET_CLOSE_ERROR:
                error("WebClientConnection", sessionId, "Error closing socket", exception);
                break;
        }
    }
    
    /**
     * Enum defining specific log message types for WebClientConnection logging.
     * Can be extended with additional types as needed by other components.
     */
    public enum LogType {
        CONNECTION_ESTABLISHED,
        CONNECTION_TERMINATION,
        READER_ALREADY_RUNNING,
        READER_THREAD_STARTED,
        READER_THREAD_ENDING,
        WAITING_FOR_MESSAGE,
        MESSAGE_PROCESSED,
        NORMAL_TIMEOUT,
        WRAPPED_TIMEOUT,
        CONNECTION_ERROR,
        SOCKET_CLOSE_ERROR
    }
}
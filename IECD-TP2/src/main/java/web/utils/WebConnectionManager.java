package web.utils;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpSession;

/**
 * Manages WebClientConnection instances and their lifecycles for each HTTP session.
 * 
 * This class provides centralized management of TCP connections between web sessions
 * and the GoBang game server. It ensures each session has at most one connection
 * and handles proper cleanup when sessions terminate.
 * 
 * Key responsibilities:
 * - Creating and caching connections per session
 * - Managing connection lifecycle (create, retrieve, cleanup)
 * - Starting background reader threads after successful login
 * - Thread-safe operations using ConcurrentHashMap
 */
public class WebConnectionManager {
    
    private static final ConcurrentHashMap<String, WebClientConnection> activeConnections = new ConcurrentHashMap<>();
    
    /**
     * Retrieves an existing connection or creates a new one for the given session.
     * If a connection already exists for this session, returns the existing one.
     * Otherwise, creates a new connection and caches it for future use.
     * 
     * @param httpSession the HTTP session requesting a connection
     * @return the WebClientConnection for this session
     * @throws Exception if connection creation fails
     */
    public static WebClientConnection getOrCreateConnection(HttpSession httpSession) throws Exception {
        String sessionIdentifier = httpSession.getId();
        
        WebClientConnection existingConnection = activeConnections.get(sessionIdentifier);
        if (existingConnection != null) {
            WebLogger.info("WebConnectionManager", sessionIdentifier, "Returning existing connection");
            return existingConnection;
        }
        
        WebLogger.info("WebConnectionManager", sessionIdentifier, "Creating new connection");
        WebClientConnection newConnection = new WebClientConnection(httpSession);
        activeConnections.put(sessionIdentifier, newConnection);
        
        WebLogger.info("WebConnectionManager", sessionIdentifier, "Connection created and cached");
        return newConnection;
    }
    
    /**
     * Retrieves an existing connection for the given session without creating a new one.
     * Returns null if no connection exists for this session.
     * 
     * @param httpSession the HTTP session to look up
     * @return the existing WebClientConnection or null if none exists
     */
    public static WebClientConnection getExistingConnection(HttpSession httpSession) {
        String sessionIdentifier = httpSession.getId();
        WebClientConnection connection = activeConnections.get(sessionIdentifier);
        
        if (connection != null) {
            WebLogger.info("WebConnectionManager", sessionIdentifier, "Retrieved existing connection");
        } else {
            WebLogger.info("WebConnectionManager", sessionIdentifier, "No existing connection found");
        }
        
        return connection;
    }
    
    /**
     * Removes and terminates the connection for the given session.
     * This method should be called when a user logs out or the session expires.
     * It safely closes the socket and stops any background threads.
     * 
     * @param httpSession the HTTP session whose connection should be removed
     */
    public static void removeAndTerminateConnection(HttpSession httpSession) {
        String sessionIdentifier = httpSession.getId();
        WebClientConnection connection = activeConnections.remove(sessionIdentifier);
        
        if (connection != null) {
            WebLogger.info("WebConnectionManager", sessionIdentifier, "Removing and terminating connection");
            connection.terminateConnection();
            WebLogger.info("WebConnectionManager", sessionIdentifier, "Connection terminated successfully");
        } else {
            WebLogger.info("WebConnectionManager", sessionIdentifier, "No connection to remove");
        }
    }
    
    /**
     * Starts the background message reader thread for the given session.
     * This method should be called after successful user login to begin processing
     * server-pushed messages like game invitations and moves.
     * 
     * @param httpSession the HTTP session whose connection should start reading
     */
    public static void startBackgroundMessageReader(HttpSession httpSession) {
        String sessionIdentifier = httpSession.getId();
        WebClientConnection connection = activeConnections.get(sessionIdentifier);
        
        if (connection != null) {
            WebLogger.info("WebConnectionManager", sessionIdentifier, "Starting background message reader");
            connection.startBackgroundReader();
        } else {
            WebLogger.warning("WebConnectionManager", sessionIdentifier, "Cannot start reader - no connection exists");
        }
    }
    
    /**
     * Returns the number of currently active connections.
     * Useful for monitoring and debugging purposes.
     * 
     * @return the count of active connections
     */
    public static int getActiveConnectionCount() {
        int count = activeConnections.size();
        WebLogger.info("WebConnectionManager", "Active connections count: " + count);
        return count;
    }
    
    /**
     * Checks if a connection exists for the given session.
     * 
     * @param httpSession the HTTP session to check
     * @return true if a connection exists for this session
     */
    public static boolean hasActiveConnection(HttpSession httpSession) {
        String sessionIdentifier = httpSession.getId();
        boolean hasConnection = activeConnections.containsKey(sessionIdentifier);
        
        WebLogger.info("WebConnectionManager", sessionIdentifier, 
                      "Connection exists: " + hasConnection);
        return hasConnection;
    }
    
    /**
     * Performs cleanup of all connections.
     * This method should be called during application shutdown to ensure
     * all connections are properly closed and resources are released.
     */
    public static void shutdownAllConnections() {
        WebLogger.info("WebConnectionManager", "Shutting down all connections (" + activeConnections.size() + ")");
        
        activeConnections.forEach((sessionId, connection) -> {
            WebLogger.info("WebConnectionManager", sessionId, "Shutting down connection");
            connection.terminateConnection();
        });
        
        activeConnections.clear();
        WebLogger.info("WebConnectionManager", "All connections shutdown complete");
    }
}
package web.servlets.api;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import web.utils.WebConnectionManager;
import web.utils.WebLogger;

/**
 * Handles user logout requests and session cleanup.
 * 
 * This servlet processes logout requests by properly closing the connection
 * to the game server, invalidating the HTTP session, and redirecting the user
 * back to the login page. It ensures all resources are cleaned up properly
 * to prevent memory leaks and connection issues.
 * 
 * The logout process follows these steps:
 * 1. Retrieve the current session and log the logout attempt
 * 2. Remove and terminate the WebClientConnection for this session
 * 3. Invalidate the HTTP session to clear all session data
 * 4. Redirect the user to the login page
 * 
 * Supports only GET requests as logout is typically triggered by navigation.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Handles logout GET requests by cleaning up connections and invalidating sessions.
     * 
     * @param httpRequest the HTTP request containing session information
     * @param httpResponse the HTTP response for sending redirects
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail during redirect
     */
    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {
        
        HttpSession userSession = httpRequest.getSession(false);
        
        if (userSession == null) {
            handleNoActiveSession(httpResponse);
            return;
        }
        
        String sessionIdentifier = userSession.getId();
        WebLogger.info("LogoutServlet", sessionIdentifier, "Logout request received");
        
        performLogoutCleanup(userSession);
        invalidateUserSession(userSession, sessionIdentifier);
        redirectToLoginPage(httpResponse, sessionIdentifier);
    }
    
    /**
     * Handles the case where no active session exists during logout attempt.
     * 
     * @param httpResponse the HTTP response for sending redirects
     * @throws IOException if redirect fails
     */
    private void handleNoActiveSession(HttpServletResponse httpResponse) throws IOException {
        WebLogger.info("LogoutServlet", "No active session found during logout attempt");
        httpResponse.sendRedirect("login");
    }
    
    /**
     * Performs all necessary cleanup operations for logout.
     * This includes removing and terminating the WebClientConnection.
     * 
     * @param userSession the session to clean up
     */
    private void performLogoutCleanup(HttpSession userSession) {
        String sessionIdentifier = userSession.getId();
        
        try {
            WebLogger.info("LogoutServlet", sessionIdentifier, "Starting connection cleanup");
            WebConnectionManager.removeAndTerminateConnection(userSession);
            WebLogger.info("LogoutServlet", sessionIdentifier, "Connection cleanup completed");
        } catch (Exception exception) {
            WebLogger.error("LogoutServlet", sessionIdentifier, "Error during connection cleanup", exception);
        }
    }
    
    /**
     * Invalidates the user session and logs the operation.
     * 
     * @param userSession the session to invalidate
     * @param sessionIdentifier the session ID for logging purposes
     */
    private void invalidateUserSession(HttpSession userSession, String sessionIdentifier) {
        try {
            WebLogger.info("LogoutServlet", sessionIdentifier, "Invalidating session");
            userSession.invalidate();
            WebLogger.info("LogoutServlet", sessionIdentifier, "Session invalidated successfully");
        } catch (Exception exception) {
            WebLogger.error("LogoutServlet", sessionIdentifier, "Error during session invalidation", exception);
        }
    }
    
    /**
     * Redirects the user to the login page after successful logout.
     * 
     * @param httpResponse the HTTP response for sending redirects
     * @param sessionIdentifier the session ID for logging purposes
     * @throws IOException if redirect fails
     */
    private void redirectToLoginPage(HttpServletResponse httpResponse, String sessionIdentifier) throws IOException {
        try {
            WebLogger.info("LogoutServlet", sessionIdentifier, "Redirecting to login page");
            httpResponse.sendRedirect("login");
            WebLogger.info("LogoutServlet", sessionIdentifier, "Logout process completed successfully");
        } catch (IOException exception) {
            WebLogger.error("LogoutServlet", sessionIdentifier, "Error during redirect to login", exception);
            throw exception;
        }
    }
}
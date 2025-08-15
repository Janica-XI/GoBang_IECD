package web.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import web.utils.WebLogger;

/**
 * Main entry point servlet for the GoBang web application.
 * 
 * This servlet acts as the application's homepage and routing dispatcher,
 * determining where users should be directed based on their authentication status.
 * It provides a simple but essential function of checking if a user is already
 * logged in and routing them to the appropriate page.
 * 
 * Routing logic:
 * - If user is authenticated (has loggedInPlayer in session): redirect to lobby
 * - If user is not authenticated: redirect to login page
 * 
 * This follows the standard web application pattern of protecting authenticated
 * areas while providing a seamless user experience. Users can bookmark the
 * index page and always be taken to the correct location based on their state.
 */
@WebServlet("/index")
public class IndexServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final String LOBBY_REDIRECT_PATH = "lobby";
    private static final String LOGIN_REDIRECT_PATH = "login";
    private static final String LOGGED_IN_PLAYER_ATTRIBUTE = "loggedInPlayer";

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        HttpSession userSession = httpRequest.getSession(false);
        String sessionIdentifier = userSession != null ? userSession.getId() : "no-session";
        
        WebLogger.info("IndexServlet", sessionIdentifier, "Index page accessed");

        try {
            if (isUserAuthenticated(userSession)) {
                redirectToLobby(httpResponse, sessionIdentifier);
            } else {
                redirectToLogin(httpResponse, sessionIdentifier);
            }
        } catch (Exception exception) {
            handleRedirectException(sessionIdentifier, exception, httpResponse);
        }
    }

    /**
     * Checks if the user is currently authenticated.
     * 
     * @param userSession the user's HTTP session, may be null
     * @return true if user is authenticated, false otherwise
     */
    private boolean isUserAuthenticated(HttpSession userSession) {
        if (userSession == null) {
            return false;
        }
        
        Object loggedInPlayer = userSession.getAttribute(LOGGED_IN_PLAYER_ATTRIBUTE);
        return loggedInPlayer != null;
    }

    /**
     * Redirects authenticated users to the lobby page.
     * 
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if redirect fails
     */
    private void redirectToLobby(HttpServletResponse httpResponse, String sessionIdentifier) throws IOException {
        WebLogger.info("IndexServlet", sessionIdentifier, "User authenticated - redirecting to lobby");
        httpResponse.sendRedirect(LOBBY_REDIRECT_PATH);
    }

    /**
     * Redirects unauthenticated users to the login page.
     * 
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if redirect fails
     */
    private void redirectToLogin(HttpServletResponse httpResponse, String sessionIdentifier) throws IOException {
        WebLogger.info("IndexServlet", sessionIdentifier, "User not authenticated - redirecting to login");
        httpResponse.sendRedirect(LOGIN_REDIRECT_PATH);
    }

    /**
     * Handles exceptions that occur during redirect operations.
     * 
     * @param sessionIdentifier the session ID for logging
     * @param exception the exception that occurred
     * @param httpResponse the HTTP response object
     * @throws IOException if error response cannot be sent
     */
    private void handleRedirectException(String sessionIdentifier, Exception exception, 
                                        HttpServletResponse httpResponse) throws IOException {
        WebLogger.error("IndexServlet", sessionIdentifier, "Error during index redirect", exception);
        
        // Fallback: try to redirect to login page
        try {
            httpResponse.sendRedirect(LOGIN_REDIRECT_PATH);
        } catch (IOException fallbackException) {
            WebLogger.error("IndexServlet", sessionIdentifier, "Fallback redirect also failed", fallbackException);
            throw fallbackException;
        }
    }
}
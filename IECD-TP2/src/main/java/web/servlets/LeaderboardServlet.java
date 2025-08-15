package web.servlets;

import java.io.IOException;
import java.util.List;

import core.Player;
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
 * Handles leaderboard display and data retrieval for the GoBang web application.
 * 
 * This servlet manages the leaderboard functionality, displaying player rankings
 * based on their game statistics. It communicates with the game server to fetch
 * the most current leaderboard data and presents it in a user-friendly format.
 * 
 * The leaderboard shows players ranked by their performance metrics including
 * victories, defeats, and total play time. This provides users with insight
 * into the competitive landscape and their own standing within the community.
 * 
 * GET requests:
 * - Displays the leaderboard interface (leaderboard.jsp)
 * - Requests fresh leaderboard data from the game server
 * - Handles connection validation and error scenarios
 * - Requires user authentication to access
 * 
 * Features:
 * - Real-time leaderboard data from server
 * - Comprehensive player statistics display
 * - Connection health monitoring
 * - Proper error handling and user feedback
 * - Session-based caching of leaderboard data
 * - Graceful degradation on connection issues
 * 
 * Security:
 * - Requires authenticated user (loggedInPlayer in session)
 * - Validates server connection before data requests
 * - Proper session management and cleanup
 * 
 * The servlet follows a simple but effective pattern:
 * 1. Authenticate user access
 * 2. Validate server connection
 * 3. Request fresh leaderboard data
 * 4. Wait for and process server response
 * 5. Display data or error messages appropriately
 */
@WebServlet("/leaderboard")
public class LeaderboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final String LEADERBOARD_VIEW = "leaderboard.jsp";
    private static final String LOGIN_REDIRECT_PATH = "login";
    
    private static final String ATTR_LOGGED_IN_PLAYER = "loggedInPlayer";
    private static final String ATTR_CURRENT_LEADERBOARD = "currentLeaderboard";
    private static final String ATTR_ALL_PLAYERS = "allPlayers";
    private static final String ATTR_ERROR = "error";
    
    private static final int SERVER_RESPONSE_WAIT_MS = 500;
    
    private static final String ERROR_CONNECTION_LOST = "Connection lost. Please log in again.";
    private static final String ERROR_LOADING_LEADERBOARD = "Unable to load leaderboard";
    private static final String ERROR_NO_DATA_RECEIVED = "No leaderboard data received from server";

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String sessionIdentifier = getSessionIdentifier(httpRequest);
        WebLogger.info("LeaderboardServlet", sessionIdentifier, "Leaderboard request received");

        if (!isUserAuthenticated(httpRequest)) {
            redirectToLogin(httpResponse, sessionIdentifier);
            return;
        }

        try {
            processLeaderboardRequest(httpRequest, sessionIdentifier);
        } catch (Exception exception) {
            handleLeaderboardException(sessionIdentifier, exception, httpRequest);
        }

        forwardToLeaderboardView(httpRequest, httpResponse);
    }

    /**
     * Processes the leaderboard request by fetching data from the server.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if leaderboard processing fails
     */
    private void processLeaderboardRequest(HttpServletRequest httpRequest, String sessionIdentifier) 
            throws Exception {
        
        WebClientConnection serverConnection = validateConnectionForLeaderboard(httpRequest, sessionIdentifier);
        
        if (serverConnection != null) {
            requestLeaderboardFromServer(serverConnection, httpRequest.getSession(), sessionIdentifier);
            processLeaderboardResponse(httpRequest, sessionIdentifier);
        } else {
            handleMissingConnection(httpRequest, sessionIdentifier);
        }
    }

    /**
     * Validates that a connection exists and is active for leaderboard requests.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     * @return the active WebClientConnection, or null if no valid connection exists
     */
    private WebClientConnection validateConnectionForLeaderboard(HttpServletRequest httpRequest, String sessionIdentifier) {
        WebClientConnection connection = WebConnectionManager.getExistingConnection(httpRequest.getSession());
        
        if (connection != null && connection.isConnectionActive()) {
            WebLogger.info("LeaderboardServlet", sessionIdentifier, "Valid connection found for leaderboard request");
            return connection;
        } else {
            WebLogger.warning("LeaderboardServlet", sessionIdentifier, "No valid connection available for leaderboard");
            return null;
        }
    }

    /**
     * Requests fresh leaderboard data from the game server.
     * 
     * @param serverConnection the connection to the game server
     * @param userSession the user's HTTP session
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if the request fails
     */
    private void requestLeaderboardFromServer(WebClientConnection serverConnection, HttpSession userSession, 
                                            String sessionIdentifier) throws Exception {
        
        // Clear any existing leaderboard data to ensure fresh results
        clearPreviousLeaderboardData(userSession);
        
        WebLogger.info("LeaderboardServlet", sessionIdentifier, "Requesting leaderboard data from server");
        serverConnection.getCommunicationProtocol().sendLeaderboardRequest();
        WebLogger.info("LeaderboardServlet", sessionIdentifier, "Leaderboard request sent to server");
        
        waitForServerResponse();
    }

    /**
     * Clears any previous leaderboard data from the session.
     * 
     * @param userSession the user's HTTP session
     */
    private void clearPreviousLeaderboardData(HttpSession userSession) {
        userSession.removeAttribute(ATTR_CURRENT_LEADERBOARD);
    }

    /**
     * Waits briefly for the server response to arrive.
     */
    private void waitForServerResponse() {
        try {
            Thread.sleep(SERVER_RESPONSE_WAIT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Processes the leaderboard response from the server.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     */
    @SuppressWarnings("unchecked")
    private void processLeaderboardResponse(HttpServletRequest httpRequest, String sessionIdentifier) {
        List<Player> leaderboardData = (List<Player>) httpRequest.getSession().getAttribute(ATTR_CURRENT_LEADERBOARD);
        
        if (leaderboardData != null) {
            setLeaderboardForDisplay(httpRequest, leaderboardData, sessionIdentifier);
        } else {
            handleNoLeaderboardData(httpRequest, sessionIdentifier);
        }
    }

    /**
     * Sets the leaderboard data for display in the view.
     * 
     * @param httpRequest the HTTP request object
     * @param leaderboardData the leaderboard data to display
     * @param sessionIdentifier the session ID for logging
     */
    private void setLeaderboardForDisplay(HttpServletRequest httpRequest, List<Player> leaderboardData, 
                                        String sessionIdentifier) {
        
        httpRequest.setAttribute(ATTR_ALL_PLAYERS, leaderboardData);
        WebLogger.info("LeaderboardServlet", sessionIdentifier, 
                      "Leaderboard loaded successfully with " + leaderboardData.size() + " players");
    }

    /**
     * Handles the case where no leaderboard data was received from the server.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     */
    private void handleNoLeaderboardData(HttpServletRequest httpRequest, String sessionIdentifier) {
        WebLogger.warning("LeaderboardServlet", sessionIdentifier, "No leaderboard data received from server");
        httpRequest.setAttribute(ATTR_ERROR, ERROR_NO_DATA_RECEIVED);
    }

    /**
     * Handles the case where no server connection is available.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     */
    private void handleMissingConnection(HttpServletRequest httpRequest, String sessionIdentifier) {
        WebLogger.warning("LeaderboardServlet", sessionIdentifier, "No connection available for leaderboard request");
        httpRequest.setAttribute(ATTR_ERROR, ERROR_CONNECTION_LOST);
    }

    /**
     * Checks if the current user is authenticated.
     * 
     * @param httpRequest the HTTP request to check
     * @return true if user is authenticated, false otherwise
     */
    private boolean isUserAuthenticated(HttpServletRequest httpRequest) {
        Player loggedInPlayer = (Player) httpRequest.getSession().getAttribute(ATTR_LOGGED_IN_PLAYER);
        return loggedInPlayer != null;
    }

    /**
     * Redirects unauthenticated users to the login page.
     * 
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if redirect fails
     */
    private void redirectToLogin(HttpServletResponse httpResponse, String sessionIdentifier) throws IOException {
        WebLogger.info("LeaderboardServlet", sessionIdentifier, "User not authenticated - redirecting to login");
        httpResponse.sendRedirect(LOGIN_REDIRECT_PATH);
    }

    /**
     * Forwards the request to the leaderboard view.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void forwardToLeaderboardView(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws ServletException, IOException {
        
        httpRequest.getRequestDispatcher(LEADERBOARD_VIEW).forward(httpRequest, httpResponse);
    }

    /**
     * Handles exceptions that occur during leaderboard processing.
     * 
     * @param sessionIdentifier the session ID for logging
     * @param exception the exception that occurred
     * @param httpRequest the HTTP request object
     */
    private void handleLeaderboardException(String sessionIdentifier, Exception exception, HttpServletRequest httpRequest) {
        WebLogger.error("LeaderboardServlet", sessionIdentifier, "Error processing leaderboard request", exception);
        httpRequest.setAttribute(ATTR_ERROR, ERROR_LOADING_LEADERBOARD + ": " + exception.getMessage());
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
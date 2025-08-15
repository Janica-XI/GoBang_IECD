package web.servlets;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import core.Player;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import web.utils.WebClientConnection;
import web.utils.WebConnectionManager;
import web.utils.WebLogger;

/**
 * Main lobby servlet for the GoBang web application.
 * 
 * This servlet handles the display and functionality of the game lobby where
 * authenticated users can view other players, send challenges, manage their
 * ready status, and interact with the game community. It serves as the central
 * hub for all pre-game activities.
 * 
 * GET requests:
 * - Displays the lobby interface (lobby.jsp)
 * - Requests fresh player list from the server
 * - Updates current user's profile with latest data
 * - Handles authentication checks and redirects
 * 
 * POST requests:
 * - Processes lobby actions (challenge, cancelChallenge, ready)
 * - Manages challenge state and ready status
 * - Provides AJAX responses for dynamic UI updates
 * 
 * Features:
 * - Real-time player list updates
 * - Challenge system for initiating games
 * - Ready status management
 * - Profile synchronization with server data
 * - Connection health monitoring
 * - Proper session state management
 * 
 * Security:
 * - Requires authenticated user (loggedInPlayer in session)
 * - Validates all user actions against server state
 * - Proper connection management and cleanup
 */
@WebServlet("/lobby")
public class LobbyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final String LOBBY_VIEW = "lobby.jsp";
    private static final String LOGIN_REDIRECT_PATH = "login";
    
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_OPPONENT = "opponent";
    private static final String PARAM_READY = "ready";
    
    private static final String ATTR_LOGGED_IN_PLAYER = "loggedInPlayer";
    private static final String ATTR_USERNAME = "username";
    private static final String ATTR_CURRENT_PLAYER_LIST = "currentPlayerList";
    private static final String ATTR_PLAYERS = "players";
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_SENT_CHALLENGE_TO = "sentChallengeTo";
    private static final String ATTR_IS_READY = "isReady";
    
    private static final String ACTION_CHALLENGE = "challenge";
    private static final String ACTION_CANCEL_CHALLENGE = "cancelChallenge";
    private static final String ACTION_READY = "ready";
    
    private static final String CHALLENGE_CANCELED_STATUS = "Canceled";
    private static final String READY_TRUE_VALUE = "true";
    
    private static final String HTTP_STATUS_OK = "OK";
    private static final String ERROR_NO_CONNECTION = "No server connection";
    private static final String ERROR_UNKNOWN_ACTION = "Unknown action";
    private static final String ERROR_CONNECTION_LOST = "Connection lost. Please log in again.";

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String sessionIdentifier = getSessionIdentifier(httpRequest);
        WebLogger.info("LobbyServlet", sessionIdentifier, "Lobby GET request received");

        if (!isUserAuthenticated(httpRequest)) {
            redirectToLogin(httpResponse, sessionIdentifier);
            return;
        }

        try {
            processLobbyDisplayRequest(httpRequest, sessionIdentifier);
        } catch (Exception exception) {
            handleLobbyException(sessionIdentifier, exception, httpRequest);
        }

        forwardToLobbyView(httpRequest, httpResponse);
    }

    @Override
    protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String requestedAction = httpRequest.getParameter(PARAM_ACTION);
        String sessionIdentifier = getSessionIdentifier(httpRequest);
        
        WebLogger.info("LobbyServlet", sessionIdentifier, "Lobby POST action: " + requestedAction);

        try {
            WebClientConnection serverConnection = validateConnectionForAction(httpRequest, sessionIdentifier);
            processLobbyAction(requestedAction, httpRequest, serverConnection, sessionIdentifier);
            sendSuccessResponse(httpResponse);
            
        } catch (Exception exception) {
            handleActionException(sessionIdentifier, exception, httpResponse);
        }
    }

    /**
     * Processes the lobby display request by fetching player data and updating profiles.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     */
    private void processLobbyDisplayRequest(HttpServletRequest httpRequest, String sessionIdentifier) {
        WebClientConnection serverConnection = WebConnectionManager.getExistingConnection(httpRequest.getSession());

        if (serverConnection != null && serverConnection.isConnectionActive()) {
            requestFreshPlayerList(serverConnection, sessionIdentifier);
            updatePlayerListDisplay(httpRequest, sessionIdentifier);
            updateCurrentUserProfile(httpRequest, sessionIdentifier);
        } else {
            handleMissingConnection(httpRequest, sessionIdentifier);
        }
    }

    /**
     * Requests a fresh player list from the server.
     * 
     * @param serverConnection the connection to the game server
     * @param sessionIdentifier the session ID for logging
     */
    private void requestFreshPlayerList(WebClientConnection serverConnection, String sessionIdentifier) {
        try {
            serverConnection.getCommunicationProtocol().sendListPlayersRequest();
            WebLogger.info("LobbyServlet", sessionIdentifier, "Requested fresh player list from server");
        } catch (Exception exception) {
            WebLogger.error("LobbyServlet", sessionIdentifier, "Failed to request player list", exception);
        }
    }

    /**
     * Updates the display with the current player list from session.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     */
    @SuppressWarnings("unchecked")
    private void updatePlayerListDisplay(HttpServletRequest httpRequest, String sessionIdentifier) {
        List<Player> currentPlayerList = (List<Player>) httpRequest.getSession().getAttribute(ATTR_CURRENT_PLAYER_LIST);
        String currentUsername = (String) httpRequest.getSession().getAttribute(ATTR_USERNAME);
        

        if (currentPlayerList != null) {
        	List<Player> filteredPlayerList = currentPlayerList.stream()
                    .filter(player -> !player.getUsername().equals(currentUsername))
                    .collect(Collectors.toList());
            httpRequest.setAttribute(ATTR_PLAYERS, filteredPlayerList);
            WebLogger.info("LobbyServlet", sessionIdentifier, 
                          "Displaying " + filteredPlayerList.size() + " players in lobby");
        } else {
            WebLogger.info("LobbyServlet", sessionIdentifier, "No player list available in session");
        }
    }

    /**
     * Updates the current user's profile with the latest data from the player list.
     * This ensures the user sees their most recent statistics and information.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     */
    @SuppressWarnings("unchecked")
    private void updateCurrentUserProfile(HttpServletRequest httpRequest, String sessionIdentifier) {
        String currentUsername = (String) httpRequest.getSession().getAttribute(ATTR_USERNAME);
        List<Player> currentPlayerList = (List<Player>) httpRequest.getSession().getAttribute(ATTR_CURRENT_PLAYER_LIST);

        if (currentUsername == null || currentPlayerList == null) {
            return;
        }

        Player updatedProfile = findPlayerInList(currentPlayerList, currentUsername);
        if (updatedProfile != null) {
            httpRequest.getSession().setAttribute(ATTR_LOGGED_IN_PLAYER, updatedProfile);
            WebLogger.info("LobbyServlet", sessionIdentifier, 
                          String.format("Profile updated for: %s (V:%d, D:%d)", 
                                       updatedProfile.getUsername(), 
                                       updatedProfile.getVictories(), 
                                       updatedProfile.getDefeats()));
        }
    }

    /**
     * Finds a player by username in the given player list.
     * 
     * @param playerList the list to search
     * @param targetUsername the username to find
     * @return the Player object if found, null otherwise
     */
    private Player findPlayerInList(List<Player> playerList, String targetUsername) {
        for (Player player : playerList) {
            if (targetUsername.equals(player.getUsername())) {
                return player;
            }
        }
        return null;
    }

    /**
     * Handles the case where no server connection is available.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     */
    private void handleMissingConnection(HttpServletRequest httpRequest, String sessionIdentifier) {
        WebLogger.warning("LobbyServlet", sessionIdentifier, "No connection found for lobby access");
        httpRequest.setAttribute(ATTR_ERROR, ERROR_CONNECTION_LOST);
    }

    /**
     * Validates that a connection exists and is active for processing actions.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     * @return the active WebClientConnection
     * @throws Exception if no valid connection exists
     */
    private WebClientConnection validateConnectionForAction(HttpServletRequest httpRequest, String sessionIdentifier) 
            throws Exception {
        
        WebClientConnection connection = WebConnectionManager.getExistingConnection(httpRequest.getSession());
        
        if (connection == null || !connection.isConnectionActive()) {
            WebLogger.error("LobbyServlet", sessionIdentifier, "No active connection for action processing");
            throw new Exception(ERROR_NO_CONNECTION);
        }
        
        return connection;
    }

    /**
     * Processes the requested lobby action by dispatching to appropriate handlers.
     * 
     * @param requestedAction the action to process
     * @param httpRequest the HTTP request object
     * @param serverConnection the server connection
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if action processing fails
     */
    private void processLobbyAction(String requestedAction, HttpServletRequest httpRequest, 
                                   WebClientConnection serverConnection, String sessionIdentifier) 
                                   throws Exception {
        
        if (requestedAction == null) {
            throw new Exception(ERROR_UNKNOWN_ACTION);
        }

        switch (requestedAction) {
            case ACTION_CHALLENGE:
                handleChallengeAction(httpRequest, serverConnection, sessionIdentifier);
                break;
            case ACTION_CANCEL_CHALLENGE:
                handleCancelChallengeAction(httpRequest, serverConnection, sessionIdentifier);
                break;
            case ACTION_READY:
                handleReadyAction(httpRequest, serverConnection, sessionIdentifier);
                break;
            default:
                WebLogger.warning("LobbyServlet", sessionIdentifier, "Unknown action: " + requestedAction);
                throw new Exception(ERROR_UNKNOWN_ACTION);
        }
    }

    /**
     * Handles challenge action by sending a challenge request to another player.
     * 
     * @param httpRequest the HTTP request object
     * @param serverConnection the server connection
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if challenge processing fails
     */
    private void handleChallengeAction(HttpServletRequest httpRequest, WebClientConnection serverConnection, 
                                     String sessionIdentifier) throws Exception {
        
        String opponentUsername = httpRequest.getParameter(PARAM_OPPONENT);
        
        if (opponentUsername == null || opponentUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("Opponent not specified");
        }

        WebLogger.info("LobbyServlet", sessionIdentifier, "Sending challenge to: " + opponentUsername);
        serverConnection.getCommunicationProtocol().sendChallengeRequest(opponentUsername);

        // Track who we challenged for UI state management
        httpRequest.getSession().setAttribute(ATTR_SENT_CHALLENGE_TO, opponentUsername);
        WebLogger.info("LobbyServlet", sessionIdentifier, "Challenge sent and tracked for: " + opponentUsername);
    }

    /**
     * Handles cancel challenge action by sending a cancellation reply.
     * 
     * @param httpRequest the HTTP request object
     * @param serverConnection the server connection
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if cancellation processing fails
     */
    private void handleCancelChallengeAction(HttpServletRequest httpRequest, WebClientConnection serverConnection, 
                                           String sessionIdentifier) throws Exception {
        
        WebLogger.info("LobbyServlet", sessionIdentifier, "Canceling pending challenge");
        
        serverConnection.getCommunicationProtocol().sendChallengeReply(CHALLENGE_CANCELED_STATUS);
        
        // Clear challenge state from session
        httpRequest.getSession().removeAttribute(ATTR_SENT_CHALLENGE_TO);
        WebLogger.info("LobbyServlet", sessionIdentifier, "Challenge canceled and state cleared");
    }

    /**
     * Handles ready status action by updating the user's ready state.
     * 
     * @param httpRequest the HTTP request object
     * @param serverConnection the server connection
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if ready status processing fails
     */
    private void handleReadyAction(HttpServletRequest httpRequest, WebClientConnection serverConnection, 
                                 String sessionIdentifier) throws Exception {
        
        String readyStatusParam = httpRequest.getParameter(PARAM_READY);
        boolean isUserReady = READY_TRUE_VALUE.equals(readyStatusParam);

        WebLogger.info("LobbyServlet", sessionIdentifier, "Changing ready status to: " + isUserReady);
        serverConnection.getCommunicationProtocol().sendReadyRequest(isUserReady);

        // Update session immediately for UI consistency
        httpRequest.getSession().setAttribute(ATTR_IS_READY, isUserReady);
        WebLogger.info("LobbyServlet", sessionIdentifier, "Ready status updated in session: " + isUserReady);
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
        WebLogger.info("LobbyServlet", sessionIdentifier, "User not authenticated - redirecting to login");
        httpResponse.sendRedirect(LOGIN_REDIRECT_PATH);
    }

    /**
     * Forwards the request to the lobby view.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void forwardToLobbyView(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws ServletException, IOException {
        
        httpRequest.getRequestDispatcher(LOBBY_VIEW).forward(httpRequest, httpResponse);
    }

    /**
     * Sends a successful response for AJAX actions.
     * 
     * @param httpResponse the HTTP response object
     * @throws IOException if writing fails
     */
    private void sendSuccessResponse(HttpServletResponse httpResponse) throws IOException {
        httpResponse.setStatus(200);
        httpResponse.getWriter().write(HTTP_STATUS_OK);
    }

    /**
     * Handles exceptions that occur during lobby display processing.
     * 
     * @param sessionIdentifier the session ID for logging
     * @param exception the exception that occurred
     * @param httpRequest the HTTP request object
     */
    private void handleLobbyException(String sessionIdentifier, Exception exception, HttpServletRequest httpRequest) {
        WebLogger.error("LobbyServlet", sessionIdentifier, "Error processing lobby display", exception);
        httpRequest.setAttribute(ATTR_ERROR, "Error loading lobby: " + exception.getMessage());
    }

    /**
     * Handles exceptions that occur during action processing.
     * 
     * @param sessionIdentifier the session ID for logging
     * @param exception the exception that occurred
     * @param httpResponse the HTTP response object
     * @throws IOException if error response cannot be sent
     */
    private void handleActionException(String sessionIdentifier, Exception exception, HttpServletResponse httpResponse) 
            throws IOException {
        
        WebLogger.error("LobbyServlet", sessionIdentifier, "Error processing lobby action", exception);
        httpResponse.setStatus(500);
        httpResponse.getWriter().write("Error: " + exception.getMessage());
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
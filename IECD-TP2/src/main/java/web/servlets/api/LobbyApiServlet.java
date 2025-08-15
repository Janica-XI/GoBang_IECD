package web.servlets.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

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
 * Comprehensive API servlet handling all lobby-related operations.
 * 
 * This servlet consolidates multiple lobby functionalities including notifications,
 * player lists, profile management, challenge responses, and logout operations.
 * It provides RESTful endpoints for the web client to interact with the game server
 * through polling and direct API calls.
 * 
 * Supported operations:
 * GET requests:
 * - notifications: Polls for pending notifications (challenges, game starts, errors)
 * - playerlist: Retrieves current list of online players
 * - profile-refresh: Updates user profile from latest player list
 * 
 * POST requests:
 * - challenge-response: Responds to incoming challenge invitations
 * - logout: Handles user logout with proper cleanup
 * 
 * All operations require user authentication (valid session with username).
 * Responses are provided in JSON format for easy client-side processing.
 */
@WebServlet("/api/lobby")
public class LobbyApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CHARSET_UTF8 = "UTF-8";
    private static final int LOGOUT_RESPONSE_WAIT_MS = 500;

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        configureJsonResponse(httpResponse);
        
        String requestedAction = httpRequest.getParameter("action");
        if (requestedAction == null) {
            sendErrorResponse(httpResponse, "No action provided");
            return;
        }

        if (!isUserAuthenticated(httpRequest)) {
            sendErrorResponse(httpResponse, "Not logged in");
            return;
        }

        String sessionIdentifier = httpRequest.getSession().getId();
        WebLogger.info("LobbyApiServlet", sessionIdentifier, "GET request for action: " + requestedAction);

        try {
            processGetRequest(requestedAction, httpRequest, httpResponse);
        } catch (Exception exception) {
            handleServletException("GET", sessionIdentifier, exception, httpResponse);
        }
    }

    @Override
    protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        configureJsonResponse(httpResponse);
        
        String requestedAction = httpRequest.getParameter("action");
        if (requestedAction == null) {
            sendErrorResponse(httpResponse, "No action provided");
            return;
        }

        String sessionIdentifier = httpRequest.getSession().getId();
        WebLogger.info("LobbyApiServlet", sessionIdentifier, "POST request for action: " + requestedAction);

        try {
            processPostRequest(requestedAction, httpRequest, httpResponse);
        } catch (Exception exception) {
            handleServletException("POST", sessionIdentifier, exception, httpResponse);
        }
    }

    /**
     * Processes GET requests by dispatching to appropriate handlers.
     * 
     * @param requestedAction the action parameter from the request
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws IOException if response writing fails
     */
    private void processGetRequest(String requestedAction, HttpServletRequest httpRequest, 
                                  HttpServletResponse httpResponse) throws IOException {
        switch (requestedAction) {
            case "notifications":
                handleNotificationPolling(httpRequest, httpResponse);
                break;
            case "playerlist":
                handlePlayerListRetrieval(httpRequest, httpResponse);
                break;
            case "profile-refresh":
                handleProfileRefresh(httpRequest, httpResponse);
                break;
            default:
                sendErrorResponse(httpResponse, "Unknown action: " + requestedAction);
                break;
        }
    }

    /**
     * Processes POST requests by dispatching to appropriate handlers.
     * 
     * @param requestedAction the action parameter from the request
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws IOException if response writing fails
     */
    private void processPostRequest(String requestedAction, HttpServletRequest httpRequest, 
                                   HttpServletResponse httpResponse) throws IOException {
        switch (requestedAction) {
            case "challenge-response":
                handleChallengeResponse(httpRequest, httpResponse);
                break;
            case "logout":
                handleLogoutRequest(httpRequest, httpResponse);
                break;
            default:
                sendErrorResponse(httpResponse, "Unknown POST action: " + requestedAction);
                break;
        }
    }

    /**
     * Handles notification polling requests.
     * Checks for various types of pending notifications in priority order:
     * 1. Challenge invitations
     * 2. Challenge replies
     * 3. Game start notifications
     * 4. Generic lobby errors
     * 5. Profile update notifications
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws IOException if response writing fails
     */
    private void handleNotificationPolling(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws IOException {
        
        HttpSession userSession = httpRequest.getSession();
        String sessionIdentifier = userSession.getId();
        
        // Priority 1: Challenge invitations
        if (processPendingChallengeInvitation(userSession, httpResponse)) {
            WebLogger.info("LobbyApiServlet", sessionIdentifier, "Sent challenge invitation notification");
            return;
        }
        
        // Priority 2: Challenge replies
        if (processChallengeReplyNotification(userSession, httpResponse)) {
            WebLogger.info("LobbyApiServlet", sessionIdentifier, "Sent challenge reply notification");
            return;
        }
        
        // Priority 3: Game start notifications
        if (processGameStartNotification(userSession, httpResponse)) {
            WebLogger.info("LobbyApiServlet", sessionIdentifier, "Sent game start notification");
            return;
        }
        
        // Priority 4: Generic lobby errors
        if (processLobbyErrorNotifications(userSession, httpResponse)) {
            WebLogger.info("LobbyApiServlet", sessionIdentifier, "Sent lobby error notification");
            return;
        }
        
        // Priority 5: Profile updates
        if (processProfileUpdateNotification(userSession, httpResponse)) {
            WebLogger.info("LobbyApiServlet", sessionIdentifier, "Sent profile update notification");
            return;
        }
        
        // No notifications available
        httpResponse.getWriter().write("{\"type\":\"none\"}");
    }

    /**
     * Processes pending challenge invitation notifications.
     * 
     * @param userSession the user's HTTP session
     * @param httpResponse the HTTP response object
     * @return true if a notification was sent, false otherwise
     * @throws IOException if response writing fails
     */
    private boolean processPendingChallengeInvitation(HttpSession userSession, HttpServletResponse httpResponse) 
            throws IOException {
        
        Player challengerProfile = (Player) userSession.getAttribute("pendingChallengeInvitation");
        if (challengerProfile == null) {
            return false;
        }
        
        userSession.removeAttribute("pendingChallengeInvitation");
        
        String challengerPhoto = challengerProfile.getPhotoBase64() != null ? 
                                challengerProfile.getPhotoBase64() : "";
        
        String jsonResponse = String.format(
            "{\"type\":\"challenge\",\"username\":\"%s\",\"photoBase64\":\"%s\"}",
            escapeJsonString(challengerProfile.getUsername()),
            escapeJsonString(challengerPhoto)
        );
        
        httpResponse.getWriter().write(jsonResponse);
        return true;
    }

    /**
     * Processes challenge reply notifications.
     * 
     * @param userSession the user's HTTP session
     * @param httpResponse the HTTP response object
     * @return true if a notification was sent, false otherwise
     * @throws IOException if response writing fails
     */
    private boolean processChallengeReplyNotification(HttpSession userSession, HttpServletResponse httpResponse) 
            throws IOException {
        
        String challengeReplyStatus = (String) userSession.getAttribute("lastChallengeReply");
        if (challengeReplyStatus == null) {
            return false;
        }
        
        userSession.removeAttribute("lastChallengeReply");
        
        String jsonResponse = String.format(
            "{\"type\":\"challengeReply\",\"status\":\"%s\"}",
            escapeJsonString(challengeReplyStatus)
        );
        
        httpResponse.getWriter().write(jsonResponse);
        return true;
    }

    /**
     * Processes game start notifications.
     * 
     * @param userSession the user's HTTP session
     * @param httpResponse the HTTP response object
     * @return true if a notification was sent, false otherwise
     * @throws IOException if response writing fails
     */
    private boolean processGameStartNotification(HttpSession userSession, HttpServletResponse httpResponse) 
            throws IOException {
        
        String pendingGameId = (String) userSession.getAttribute("pendingGameStart");
        if (pendingGameId == null) {
            return false;
        }
        
        String blackPlayerName = (String) userSession.getAttribute("game_" + pendingGameId + "_blackPlayer");
        String whitePlayerName = (String) userSession.getAttribute("game_" + pendingGameId + "_whitePlayer");
        
        userSession.removeAttribute("pendingGameStart");
        
        String jsonResponse = String.format(
            "{\"type\":\"gameStart\",\"gameId\":\"%s\",\"blackPlayer\":\"%s\",\"whitePlayer\":\"%s\"}",
            escapeJsonString(pendingGameId),
            escapeJsonString(blackPlayerName),
            escapeJsonString(whitePlayerName)
        );
        
        httpResponse.getWriter().write(jsonResponse);
        return true;
    }

    /**
     * Processes generic lobby error notifications.
     * 
     * @param userSession the user's HTTP session
     * @param httpResponse the HTTP response object
     * @return true if a notification was sent, false otherwise
     * @throws IOException if response writing fails
     */
    @SuppressWarnings("unchecked")
    private boolean processLobbyErrorNotifications(HttpSession userSession, HttpServletResponse httpResponse) 
            throws IOException {
        
        Queue<String> notificationQueue = (Queue<String>) userSession.getAttribute("lobbyNotifications");
        if (notificationQueue == null || notificationQueue.isEmpty()) {
            return false;
        }
        
        String errorNotification = notificationQueue.poll();
        
        String jsonResponse = String.format(
            "{\"type\":\"error\",\"message\":\"%s\"}",
            escapeJsonString(errorNotification)
        );
        
        httpResponse.getWriter().write(jsonResponse);
        return true;
    }

    /**
     * Processes profile update notifications.
     * 
     * @param userSession the user's HTTP session
     * @param httpResponse the HTTP response object
     * @return true if a notification was sent, false otherwise
     * @throws IOException if response writing fails
     */
    private boolean processProfileUpdateNotification(HttpSession userSession, HttpServletResponse httpResponse) 
            throws IOException {
        
        Player updatedPlayerProfile = (Player) userSession.getAttribute("updatedProfile");
        if (updatedPlayerProfile == null) {
            return false;
        }
        
        userSession.removeAttribute("updatedProfile");
        
        String formattedPlayTime = formatPlayerTotalTime(updatedPlayerProfile);
        
        String jsonResponse = String.format(
            "{\"type\":\"profileUpdate\",\"victories\":%d,\"defeats\":%d,\"totalTime\":\"%s\"}",
            updatedPlayerProfile.getVictories(),
            updatedPlayerProfile.getDefeats(),
            formattedPlayTime
        );
        
        httpResponse.getWriter().write(jsonResponse);
        return true;
    }

    /**
     * Handles player list retrieval requests.
     * Returns cached player list from session or empty list if none available.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws IOException if response writing fails
     */
    @SuppressWarnings("unchecked")
    private void handlePlayerListRetrieval(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws IOException {
        
        String sessionIdentifier = httpRequest.getSession().getId();
        String currentUsername = (String) httpRequest.getSession().getAttribute("username");
        List<Player> cachedPlayerList = (List<Player>) httpRequest.getSession().getAttribute("currentPlayerList");

        if (cachedPlayerList != null) {
            // Filter out the current user from the API response
            List<Player> filteredPlayerList = cachedPlayerList.stream()
                .filter(player -> !player.getUsername().equals(currentUsername))
                .collect(Collectors.toList());
                
            String playersJsonResponse = buildPlayerListJsonResponse(filteredPlayerList);
            WebLogger.info("LobbyApiServlet", sessionIdentifier, 
                          "Returning player list with " + filteredPlayerList.size() + " players");
            httpResponse.getWriter().write(playersJsonResponse);
        } else {
            WebLogger.info("LobbyApiServlet", sessionIdentifier, "No player list available in session");
            httpResponse.getWriter().write("{\"players\":[]}");
        }
    }

    /**
     * Handles profile refresh requests.
     * Updates the logged-in player profile from the current player list.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws IOException if response writing fails
     */
    @SuppressWarnings("unchecked")
    private void handleProfileRefresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws IOException {
        
        String currentUsername = (String) httpRequest.getSession().getAttribute("username");
        List<Player> currentPlayerList = (List<Player>) httpRequest.getSession().getAttribute("currentPlayerList");

        if (currentPlayerList == null) {
            sendErrorResponse(httpResponse, "No player list available");
            return;
        }

        Player updatedUserProfile = findPlayerByUsername(currentPlayerList, currentUsername);
        if (updatedUserProfile == null) {
            sendErrorResponse(httpResponse, "Profile not found");
            return;
        }

        httpRequest.getSession().setAttribute("loggedInPlayer", updatedUserProfile);
        String formattedPlayTime = formatPlayerTotalTime(updatedUserProfile);

        String profileJsonResponse = String.format(
            "{\"victories\":%d,\"defeats\":%d,\"totalTime\":\"%s\"}",
            updatedUserProfile.getVictories(),
            updatedUserProfile.getDefeats(),
            formattedPlayTime
        );

        httpResponse.getWriter().write(profileJsonResponse);
    }

    /**
     * Handles challenge response requests.
     * Processes user responses to incoming challenge invitations.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws IOException if response writing fails
     */
    private void handleChallengeResponse(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws IOException {

        String challengeResponse = httpRequest.getParameter("response");
        String sessionIdentifier = httpRequest.getSession().getId();

        if (challengeResponse == null) {
            httpResponse.setStatus(400);
            sendErrorResponse(httpResponse, "No response provided");
            return;
        }

        WebLogger.info("LobbyApiServlet", sessionIdentifier, "Challenge response: " + challengeResponse);

        try {
            WebClientConnection userConnection = WebConnectionManager.getExistingConnection(httpRequest.getSession());
            if (userConnection == null || !userConnection.isConnectionActive()) {
                httpResponse.setStatus(400);
                sendErrorResponse(httpResponse, "No connection");
                return;
            }

            String protocolResponse = "accept".equals(challengeResponse) ? "Accepted" : "Rejected";
            userConnection.getCommunicationProtocol().sendChallengeReply(protocolResponse);
            
            WebLogger.info("LobbyApiServlet", sessionIdentifier, "Sent challenge reply: " + protocolResponse);

            if ("Rejected".equals(protocolResponse)) {
                httpRequest.getSession().removeAttribute("pendingChallengeInvitation");
                WebLogger.info("LobbyApiServlet", sessionIdentifier, "Cleared challenge data after rejection");
            }

            httpResponse.getWriter().write("{\"status\":\"ok\"}");

        } catch (Exception exception) {
            WebLogger.error("LobbyApiServlet", sessionIdentifier, "Error sending challenge response", exception);
            httpResponse.setStatus(500);
            sendErrorResponse(httpResponse, escapeJsonString(exception.getMessage()));
        }
    }

    /**
     * Handles logout requests with proper server communication and cleanup.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws IOException if response writing fails
     */
    private void handleLogoutRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws IOException {

        String sessionIdentifier = httpRequest.getSession().getId();
        String currentUsername = (String) httpRequest.getSession().getAttribute("username");
        
        WebLogger.info("LobbyApiServlet", sessionIdentifier, "Logout request for user: " + currentUsername);

        try {
            WebClientConnection userConnection = WebConnectionManager.getExistingConnection(httpRequest.getSession());
            if (userConnection == null || !userConnection.isConnectionActive()) {
                sendErrorResponse(httpResponse, "No connection");
                return;
            }

            httpRequest.getSession().removeAttribute("lastLogoutStatus");
            userConnection.getCommunicationProtocol().sendLogoutRequest();
            
            WebLogger.info("LobbyApiServlet", sessionIdentifier, "Sent logout request to server");

            waitForServerResponse();

            String logoutStatus = (String) httpRequest.getSession().getAttribute("lastLogoutStatus");
            if ("Accepted".equals(logoutStatus)) {
                performCompleteSessionCleanup(httpRequest.getSession());
                WebLogger.info("LobbyApiServlet", sessionIdentifier, "Logout successful and session cleaned for: " + currentUsername);
                httpResponse.getWriter().write("{\"status\":\"success\",\"message\":\"Logout successful\"}");
            } else {
                WebLogger.warning("LobbyApiServlet", sessionIdentifier, "Logout rejected: " + logoutStatus);
                httpResponse.getWriter().write("{\"status\":\"error\",\"message\":\"Logout rejected\"}");
            }

        } catch (Exception exception) {
            WebLogger.error("LobbyApiServlet", sessionIdentifier, "Error during logout", exception);
            sendErrorResponse(httpResponse, "Error during logout: " + escapeJsonString(exception.getMessage()));
        }
    }

    /**
     * Performs complete cleanup of user data from the session.
     * Removes all user-related attributes while maintaining connection infrastructure.
     * Called after successful logout to prepare for potential new login.
     * 
     * @param userSession the session to clean up
     */
    private void performCompleteSessionCleanup(HttpSession userSession) {
        String sessionIdentifier = userSession.getId();
        WebLogger.info("LobbyApiServlet", sessionIdentifier, "Starting complete session cleanup");

        clearBasicUserData(userSession);
        clearLobbyStateData(userSession);
        clearAllGameSpecificData(userSession);
        clearProtocolResponseData(userSession);
        resetNotificationQueue(userSession);

        WebLogger.info("LobbyApiServlet", sessionIdentifier, "Session cleanup completed");
    }

    /**
     * Clears basic user authentication and profile data.
     * 
     * @param userSession the session to clean up
     */
    private void clearBasicUserData(HttpSession userSession) {
        userSession.removeAttribute("loggedInPlayer");
        userSession.removeAttribute("username");
        userSession.removeAttribute("isReady");
    }

    /**
     * Clears lobby-related state data.
     * 
     * @param userSession the session to clean up
     */
    private void clearLobbyStateData(HttpSession userSession) {
        userSession.removeAttribute("currentPlayerList");
        userSession.removeAttribute("pendingChallengeInvitation");
        userSession.removeAttribute("sentChallengeTo");
        userSession.removeAttribute("lastChallengeReply");
        userSession.removeAttribute("pendingGameStart");
        userSession.removeAttribute("updatedProfile");
    }

    /**
     * Clears all game-specific data from the session.
     * Searches for and removes attributes with game-related prefixes.
     * 
     * @param userSession the session to clean up
     */
    private void clearAllGameSpecificData(HttpSession userSession) {
        Enumeration<String> attributeNames = userSession.getAttributeNames();
        List<String> gameAttributesToRemove = new ArrayList<>();

        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            if (isGameRelatedAttribute(attributeName)) {
                gameAttributesToRemove.add(attributeName);
            }
        }

        for (String attributeName : gameAttributesToRemove) {
            userSession.removeAttribute(attributeName);
            WebLogger.info("LobbyApiServlet", userSession.getId(), "Removed session attribute: " + attributeName);
        }
    }

    /**
     * Clears protocol response data from the session.
     * 
     * @param userSession the session to clean up
     */
    private void clearProtocolResponseData(HttpSession userSession) {
        userSession.removeAttribute("lastLoginStatus");
        userSession.removeAttribute("lastLoginProfile");
        userSession.removeAttribute("lastRegisterStatus");
        userSession.removeAttribute("lastProfileUpdateStatus");
        userSession.removeAttribute("lastPhotoUpdateStatus");
        userSession.removeAttribute("lastReadyStatus");
    }

    /**
     * Resets the lobby notification queue to empty state.
     * 
     * @param userSession the session to reset
     */
    private void resetNotificationQueue(HttpSession userSession) {
        userSession.setAttribute("lobbyNotifications", new java.util.concurrent.ConcurrentLinkedQueue<String>());
    }

    /**
     * Checks if an attribute name is related to game data.
     * 
     * @param attributeName the attribute name to check
     * @return true if the attribute is game-related
     */
    private boolean isGameRelatedAttribute(String attributeName) {
        return attributeName.startsWith("game_") ||
               attributeName.startsWith("gameState_") ||
               attributeName.startsWith("moveReply_") ||
               attributeName.startsWith("gameEnd_") ||
               attributeName.startsWith("disconnect_");
    }

    /**
     * Builds a JSON response containing the list of players.
     * 
     * @param playerList the list of players to serialize
     * @return JSON string representation of the player list
     */
    private String buildPlayerListJsonResponse(List<Player> playerList) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"players\":[");

        for (int i = 0; i < playerList.size(); i++) {
            Player player = playerList.get(i);
            if (i > 0) {
                jsonBuilder.append(",");
            }
            
            String playerPhoto = player.getPhotoBase64() != null ? player.getPhotoBase64() : "";
            
            jsonBuilder.append("{")
                      .append("\"username\":\"").append(escapeJsonString(player.getUsername())).append("\",")
                      .append("\"victories\":").append(player.getVictories()).append(",")
                      .append("\"defeats\":").append(player.getDefeats()).append(",")
                      .append("\"photoBase64\":\"").append(escapeJsonString(playerPhoto)).append("\"")
                      .append("}");
        }

        jsonBuilder.append("]}");
        return jsonBuilder.toString();
    }

    /**
     * Finds a player by username in the given player list.
     * 
     * @param playerList the list to search
     * @param targetUsername the username to find
     * @return the Player object if found, null otherwise
     */
    private Player findPlayerByUsername(List<Player> playerList, String targetUsername) {
        for (Player player : playerList) {
            if (targetUsername.equals(player.getUsername())) {
                return player;
            }
        }
        return null;
    }

    /**
     * Formats a player's total time as HH:MM:SS string.
     * 
     * @param player the player whose time to format
     * @return formatted time string
     */
    private String formatPlayerTotalTime(Player player) {
        long hours = player.getTotalTime().toHours();
        long minutes = player.getTotalTime().toMinutesPart();
        long seconds = player.getTotalTime().toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Waits briefly for server response to arrive.
     * Used after sending requests that expect immediate replies.
     */
    private void waitForServerResponse() {
        try {
            Thread.sleep(LOGOUT_RESPONSE_WAIT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Configures the HTTP response for JSON content.
     * 
     * @param httpResponse the response to configure
     */
    private void configureJsonResponse(HttpServletResponse httpResponse) {
        httpResponse.setContentType(CONTENT_TYPE_JSON);
        httpResponse.setCharacterEncoding(CHARSET_UTF8);
    }

    /**
     * Checks if the current user is authenticated.
     * 
     * @param httpRequest the request to check
     * @return true if user has valid session with username
     */
    private boolean isUserAuthenticated(HttpServletRequest httpRequest) {
        String currentUsername = (String) httpRequest.getSession().getAttribute("username");
        return currentUsername != null;
    }

    /**
     * Sends a standardized error response in JSON format.
     * 
     * @param httpResponse the response object
     * @param errorMessage the error message to send
     * @throws IOException if writing fails
     */
    private void sendErrorResponse(HttpServletResponse httpResponse, String errorMessage) throws IOException {
        String jsonError = String.format("{\"error\":\"%s\"}", escapeJsonString(errorMessage));
        httpResponse.getWriter().write(jsonError);
    }

    /**
     * Handles servlet exceptions with proper logging and error responses.
     * 
     * @param requestType the type of request (GET or POST)
     * @param sessionIdentifier the session ID for logging
     * @param exception the exception that occurred
     * @param httpResponse the response object
     * @throws IOException if writing the error response fails
     */
    private void handleServletException(String requestType, String sessionIdentifier, Exception exception, 
                                       HttpServletResponse httpResponse) throws IOException {
        WebLogger.error("LobbyApiServlet", sessionIdentifier, "Error in " + requestType + " request", exception);
        sendErrorResponse(httpResponse, "Internal server error");
    }

    /**
     * Escapes special characters for valid JSON strings.
     * 
     * @param input the string to escape
     * @return escaped string safe for JSON
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
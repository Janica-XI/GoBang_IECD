package web.servlets.api;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import web.utils.WebLogger;

/**
 * API servlet for polling game-specific notifications and events.
 * 
 * This servlet provides real-time notifications for individual games through
 * a polling mechanism. Unlike lobby notifications which are global, this servlet
 * handles events that are specific to a particular game instance.
 * 
 * The servlet checks for notifications in priority order and returns immediately
 * when a notification is found. This follows the KISS principle by using simple
 * polling with immediate consumption of notification data.
 * 
 * Supported notification types (in priority order):
 * 1. Game end notifications - when a game concludes with a winner
 * 2. Opponent disconnection - when the opponent leaves the game
 * 3. Move error notifications - when a move is rejected by the server
 * 
 * Required parameters:
 * - gameId: The unique identifier of the game to check for notifications
 * 
 * Authentication:
 * - Requires valid session with username attribute
 * 
 * Response format:
 * - Notification found: JSON object with notification type and relevant data
 * - No notifications: JSON object with type "none"
 * - Error: JSON object with error message
 */
@WebServlet("/api/game-notifications")
public class GameNotificationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CHARSET_UTF8 = "UTF-8";
    
    private static final String GAME_END_KEY_PREFIX = "gameEnd_";
    private static final String DISCONNECT_KEY_PREFIX = "disconnect_";
    private static final String MOVE_REPLY_KEY_PREFIX = "moveReply_";

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        configureJsonResponse(httpResponse);
        
        String requestedGameId = httpRequest.getParameter("gameId");
        if (!isValidGameId(requestedGameId)) {
            sendErrorResponse(httpResponse, "GameId required");
            return;
        }

        if (!isUserAuthenticated(httpRequest)) {
            sendErrorResponse(httpResponse, "Not logged in");
            return;
        }

        String sessionIdentifier = httpRequest.getSession().getId();
        WebLogger.info("GameNotificationServlet", sessionIdentifier, 
                      "Notification polling for gameId: " + requestedGameId);

        try {
            processGameNotificationRequest(requestedGameId, httpRequest.getSession(), 
                                         httpResponse, sessionIdentifier);
        } catch (Exception exception) {
            handleServletException(requestedGameId, sessionIdentifier, exception, httpResponse);
        }
    }

    /**
     * Processes the game notification request by checking for notifications in priority order.
     * 
     * @param gameId the ID of the game to check notifications for
     * @param userSession the user's HTTP session
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if response writing fails
     */
    private void processGameNotificationRequest(String gameId, HttpSession userSession, 
                                              HttpServletResponse httpResponse, String sessionIdentifier) 
                                              throws IOException {
        
        // Priority 1: Check for game end notifications
        if (processGameEndNotification(gameId, userSession, httpResponse, sessionIdentifier)) {
            return;
        }
        
        // Priority 2: Check for opponent disconnection
        if (processOpponentDisconnectNotification(gameId, userSession, httpResponse, sessionIdentifier)) {
            return;
        }
        
        // Priority 3: Check for move error notifications
        if (processMoveErrorNotification(gameId, userSession, httpResponse, sessionIdentifier)) {
            return;
        }
        
        // No notifications found for this game
        sendNoNotificationResponse(httpResponse);
    }

    /**
     * Processes game end notifications for the specified game.
     * 
     * @param gameId the game ID to check
     * @param userSession the user's HTTP session
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @return true if a notification was sent, false otherwise
     * @throws IOException if response writing fails
     */
    private boolean processGameEndNotification(String gameId, HttpSession userSession, 
                                             HttpServletResponse httpResponse, String sessionIdentifier) 
                                             throws IOException {
        
        String gameEndSessionKey = GAME_END_KEY_PREFIX + gameId;
        String gameEndMessage = (String) userSession.getAttribute(gameEndSessionKey);
        
        if (gameEndMessage == null) {
            return false;
        }
        
        // Consume immediately - this only happens once per game
        userSession.removeAttribute(gameEndSessionKey);
        
        WebLogger.info("GameNotificationServlet", sessionIdentifier, "Game end notification for: " + gameId);
        
        String jsonResponse = String.format(
            "{\"type\":\"gameEnd\",\"message\":\"%s\"}",
            escapeJsonString(gameEndMessage)
        );
        
        httpResponse.getWriter().write(jsonResponse);
        return true;
    }

    /**
     * Processes opponent disconnection notifications for the specified game.
     * 
     * @param gameId the game ID to check
     * @param userSession the user's HTTP session
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @return true if a notification was sent, false otherwise
     * @throws IOException if response writing fails
     */
    private boolean processOpponentDisconnectNotification(String gameId, HttpSession userSession, 
                                                        HttpServletResponse httpResponse, String sessionIdentifier) 
                                                        throws IOException {
        
        String disconnectSessionKey = DISCONNECT_KEY_PREFIX + gameId;
        String disconnectMessage = (String) userSession.getAttribute(disconnectSessionKey);
        
        if (disconnectMessage == null) {
            return false;
        }
        
        // Consume immediately - this only happens once per game
        userSession.removeAttribute(disconnectSessionKey);
        
        WebLogger.info("GameNotificationServlet", sessionIdentifier, "Opponent disconnect notification for: " + gameId);
        
        String jsonResponse = String.format(
            "{\"type\":\"opponentDisconnected\",\"message\":\"%s\"}",
            escapeJsonString(disconnectMessage)
        );
        
        httpResponse.getWriter().write(jsonResponse);
        return true;
    }

    /**
     * Processes move error notifications for the specified game.
     * Only notifies on move errors - successful moves are handled silently.
     * 
     * @param gameId the game ID to check
     * @param userSession the user's HTTP session
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @return true if a notification was sent, false otherwise
     * @throws IOException if response writing fails
     */
    private boolean processMoveErrorNotification(String gameId, HttpSession userSession, 
                                               HttpServletResponse httpResponse, String sessionIdentifier) 
                                               throws IOException {
        
        String moveReplySessionKey = MOVE_REPLY_KEY_PREFIX + gameId;
        String moveReplyStatus = (String) userSession.getAttribute(moveReplySessionKey);
        
        if (moveReplyStatus == null) {
            return false;
        }
        
        // Always consume the move reply
        userSession.removeAttribute(moveReplySessionKey);
        
        // Only notify if it's an error - successful moves are silent
        if ("Accepted".equals(moveReplyStatus)) {
            return false;
        }
        
        WebLogger.info("GameNotificationServlet", sessionIdentifier, 
                      "Move error notification for " + gameId + " - " + moveReplyStatus);
        
        String jsonResponse = String.format(
            "{\"type\":\"moveError\",\"status\":\"%s\"}",
            escapeJsonString(moveReplyStatus)
        );
        
        httpResponse.getWriter().write(jsonResponse);
        return true;
    }

    /**
     * Sends a response indicating no notifications are available.
     * 
     * @param httpResponse the HTTP response object
     * @throws IOException if response writing fails
     */
    private void sendNoNotificationResponse(HttpServletResponse httpResponse) throws IOException {
        httpResponse.getWriter().write("{\"type\":\"none\"}");
    }

    /**
     * Validates that the provided game ID is not null or empty.
     * 
     * @param gameId the game ID to validate
     * @return true if the game ID is valid, false otherwise
     */
    private boolean isValidGameId(String gameId) {
        return gameId != null && !gameId.trim().isEmpty();
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
     * Configures the HTTP response for JSON content.
     * 
     * @param httpResponse the response to configure
     */
    private void configureJsonResponse(HttpServletResponse httpResponse) {
        httpResponse.setContentType(CONTENT_TYPE_JSON);
        httpResponse.setCharacterEncoding(CHARSET_UTF8);
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
     * @param gameId the game ID being processed when error occurred
     * @param sessionIdentifier the session ID for logging
     * @param exception the exception that occurred
     * @param httpResponse the response object
     * @throws IOException if writing the error response fails
     */
    private void handleServletException(String gameId, String sessionIdentifier, Exception exception, 
                                       HttpServletResponse httpResponse) throws IOException {
        WebLogger.error("GameNotificationServlet", sessionIdentifier, 
                       "Error processing notifications for gameId " + gameId, exception);
        sendErrorResponse(httpResponse, "Failed to get notifications");
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
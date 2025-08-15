package web.servlets.api;

import java.io.IOException;
import java.util.List;

import core.GameState;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import web.utils.WebLogger;

/**
 * API servlet for retrieving game state information for specific games.
 * 
 * This servlet provides real-time game state data for active games through
 * a RESTful GET endpoint. The game state includes board configuration,
 * player turn information, remaining time for each player, and game metadata.
 * 
 * The servlet follows the KISS principle by using game-specific session keys
 * rather than global state management. Each game maintains its own state
 * independently, allowing multiple simultaneous games per session.
 * 
 * Required parameters:
 * - gameId: The unique identifier of the game whose state is requested
 * 
 * Authentication:
 * - Requires valid session with username attribute
 * 
 * Response format:
 * - Success: JSON object containing complete game state
 * - Error: JSON object with error message
 * - Not found: JSON object with null gameState
 */
@WebServlet("/api/gamestate")
public class GameStateServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String GAME_STATE_KEY_PREFIX = "gameState_";

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        configureJsonResponse(httpResponse);
        
        String requestedGameId = httpRequest.getParameter("gameId");
        if (!isValidGameId(requestedGameId)) {
            sendErrorResponse(httpResponse, "No gameId provided");
            return;
        }

        if (!isUserAuthenticated(httpRequest)) {
            sendErrorResponse(httpResponse, "Not logged in");
            return;
        }

        String sessionIdentifier = httpRequest.getSession().getId();
        WebLogger.info("GameStateServlet", sessionIdentifier, "Game state request for gameId: " + requestedGameId);

        try {
            processGameStateRequest(requestedGameId, httpRequest.getSession(), httpResponse, sessionIdentifier);
        } catch (Exception exception) {
            handleServletException(requestedGameId, sessionIdentifier, exception, httpResponse);
        }
    }

    /**
     * Processes the game state request by retrieving and formatting the game state.
     * 
     * @param gameId the ID of the game to retrieve state for
     * @param userSession the user's HTTP session
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if response writing fails
     */
    private void processGameStateRequest(String gameId, HttpSession userSession, 
                                        HttpServletResponse httpResponse, String sessionIdentifier) 
                                        throws IOException {
        
        String gameStateSessionKey = GAME_STATE_KEY_PREFIX + gameId;
        GameState currentGameState = (GameState) userSession.getAttribute(gameStateSessionKey);

        if (currentGameState != null) {
            sendGameStateResponse(currentGameState, httpResponse, sessionIdentifier);
        } else {
            sendNoGameStateResponse(gameId, httpResponse, sessionIdentifier);
        }
    }

    /**
     * Sends a successful game state response with complete game data.
     * 
     * @param gameState the game state to serialize
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if response writing fails
     */
    private void sendGameStateResponse(GameState gameState, HttpServletResponse httpResponse, 
                                      String sessionIdentifier) throws IOException {
        
        String gameStateJson = buildGameStateJsonResponse(gameState);
        
        WebLogger.info("GameStateServlet", sessionIdentifier, 
                      String.format("Returning state for game %s - nextPlayer: %s - blackTime: %dms - whiteTime: %dms",
                                   gameState.getGameId(),
                                   gameState.getNextPlayerColor(),
                                   gameState.getBlackPlayerTimeRemaining().toMillis(),
                                   gameState.getWhitePlayerTimeRemaining().toMillis()));
        
        httpResponse.getWriter().write(gameStateJson);
    }

    /**
     * Sends a response indicating no game state was found for the requested game.
     * 
     * @param gameId the game ID that was not found
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if response writing fails
     */
    private void sendNoGameStateResponse(String gameId, HttpServletResponse httpResponse, 
                                        String sessionIdentifier) throws IOException {
        
        WebLogger.info("GameStateServlet", sessionIdentifier, "No state found for game: " + gameId);
        httpResponse.getWriter().write("{\"gameState\":null}");
    }

    /**
     * Builds a complete JSON response containing all game state information.
     * 
     * @param gameState the game state to serialize
     * @return JSON string representation of the game state
     */
    private String buildGameStateJsonResponse(GameState gameState) {
        StringBuilder jsonBuilder = new StringBuilder();
        
        jsonBuilder.append("{\"gameState\":{");
        appendGameMetadata(jsonBuilder, gameState);
        jsonBuilder.append(",");
        appendPlayerTiming(jsonBuilder, gameState);
        jsonBuilder.append(",");
        appendBoardConfiguration(jsonBuilder, gameState);
        jsonBuilder.append("}}");
        
        return jsonBuilder.toString();
    }

    /**
     * Appends game metadata to the JSON response.
     * 
     * @param jsonBuilder the JSON builder to append to
     * @param gameState the game state containing metadata
     */
    private void appendGameMetadata(StringBuilder jsonBuilder, GameState gameState) {
        jsonBuilder.append("\"gameId\":\"").append(escapeJsonString(gameState.getGameId())).append("\",");
        jsonBuilder.append("\"nextPlayerColor\":\"").append(escapeJsonString(gameState.getNextPlayerColor())).append("\",");
        jsonBuilder.append("\"timestamp\":\"").append(gameState.getTimestamp().toString()).append("\"");
    }

    /**
     * Appends player timing information to the JSON response.
     * 
     * @param jsonBuilder the JSON builder to append to
     * @param gameState the game state containing timing data
     */
    private void appendPlayerTiming(StringBuilder jsonBuilder, GameState gameState) {
        jsonBuilder.append("\"blackTimeRemaining\":").append(gameState.getBlackPlayerTimeRemaining().toMillis());
        jsonBuilder.append(",");
        jsonBuilder.append("\"whiteTimeRemaining\":").append(gameState.getWhitePlayerTimeRemaining().toMillis());
    }

    /**
     * Appends board configuration to the JSON response.
     * 
     * @param jsonBuilder the JSON builder to append to
     * @param gameState the game state containing board data
     */
    private void appendBoardConfiguration(StringBuilder jsonBuilder, GameState gameState) {
        jsonBuilder.append("\"boardRows\":[");
        
        List<String> boardRows = gameState.getBoardRows();
        for (int rowIndex = 0; rowIndex < boardRows.size(); rowIndex++) {
            if (rowIndex > 0) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\"").append(escapeJsonString(boardRows.get(rowIndex))).append("\"");
        }
        
        jsonBuilder.append("]");
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
        WebLogger.error("GameStateServlet", sessionIdentifier, 
                       "Error processing game state for gameId " + gameId, exception);
        sendErrorResponse(httpResponse, "Failed to get game state");
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
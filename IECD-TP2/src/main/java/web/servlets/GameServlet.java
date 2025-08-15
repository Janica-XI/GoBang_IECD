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
 * Handles the main game interface and gameplay interactions for the GoBang web application.
 * 
 * This servlet manages the core gaming experience, including game board display,
 * move processing, and game state management. It serves as the primary interface
 * between players and the ongoing game, handling both game visualization and
 * real-time player interactions.
 * 
 * GET requests:
 * - Displays the game interface (game.jsp) with complete game context
 * - Processes game parameters (gameId, player names, roles)
 * - Prepares comprehensive game data including player profiles and photos
 * - Determines player roles (black, white, or spectator)
 * - Handles authentication and parameter validation
 * 
 * POST requests:
 * - Processes game actions (move, forfeit)
 * - Validates move coordinates and game state
 * - Communicates with game server for move validation
 * - Provides real-time feedback on action success/failure
 * 
 * Features:
 * - Complete game context preparation with player profiles
 * - Support for multiple simultaneous games per session
 * - Real-time move validation and server communication
 * - Player role determination (Black/White/Spectator)
 * - Photo fallback system for player avatars
 * - Comprehensive error handling and validation
 * - Game timestamp tracking for session management
 * - Forfeit functionality for game termination
 * 
 * Game Data Management:
 * - Uses game-specific session keys for multi-game support
 * - Prioritizes complete player profiles over basic data
 * - Fallback mechanisms for missing player information
 * - Efficient photo handling with Base64 encoding
 * 
 * Security:
 * - Requires authenticated user (loggedInPlayer in session)
 * - Validates all game parameters before processing
 * - Server-side validation of all moves and actions
 * - Proper session state management per game
 */
@WebServlet("/game")
public class GameServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final String GAME_VIEW = "game.jsp";
    private static final String LOGIN_REDIRECT_PATH = "login";
    private static final String LOBBY_REDIRECT_PATH = "lobby";
    
    private static final String PARAM_GAME_ID = "gameId";
    private static final String PARAM_BLACK_PLAYER = "blackPlayer";
    private static final String PARAM_WHITE_PLAYER = "whitePlayer";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ROW = "row";
    private static final String PARAM_COL = "col";
    
    private static final String ATTR_LOGGED_IN_PLAYER = "loggedInPlayer";
    private static final String ATTR_USERNAME = "username";
    private static final String ATTR_CURRENT_PLAYER_LIST = "currentPlayerList";
    private static final String ATTR_GAME_DATA = "gameData";
    
    private static final String ACTION_MOVE = "move";
    private static final String ACTION_FORFEIT = "forfeit";
    
    private static final String STATUS_ACCEPTED = "Accepted";
    private static final String HTTP_STATUS_OK = "OK";
    
    private static final String COLOR_BLACK = "Black";
    private static final String COLOR_WHITE = "White";
    private static final String COLOR_SPECTATOR = "Spectator";
    
    private static final String DEFAULT_PHOTO_PATH = "images/default.jpg";
    private static final String PHOTO_DATA_PREFIX = "data:image/jpeg;base64,";
    
    private static final int MOVE_RESPONSE_WAIT_MS = 200;
    
    private static final String ERROR_MISSING_PARAMETERS = "Missing parameters";
    private static final String ERROR_INVALID_COORDINATES = "Invalid coordinates";
    private static final String ERROR_NO_CONNECTION = "No connection";
    private static final String ERROR_UNKNOWN_ACTION = "Unknown action";
    private static final String ERROR_MISSING_ACTION = "Missing action parameter";
    private static final String ERROR_NO_GAME_ID = "No gameId provided";

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String sessionIdentifier = getSessionIdentifier(httpRequest);
        WebLogger.info("GameServlet", sessionIdentifier, "Game page request received");

        if (!isUserAuthenticated(httpRequest)) {
            redirectToLogin(httpResponse, sessionIdentifier);
            return;
        }

        if (!validateGameParameters(httpRequest, httpResponse, sessionIdentifier)) {
            return; // Validation failed, response already sent
        }

        try {
            GameData gameData = prepareGameData(httpRequest, sessionIdentifier);
            httpRequest.setAttribute(ATTR_GAME_DATA, gameData);
            
            WebLogger.info("GameServlet", sessionIdentifier, "Game data prepared - forwarding to game view");
            forwardToGameView(httpRequest, httpResponse);
            
        } catch (Exception exception) {
            handleGameException(sessionIdentifier, exception, httpResponse);
        }
    }

    @Override
    protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String requestedAction = httpRequest.getParameter(PARAM_ACTION);
        String sessionIdentifier = getSessionIdentifier(httpRequest);
        
        WebLogger.info("GameServlet", sessionIdentifier, "Game POST action: " + requestedAction);

        if (requestedAction == null) {
            sendErrorResponse(httpResponse, 400, ERROR_MISSING_ACTION);
            return;
        }

        try {
            WebClientConnection serverConnection = validateConnectionForAction(httpRequest, sessionIdentifier);
            processGameAction(requestedAction, httpRequest, httpResponse, serverConnection, sessionIdentifier);
            
        } catch (Exception exception) {
            handleActionException(sessionIdentifier, exception, httpResponse);
        }
    }

    /**
     * Validates required game parameters from the request.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @return true if parameters are valid, false otherwise
     * @throws IOException if redirect fails
     */
    private boolean validateGameParameters(HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
                                         String sessionIdentifier) throws IOException {
        
        String gameId = httpRequest.getParameter(PARAM_GAME_ID);
        String blackPlayerName = httpRequest.getParameter(PARAM_BLACK_PLAYER);
        String whitePlayerName = httpRequest.getParameter(PARAM_WHITE_PLAYER);

        WebLogger.info("GameServlet", sessionIdentifier, 
                      String.format("Game parameters - GameId: %s, Black: %s, White: %s", 
                                   gameId, blackPlayerName, whitePlayerName));

        if (gameId == null || blackPlayerName == null || whitePlayerName == null) {
            WebLogger.warning("GameServlet", sessionIdentifier, "Missing game parameters - redirecting to lobby");
            httpResponse.sendRedirect(LOBBY_REDIRECT_PATH);
            return false;
        }

        return true;
    }

    /**
     * Prepares comprehensive game data for display.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     * @return prepared GameData object
     */
    private GameData prepareGameData(HttpServletRequest httpRequest, String sessionIdentifier) {
        String gameId = httpRequest.getParameter(PARAM_GAME_ID);
        String blackPlayerName = httpRequest.getParameter(PARAM_BLACK_PLAYER);
        String whitePlayerName = httpRequest.getParameter(PARAM_WHITE_PLAYER);
        String currentUsername = (String) httpRequest.getSession().getAttribute(ATTR_USERNAME);

        GameData gameData = new GameData();
        gameData.setGameId(gameId);
        gameData.setCurrentUser(currentUsername);
        
        // Try to get game timestamp
        Long gameTimestamp = (Long) httpRequest.getSession().getAttribute("game_" + gameId + "_startTimestamp");
        if (gameTimestamp != null) {
            gameData.setGameTimestamp(gameTimestamp);
        }

        // Try to get complete player profiles first
        if (loadCompletePlayerProfiles(httpRequest, gameData, gameId, sessionIdentifier)) {
            WebLogger.info("GameServlet", sessionIdentifier, "Using complete player profiles from session");
        } else {
            loadPlayerDataWithFallback(httpRequest, gameData, blackPlayerName, whitePlayerName, sessionIdentifier);
        }

        // Determine player roles and color assignment
        determinePlayerRoles(gameData, currentUsername);

        return gameData;
    }

    /**
     * Attempts to load complete player profiles from session data.
     * 
     * @param httpRequest the HTTP request object
     * @param gameData the game data object to populate
     * @param gameId the game identifier
     * @param sessionIdentifier the session ID for logging
     * @return true if complete profiles were loaded, false otherwise
     */
    private boolean loadCompletePlayerProfiles(HttpServletRequest httpRequest, GameData gameData, 
                                             String gameId, String sessionIdentifier) {
        
        Player blackPlayer = (Player) httpRequest.getSession().getAttribute("game_" + gameId + "_blackPlayerProfile");
        Player whitePlayer = (Player) httpRequest.getSession().getAttribute("game_" + gameId + "_whitePlayerProfile");

        if (blackPlayer != null && whitePlayer != null) {
            gameData.setBlackPlayer(blackPlayer.getUsername());
            gameData.setWhitePlayer(whitePlayer.getUsername());
            gameData.setBlackPlayerPhoto(blackPlayer.getPhotoBase64());
            gameData.setWhitePlayerPhoto(whitePlayer.getPhotoBase64());
            return true;
        }

        return false;
    }

    /**
     * Loads player data with fallback to cached player list for photos.
     * 
     * @param httpRequest the HTTP request object
     * @param gameData the game data object to populate
     * @param blackPlayerName the black player's username
     * @param whitePlayerName the white player's username
     * @param sessionIdentifier the session ID for logging
     */
    @SuppressWarnings("unchecked")
    private void loadPlayerDataWithFallback(HttpServletRequest httpRequest, GameData gameData,
                                          String blackPlayerName, String whitePlayerName, 
                                          String sessionIdentifier) {
        
        gameData.setBlackPlayer(blackPlayerName);
        gameData.setWhitePlayer(whitePlayerName);

        // Try to get photos from cached player list
        List<Player> cachedPlayerList = (List<Player>) httpRequest.getSession().getAttribute(ATTR_CURRENT_PLAYER_LIST);

        if (cachedPlayerList != null) {
            for (Player player : cachedPlayerList) {
                if (player.getUsername().equals(blackPlayerName)) {
                    gameData.setBlackPlayerPhoto(player.getPhotoBase64());
                } else if (player.getUsername().equals(whitePlayerName)) {
                    gameData.setWhitePlayerPhoto(player.getPhotoBase64());
                }
            }
        }

        WebLogger.info("GameServlet", sessionIdentifier, "Using fallback method for player photos");
    }

    /**
     * Determines player roles and assigns color for the current user.
     * 
     * @param gameData the game data object to update
     * @param currentUsername the current user's username
     */
    private void determinePlayerRoles(GameData gameData, String currentUsername) {
        boolean isBlackPlayer = currentUsername != null && currentUsername.equals(gameData.getBlackPlayer());
        boolean isWhitePlayer = currentUsername != null && currentUsername.equals(gameData.getWhitePlayer());

        gameData.setIsBlackPlayer(isBlackPlayer);
        gameData.setIsWhitePlayer(isWhitePlayer);

        if (isBlackPlayer) {
            gameData.setMyColor(COLOR_BLACK);
        } else if (isWhitePlayer) {
            gameData.setMyColor(COLOR_WHITE);
        } else {
            gameData.setMyColor(COLOR_SPECTATOR);
        }
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
            WebLogger.error("GameServlet", sessionIdentifier, "No active connection for game action");
            throw new Exception(ERROR_NO_CONNECTION);
        }
        
        return connection;
    }

    /**
     * Processes the requested game action by dispatching to appropriate handlers.
     * 
     * @param requestedAction the action to process
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param serverConnection the server connection
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if action processing fails
     * @throws IOException if response writing fails
     */
    private void processGameAction(String requestedAction, HttpServletRequest httpRequest, 
                                 HttpServletResponse httpResponse, WebClientConnection serverConnection, 
                                 String sessionIdentifier) throws Exception, IOException {
        
        switch (requestedAction) {
            case ACTION_MOVE:
                handleMoveAction(httpRequest, httpResponse, serverConnection, sessionIdentifier);
                break;
            case ACTION_FORFEIT:
                handleForfeitAction(httpRequest, httpResponse, serverConnection, sessionIdentifier);
                break;
            default:
                WebLogger.warning("GameServlet", sessionIdentifier, "Unknown action: " + requestedAction);
                sendErrorResponse(httpResponse, 400, ERROR_UNKNOWN_ACTION);
                break;
        }
    }

    /**
     * Handles move action by processing coordinates and communicating with server.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param serverConnection the server connection
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if move processing fails
     * @throws IOException if response writing fails
     */
    private void handleMoveAction(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                WebClientConnection serverConnection, String sessionIdentifier) 
                                throws Exception, IOException {
        
        String gameId = httpRequest.getParameter(PARAM_GAME_ID);
        String rowString = httpRequest.getParameter(PARAM_ROW);
        String colString = httpRequest.getParameter(PARAM_COL);

        if (gameId == null || rowString == null || colString == null) {
            sendErrorResponse(httpResponse, 400, ERROR_MISSING_PARAMETERS);
            return;
        }

        try {
            int row = Integer.parseInt(rowString);
            int col = Integer.parseInt(colString);

            WebLogger.info("GameServlet", sessionIdentifier, 
                          String.format("Sending move: gameId=%s, row=%d, col=%d", gameId, row, col));

            processMoveRequest(gameId, row, col, httpRequest.getSession(), serverConnection, sessionIdentifier);
            
            String moveResponse = waitForMoveResponse(gameId, httpRequest.getSession(), sessionIdentifier);
            sendMoveResponse(moveResponse, httpResponse);

        } catch (NumberFormatException exception) {
            sendErrorResponse(httpResponse, 400, ERROR_INVALID_COORDINATES);
        }
    }

    /**
     * Processes a move request by clearing previous state and sending to server.
     * 
     * @param gameId the game identifier
     * @param row the row coordinate
     * @param col the column coordinate
     * @param userSession the user's HTTP session
     * @param serverConnection the server connection
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if move request fails
     */
    private void processMoveRequest(String gameId, int row, int col, HttpSession userSession,
                                  WebClientConnection serverConnection, String sessionIdentifier) throws Exception {
        
        // Clear any previous move response for this game
        String moveReplyKey = "moveReply_" + gameId;
        userSession.removeAttribute(moveReplyKey);

        // Send move request to server
        serverConnection.getCommunicationProtocol().sendMoveRequest(gameId, row, col);
    }

    /**
     * Waits for and retrieves the move response from the server.
     * 
     * @param gameId the game identifier
     * @param userSession the user's HTTP session
     * @param sessionIdentifier the session ID for logging
     * @return the move response status
     */
    private String waitForMoveResponse(String gameId, HttpSession userSession, String sessionIdentifier) {
        try {
            Thread.sleep(MOVE_RESPONSE_WAIT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        String moveReplyKey = "moveReply_" + gameId;
        String moveStatus = (String) userSession.getAttribute(moveReplyKey);
        
        WebLogger.info("GameServlet", sessionIdentifier, "Move response after wait: " + moveStatus);
        return moveStatus;
    }

    /**
     * Sends the appropriate response based on move validation result.
     * 
     * @param moveStatus the move validation status from server
     * @param httpResponse the HTTP response object
     * @throws IOException if response writing fails
     */
    private void sendMoveResponse(String moveStatus, HttpServletResponse httpResponse) throws IOException {
        if (moveStatus != null && !STATUS_ACCEPTED.equals(moveStatus)) {
            sendErrorResponse(httpResponse, 400, "Error: " + moveStatus);
        } else {
            sendSuccessResponse(httpResponse);
        }
    }

    /**
     * Handles forfeit action by communicating game abandonment to server.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param serverConnection the server connection
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if forfeit processing fails
     * @throws IOException if response writing fails
     */
    private void handleForfeitAction(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                   WebClientConnection serverConnection, String sessionIdentifier) 
                                   throws Exception, IOException {
        
        String gameId = httpRequest.getParameter(PARAM_GAME_ID);
        
        if (gameId == null) {
            sendErrorResponse(httpResponse, 400, ERROR_NO_GAME_ID);
            return;
        }

        WebLogger.info("GameServlet", sessionIdentifier, "Forfeit request for gameId: " + gameId);
        
        serverConnection.getCommunicationProtocol().sendForfeitMatchRequest(gameId);
        WebLogger.info("GameServlet", sessionIdentifier, "Forfeit request sent for game: " + gameId);

        sendSuccessResponse(httpResponse);
    }

    /**
     * Checks if the current user is authenticated.
     * 
     * @param httpRequest the HTTP request to check
     * @return true if user is authenticated, false otherwise
     */
    private boolean isUserAuthenticated(HttpServletRequest httpRequest) {
        return httpRequest.getSession().getAttribute(ATTR_LOGGED_IN_PLAYER) != null;
    }

    /**
     * Redirects unauthenticated users to the login page.
     * 
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if redirect fails
     */
    private void redirectToLogin(HttpServletResponse httpResponse, String sessionIdentifier) throws IOException {
        WebLogger.info("GameServlet", sessionIdentifier, "User not authenticated - redirecting to login");
        httpResponse.sendRedirect(LOGIN_REDIRECT_PATH);
    }

    /**
     * Forwards the request to the game view.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void forwardToGameView(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws ServletException, IOException {
        
        httpRequest.getRequestDispatcher(GAME_VIEW).forward(httpRequest, httpResponse);
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
     * Sends an error response with specified status code and message.
     * 
     * @param httpResponse the HTTP response object
     * @param statusCode the HTTP status code
     * @param errorMessage the error message
     * @throws IOException if writing fails
     */
    private void sendErrorResponse(HttpServletResponse httpResponse, int statusCode, String errorMessage) 
            throws IOException {
        
        httpResponse.setStatus(statusCode);
        httpResponse.getWriter().write(errorMessage);
    }

    /**
     * Handles exceptions that occur during game display processing.
     * 
     * @param sessionIdentifier the session ID for logging
     * @param exception the exception that occurred
     * @param httpResponse the HTTP response object
     * @throws IOException if error response cannot be sent
     */
    private void handleGameException(String sessionIdentifier, Exception exception, HttpServletResponse httpResponse) 
            throws IOException {
        
        WebLogger.error("GameServlet", sessionIdentifier, "Error processing game request", exception);
        httpResponse.sendRedirect(LOBBY_REDIRECT_PATH);
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
        
        WebLogger.error("GameServlet", sessionIdentifier, "Error processing game action", exception);
        sendErrorResponse(httpResponse, 500, "Error: " + exception.getMessage());
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

    /**
     * Simple data class for game context and player information (KISS principle).
     * 
     * This class encapsulates all the game-related data needed by the JSP view,
     * including player information, roles, photos, and game metadata. It provides
     * convenient helper methods for the view layer while maintaining simplicity.
     */
    public static class GameData {
        private String gameId;
        private String blackPlayer;
        private String whitePlayer;
        private String currentUser;
        private boolean isBlackPlayer;
        private boolean isWhitePlayer;
        private String myColor;
        private String blackPlayerPhoto = "";
        private String whitePlayerPhoto = "";
        private long gameTimestamp;

        // Game identification methods
        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }

        /**
         * Returns a shortened version of the game ID for display purposes.
         * 
         * @return shortened game ID with ellipsis if longer than 8 characters
         */
        public String getGameIdShort() {
            return gameId != null && gameId.length() > 8 ? gameId.substring(0, 8) + "..." : gameId;
        }

        // Player information methods
        public String getBlackPlayer() { return blackPlayer; }
        public void setBlackPlayer(String blackPlayer) { this.blackPlayer = blackPlayer; }

        public String getWhitePlayer() { return whitePlayer; }
        public void setWhitePlayer(String whitePlayer) { this.whitePlayer = whitePlayer; }

        public String getCurrentUser() { return currentUser; }
        public void setCurrentUser(String currentUser) { this.currentUser = currentUser; }

        // Player role methods
        public boolean isBlackPlayer() { return isBlackPlayer; }
        public void setIsBlackPlayer(boolean isBlackPlayer) { this.isBlackPlayer = isBlackPlayer; }

        public boolean isWhitePlayer() { return isWhitePlayer; }
        public void setIsWhitePlayer(boolean isWhitePlayer) { this.isWhitePlayer = isWhitePlayer; }

        public String getMyColor() { return myColor; }
        public void setMyColor(String myColor) { this.myColor = myColor; }

        // Photo handling methods
        public String getBlackPlayerPhoto() { return blackPlayerPhoto; }
        public void setBlackPlayerPhoto(String blackPlayerPhoto) { 
            this.blackPlayerPhoto = blackPlayerPhoto != null ? blackPlayerPhoto : ""; 
        }

        public String getWhitePlayerPhoto() { return whitePlayerPhoto; }
        public void setWhitePlayerPhoto(String whitePlayerPhoto) { 
            this.whitePlayerPhoto = whitePlayerPhoto != null ? whitePlayerPhoto : ""; 
        }

        /**
         * Returns the complete image source URL for the black player's photo.
         * 
         * @return Base64 data URL or default image path
         */
        public String getBlackPlayerPhotoSrc() {
            return !blackPlayerPhoto.isEmpty() ? PHOTO_DATA_PREFIX + blackPlayerPhoto : DEFAULT_PHOTO_PATH;
        }

        /**
         * Returns the complete image source URL for the white player's photo.
         * 
         * @return Base64 data URL or default image path
         */
        public String getWhitePlayerPhotoSrc() {
            return !whitePlayerPhoto.isEmpty() ? PHOTO_DATA_PREFIX + whitePlayerPhoto : DEFAULT_PHOTO_PATH;
        }

        // Game metadata methods
        public long getGameTimestamp() { return gameTimestamp; }
        public void setGameTimestamp(long gameTimestamp) { this.gameTimestamp = gameTimestamp; }
    }
}
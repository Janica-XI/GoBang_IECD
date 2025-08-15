package web.utils;

import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import communication.client.ClientProtocolHandler;
import core.GameState;
import core.Player;
import jakarta.servlet.http.HttpSession;

/**
 * Web-based implementation of ClientProtocolHandler for handling server messages.
 * 
 * This handler manages multiple simultaneous games through game-specific session keys
 * and follows the KISS principle where data is consumed immediately via polling.
 * Each handler instance is bound to a specific HTTP session and stores all received
 * data as session attributes for retrieval by servlets.
 * 
 * Key responsibilities:
 * - Processing all server protocol messages and storing them in session
 * - Managing game-specific data using gameId-based keys
 * - Handling lobby notifications through a concurrent queue
 * - Cleaning up game data when games end or players disconnect
 * - Maintaining session state for authentication and player readiness
 */
public class WebClientHandler implements ClientProtocolHandler {

    private final HttpSession httpSession;
    private final String sessionIdentifier;
    
    /**
     * Queue for generic lobby notifications that don't belong to specific games.
     * Other data uses game-specific keys to support multiple simultaneous games.
     */
    private final Queue<String> lobbyNotificationQueue = new ConcurrentLinkedQueue<>();

    /**
     * Creates a new web client handler for the given HTTP session.
     * Initializes the lobby notification queue and stores it in the session.
     * 
     * @param httpSession the HTTP session this handler will manage
     */
    public WebClientHandler(HttpSession httpSession) {
        this.httpSession = httpSession;
        this.sessionIdentifier = httpSession.getId();
        this.httpSession.setAttribute("lobbyNotifications", lobbyNotificationQueue);
        
        WebLogger.info("WebClientHandler", sessionIdentifier, "Handler created and initialized");
    }

    @Override
    public void handleLoginReply(String loginStatus, Player playerProfile) {
        WebLogger.info("WebClientHandler", sessionIdentifier, "Login reply: " + loginStatus);
        
        httpSession.setAttribute("lastLoginStatus", loginStatus);
        httpSession.setAttribute("lastLoginProfile", playerProfile);
        
        if ("Accepted".equals(loginStatus) && playerProfile != null) {
            httpSession.setAttribute("loggedInPlayer", playerProfile);
            httpSession.setAttribute("username", playerProfile.getUsername());
            WebLogger.info("WebClientHandler", sessionIdentifier, "Login successful for: " + playerProfile.getUsername());
        }
    }

    @Override
    public void handleLogoutReply(String logoutStatus) {
        WebLogger.info("WebClientHandler", sessionIdentifier, "Logout reply: " + logoutStatus);
        httpSession.setAttribute("lastLogoutStatus", logoutStatus);

        if ("Accepted".equals(logoutStatus)) {
            clearSessionDataOnLogout();
            WebLogger.info("WebClientHandler", sessionIdentifier, "Session data cleared after logout");
        }
    }

    @Override
    public void handleRegisterReply(String registerStatus) {
        WebLogger.info("WebClientHandler", sessionIdentifier, "Register reply: " + registerStatus);
        httpSession.setAttribute("lastRegisterStatus", registerStatus);
    }

    @Override
    public void handleUpdateProfileReply(String updateProfileStatus) {
        WebLogger.info("WebClientHandler", sessionIdentifier, "Profile update reply: " + updateProfileStatus);
        httpSession.setAttribute("lastProfileUpdateStatus", updateProfileStatus);
    }

    @Override
    public void handleUpdatePhotoReply(String updatePhotoStatus) {
        WebLogger.info("WebClientHandler", sessionIdentifier, "Photo update reply: " + updatePhotoStatus);
        httpSession.setAttribute("lastPhotoUpdateStatus", updatePhotoStatus);
    }

    @Override
    public void handleListPlayersReply(List<Player> playerList) {
        WebLogger.info("WebClientHandler", sessionIdentifier, "Player list received: " + playerList.size() + " players");
        
        httpSession.setAttribute("currentPlayerList", playerList);
        
        if (!playerList.isEmpty()) {
            logPlayerListNames(playerList);
        }
    }

    @Override
    public void handleLeaderboardReply(List<Player> leaderboardPlayers) {
        WebLogger.info("WebClientHandler", sessionIdentifier, "Leaderboard received: " + leaderboardPlayers.size() + " players");
        httpSession.setAttribute("currentLeaderboard", leaderboardPlayers);
    }

    @Override
    public void handleReadyReply(String readyStatus) {
        WebLogger.info("WebClientHandler", sessionIdentifier, "Ready reply: " + readyStatus);
        httpSession.setAttribute("lastReadyStatus", readyStatus);
        
        if (!"Accepted".equals(readyStatus)) {
            httpSession.removeAttribute("isReady");
        }
    }

    @Override
    public void handleChallengeInvitation(Player challengerProfile) {
        WebLogger.info("WebClientHandler", sessionIdentifier, 
                      "Challenge invitation from: " + challengerProfile.getUsername());
        
        httpSession.setAttribute("pendingChallengeInvitation", challengerProfile);
        WebLogger.info("WebClientHandler", sessionIdentifier, 
                      "Challenge invitation stored from: " + challengerProfile.getUsername());
    }

    @Override
    public void handleChallengeReply(String challengeStatus) {
        WebLogger.info("WebClientHandler", sessionIdentifier, "Challenge reply: " + challengeStatus);
        
        httpSession.setAttribute("lastChallengeReply", challengeStatus);
        
        if ("Rejected".equals(challengeStatus) || "Canceled".equals(challengeStatus)) {
            clearChallengeStateData();
            WebLogger.info("WebClientHandler", sessionIdentifier, 
                          "Challenge state cleared due to: " + challengeStatus);
        }
    }

    @Override
    public void handleGameStarted(String gameId, Player blackPlayer, Player whitePlayer) {
        WebLogger.info("WebClientHandler", sessionIdentifier, 
                      "Game started: " + gameId + " (" + blackPlayer.getUsername() + " vs " + whitePlayer.getUsername() + ")");
        
        storeGameStartData(gameId, blackPlayer, whitePlayer);
        clearLobbyStateData();
        
        WebLogger.info("WebClientHandler", sessionIdentifier, 
                      "Game " + gameId + " registered with complete profiles and timestamp: " + System.currentTimeMillis());
    }

    @Override
    public void handleGameState(GameState gameState) {
        String gameId = gameState.getGameId();
        WebLogger.info("WebClientHandler", sessionIdentifier, "Game state update for game: " + gameId);
        
        httpSession.setAttribute("gameState_" + gameId, gameState);
        WebLogger.info("WebClientHandler", sessionIdentifier, "GameState stored for game: " + gameId);
    }

    @Override
    public void handleMoveReply(String gameId, String moveStatus) {
        WebLogger.info("WebClientHandler", sessionIdentifier, 
                      "Move reply for game " + gameId + ": " + moveStatus);
        
        httpSession.setAttribute("moveReply_" + gameId, moveStatus);
    }

    @Override
    public void handleEndGameNotification(String gameId, String winnerUsername, Duration totalTime) {
        WebLogger.info("WebClientHandler", sessionIdentifier, 
                      "Game ended: " + gameId + " winner: " + winnerUsername);
        
        String gameEndMessage = createGameEndMessage(gameId, winnerUsername);
        httpSession.setAttribute("gameEnd_" + gameId, gameEndMessage);
        
        cleanupGameSpecificData(gameId);
        WebLogger.info("WebClientHandler", sessionIdentifier, "Game " + gameId + " ended and cleaned up");
    }

    @Override
    public void handleOpponentDisconnected(String gameId, String description) {
        WebLogger.info("WebClientHandler", sessionIdentifier, 
                      "Opponent disconnected in game " + gameId + ": " + description);
        
        httpSession.setAttribute("disconnect_" + gameId, description);
        cleanupGameSpecificData(gameId);
        
        WebLogger.info("WebClientHandler", sessionIdentifier, "Disconnection registered for game: " + gameId);
    }

    @Override
    public void handleProfileUpdateNotification(Player updatedProfile) {
        WebLogger.info("WebClientHandler", sessionIdentifier, 
                      "Profile updated: " + updatedProfile.getUsername() + 
                      " (V:" + updatedProfile.getVictories() + ", D:" + updatedProfile.getDefeats() + ")");
        
        httpSession.setAttribute("loggedInPlayer", updatedProfile);
        httpSession.setAttribute("updatedProfile", updatedProfile);
    }

    @Override
    public void handleErrorNotification(String errorCode, String description) {
        WebLogger.info("WebClientHandler", sessionIdentifier, 
                      "Error notification: " + errorCode + ": " + description);
        
        lobbyNotificationQueue.offer(errorCode + ": " + description);
    }

    /**
     * Removes all data associated with a specific game from the session.
     * Called when a game ends or there is a disconnection.
     * 
     * @param gameId the identifier of the game to clean up
     */
    private void cleanupGameSpecificData(String gameId) {
        httpSession.removeAttribute("game_" + gameId + "_blackPlayer");
        httpSession.removeAttribute("game_" + gameId + "_whitePlayer");
        httpSession.removeAttribute("game_" + gameId + "_blackPlayerProfile");
        httpSession.removeAttribute("game_" + gameId + "_whitePlayerProfile");
        httpSession.removeAttribute("game_" + gameId + "_startTimestamp");
        httpSession.removeAttribute("gameState_" + gameId);
        httpSession.removeAttribute("moveReply_" + gameId);
        
        WebLogger.info("WebClientHandler", sessionIdentifier, "Cleaned up data for game: " + gameId);
    }

    /**
     * Clears session data when user logs out successfully.
     */
    private void clearSessionDataOnLogout() {
        httpSession.removeAttribute("loggedInPlayer");
        httpSession.removeAttribute("username");
        httpSession.removeAttribute("isReady");
        httpSession.removeAttribute("currentPlayerList");
        httpSession.removeAttribute("currentLeaderboard");
    }

    /**
     * Clears challenge-related state data from the session.
     */
    private void clearChallengeStateData() {
        httpSession.removeAttribute("sentChallengeTo");
        httpSession.removeAttribute("pendingChallengeInvitation");
    }

    /**
     * Clears lobby-related state data when a game starts.
     */
    private void clearLobbyStateData() {
        httpSession.removeAttribute("sentChallengeTo");
        httpSession.removeAttribute("pendingChallengeInvitation");
        httpSession.setAttribute("isReady", false);
    }

    /**
     * Stores all game start data in the session with appropriate keys.
     * 
     * @param gameId the game identifier
     * @param blackPlayer the black player profile
     * @param whitePlayer the white player profile
     */
    private void storeGameStartData(String gameId, Player blackPlayer, Player whitePlayer) {
        httpSession.setAttribute("pendingGameStart", gameId);
        
        // Store complete player profiles
        httpSession.setAttribute("game_" + gameId + "_blackPlayerProfile", blackPlayer);
        httpSession.setAttribute("game_" + gameId + "_whitePlayerProfile", whitePlayer);
        
        // Maintain compatibility for JavaScript (usernames only)
        httpSession.setAttribute("game_" + gameId + "_blackPlayer", blackPlayer.getUsername());
        httpSession.setAttribute("game_" + gameId + "_whitePlayer", whitePlayer.getUsername());
        
        // Store game start timestamp
        httpSession.setAttribute("game_" + gameId + "_startTimestamp", System.currentTimeMillis());
    }

    /**
     * Creates a formatted game end message for display.
     * 
     * @param gameId the game identifier
     * @param winnerUsername the username of the winner
     * @return formatted game end message
     */
    private String createGameEndMessage(String gameId, String winnerUsername) {
        String shortGameId = gameId.substring(0, Math.min(8, gameId.length()));
        return "Game " + shortGameId + "... ended. Winner: " + winnerUsername;
    }

    /**
     * Logs the names of players in the received player list for debugging.
     * 
     * @param playerList the list of players to log
     */
    private void logPlayerListNames(List<Player> playerList) {
        StringBuilder playerNames = new StringBuilder("Players: ");
        for (Player player : playerList) {
            playerNames.append(player.getUsername()).append(" ");
        }
        WebLogger.info("WebClientHandler", sessionIdentifier, playerNames.toString());
    }

    /**
     * Debug method to check current session state.
     * Useful during development and troubleshooting.
     */
    public void debugCurrentSessionState() {
        if (!WebDebugConfig.DEBUG) {
            return;
        }
        
        WebLogger.info("WebClientHandler", sessionIdentifier, "=== Session State Debug ===");
        WebLogger.info("WebClientHandler", sessionIdentifier, "Username: " + httpSession.getAttribute("username"));
        WebLogger.info("WebClientHandler", sessionIdentifier, "IsReady: " + httpSession.getAttribute("isReady"));
        WebLogger.info("WebClientHandler", sessionIdentifier, "PendingChallenger: " + httpSession.getAttribute("pendingChallenger"));
        WebLogger.info("WebClientHandler", sessionIdentifier, "SentChallengeTo: " + httpSession.getAttribute("sentChallengeTo"));
        WebLogger.info("WebClientHandler", sessionIdentifier, "Lobby notifications: " + lobbyNotificationQueue.size());
        WebLogger.info("WebClientHandler", sessionIdentifier, "=========================");
    }
}
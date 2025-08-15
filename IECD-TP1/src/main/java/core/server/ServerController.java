package core.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import communication.server.ServerProtocol;
import communication.server.ServerProtocolHandler;
import core.Player;

/**
 * Manages XML‐based communication for a single client connection. Each instance
 * runs on its own thread, reading requests, dispatching to handler methods, and
 * sending replies/notifications.
 */
public class ServerController extends Thread implements ServerProtocolHandler {

	private final Socket socket;
	private final List<Player> players;
	private ServerProtocol protocolServer;

	private Player currentPlayer;
	private Player currentChallenger;

	private boolean loggedIn = false;
	private boolean readyToPlay = false;
	private boolean inChallenge = false;

	/**
	 * Constructs a handler bound to the given socket and shared player list.
	 *
	 * @param socket  client socket for I/O
	 * @param players shared list of all registered players
	 */
	public ServerController(Socket socket, List<Player> players) {
		this.socket = socket;
		this.players = players;
	}

	/**
	 * Calculates the actual time spent by a player in the game. This tracks
	 * "thinking time" rather than total game duration.
	 * 
	 * @param session The game session
	 * @param player  The player whose time to calculate
	 * @return Duration representing time actually spent thinking/playing
	 */
	private Duration calculatePlayerTimeSpent(GameSession session, Player player) {
		// Get the initial time (5 minutes as defined in
		// GameSession.INITIAL_TIME_PER_PLAYER)
		Duration initialTime = Duration.ofMinutes(5); // Same as GameSession.INITIAL_TIME_PER_PLAYER
		Duration remainingTime;

		// Determine if this player is black or white and get their remaining time
		if (player.equals(session.getBlack())) {
			remainingTime = session.getBlackPlayerTimeRemaining();
		} else {
			remainingTime = session.getWhitePlayerTimeRemaining();
		}

		// Time spent = Initial time - Remaining time
		Duration timeSpent = initialTime.minus(remainingTime);

		// Ensure we don't return negative values (shouldn't happen, but safety first)
		return timeSpent.isNegative() ? Duration.ZERO : timeSpent;
	}

	/**
	 * Cancels a pending challenge between two players. Centralizes challenge
	 * cleanup logic with proper null checking.
	 * 
	 * @param challenger The player who initiated the challenge
	 * @param challenged The player who was challenged
	 * @param reply      The reply to send to the challenger ("Rejected",
	 *                   "Canceled", etc.)
	 */
	private void cancelChallenge(Player challenger, Player challenged, String reply) {
		if (challenger == null || challenged == null)
			return;

		ServerController challengerHandler = SessionManager.getHandler(challenger.getUsername());
		ServerController challengedHandler = SessionManager.getHandler(challenged.getUsername());

		if (challengerHandler != null) {
			challengerHandler.inChallenge = false;
			challengerHandler.currentChallenger = null;
			try {
				challengerHandler.protocolServer.sendChallengeReply(reply);
			} catch (Exception ignored) {
			}
		}

		if (challengedHandler != null) {
			challengedHandler.inChallenge = false;
			challengedHandler.currentChallenger = null;
		}
	}

	/**
	 * Verifica se algum jogador excedeu o tempo limite. Deve ser chamado
	 * periodicamente (ex: a cada segundo).
	 */
	public void checkForTimeouts() {
		if (!loggedIn || currentPlayer == null) {
			return;
		}

		Set<GameSession> playerSessions = SessionManager.getSessionsForPlayer(currentPlayer);

		for (GameSession session : playerSessions) {
			if (session.isFinished()) {
				continue;
			}

			Player nextPlayer = session.getNextPlayer();
			if (nextPlayer.equals(currentPlayer) && session.isCurrentPlayerTimeExpired()) {
				Player winner = session.timeoutCurrentPlayer();
				processGameEnd(session, winner, currentPlayer, "timeout");
			}
		}
	}

	/** @return the Player tied to this connection, or null if not logged in. */
	public Player getCurrentPlayer() {
		return currentPlayer;
	}

	/**
	 * Handles the invited player's acceptance or rejection.
	 *
	 * @param challengeStatus "Accepted" or "Rejected"
	 */
	@Override
	public void handleChallengeReply(String challengeStatus) {
		if (!loggedIn || !inChallenge || currentChallenger == null) {
			inChallenge = false;
			currentChallenger = null;
			protocolServer.sendErrorNotification("Rejected", "No pending invitation");
			return;
		}

		ServerController challengerHandler = SessionManager.getHandler(currentChallenger.getUsername());

		if (challengerHandler == null) {
			// Challenger disconnected
			inChallenge = false;
			currentChallenger = null;
			protocolServer.sendErrorNotification("Rejected", "Challenger no longer available");
			return;
		}

		if (!challengerHandler.inChallenge) {
			// The challenger cancelled or no longer in challenge
			inChallenge = false;
			currentChallenger = null;
			protocolServer.sendErrorNotification("Rejected", "Challenge has been canceled");
			return;
		}

		if ("Canceled".equals(challengeStatus)) {
			inChallenge = challengerHandler.inChallenge = false;
			this.currentChallenger = challengerHandler.currentChallenger = null;
			challengerHandler.protocolServer.sendChallengeReply("Canceled");
			return;
		}

		if ("Accepted".equals(challengeStatus)) {
			// drop lobby state
			inChallenge = challengerHandler.inChallenge = false;
			readyToPlay = challengerHandler.readyToPlay = false;
			GameServer.broadcastAvailablePlayers();

			// randomize Black/White
			boolean challengerIsBlack = ThreadLocalRandom.current().nextBoolean();
			Player blackPlayer = challengerIsBlack ? currentChallenger : currentPlayer;
			Player whitePlayer = challengerIsBlack ? currentPlayer : currentChallenger;

			String newGameId = SessionManager.createGameSession(blackPlayer, whitePlayer);

			// notify both
			challengerHandler.protocolServer.sendGameStarted(newGameId, blackPlayer, whitePlayer);
			protocolServer.sendGameStarted(newGameId, blackPlayer, whitePlayer);

			// Clear challenger references after game starts
			this.currentChallenger = challengerHandler.currentChallenger = null;

		} else {
			// declined: inform challenger
			inChallenge = challengerHandler.inChallenge = false;
			this.currentChallenger = challengerHandler.currentChallenger = null;
			challengerHandler.protocolServer.sendChallengeReply("Rejected");
		}
	}

	/**
	 * Initiates a challenge from this client to another ready player.
	 *
	 * @param opponentUsername the player being challenged
	 */
	@Override
	public void handleChallengeRequest(String opponentUsername) {
		if (!loggedIn || inChallenge || currentPlayer.getUsername().equals(opponentUsername)) {
			protocolServer.sendChallengeReply("Rejected");
			return;
		}
		ServerController opponentHandler = SessionManager.getHandler(opponentUsername);
		if (opponentHandler == null) {
			protocolServer.sendChallengeReply("UsernameUnknown");
			return;
		}
		if (!opponentHandler.isLoggedIn() || !opponentHandler.isReadyToPlay() || opponentHandler.isInChallenge()) {
			protocolServer.sendChallengeReply("Rejected");
			return;
		}

		this.currentChallenger = opponentHandler.currentPlayer;

		opponentHandler.currentChallenger = this.currentPlayer;

		opponentHandler.protocolServer.sendChallengeInvitation(currentPlayer);

		inChallenge = opponentHandler.inChallenge = true;

		System.out
				.println("[Server] Challenge established: " + currentPlayer.getUsername() + " -> " + opponentUsername);

	}

	/** Forfeits the current match, awarding victory to the opponent. */
	@Override
	public void handleForfeitMatchRequest(String gameId) {
		if (!loggedIn) {
			protocolServer.sendErrorNotification("GameNotFound", "Not logged in");
			return;
		}

		GameSession session = SessionManager.getSessionForPlayer(currentPlayer, gameId);
		if (session == null) {
			protocolServer.sendErrorNotification("GameNotFound", "Session not found for gameId: " + gameId);
			return;
		}

		session.forfeit(currentPlayer);
		Player opponent = session.getOpponentOf(currentPlayer);
		processGameEnd(session, opponent, currentPlayer, "forfeit");
	}

	/** Sends the list of registered players sorted for a leaderboard. */
	@Override
	public void handleLeaderboardRequest() {
		if (!loggedIn) {
			protocolServer.sendErrorNotification("Rejected", "Not logged in");
			return;
		}

		// Ordenação conforme especificado: vitórias desc, derrotas asc, tempo asc,
		// username asc
		List<Player> sortedPlayers = new ArrayList<>(players);
		sortedPlayers.sort(Comparator.comparingInt(Player::getVictories).reversed().thenComparingInt(Player::getDefeats)
				.thenComparing(Player::getTotalTime).thenComparing(Player::getUsername));

		protocolServer.sendLeaderboardReply(sortedPlayers);
	}

	/** Refreshes the lobby list for this client if they're logged in. */
	@Override
	public void handleListPlayersRequest() {
		if (!loggedIn) {
			protocolServer.sendErrorNotification("Rejected", "Not logged in");
			return;
		}
		sendOnlinePlayersList(GameServer.getAvailablePlayers());
	}

	/**
	 * Handles a login request by validating credentials. On success, marks as
	 * loggedIn and registers handler.
	 */
	@Override
	public void handleLoginRequest(String username, String password) {
		synchronized (players) {
			for (Player p : players) {
				if (p.getUsername().equalsIgnoreCase(username)) {
					if (p.getPassword().equals(password)) {
						currentPlayer = p;
						loggedIn = true;
						protocolServer.sendLoginReply("Accepted", p);
						SessionManager.registerHandler(username, this);
						sendOnlinePlayersList(GameServer.getAvailablePlayers());
					} else {
						protocolServer.sendLoginReply("WrongPassword", null);
					}
					return;
				}
			}
		}
		protocolServer.sendLoginReply("UsernameUnknown", null);
	}

	/**
	 * Resets this connection's state (loggedIn, ready/inGame), but does not close
	 * socket.
	 */
	@Override
	public void handleLogoutRequest() {
		// only if we were loggedIn
		if (!loggedIn) {
			protocolServer.sendLogoutReply("Rejected");
			return;
		}

		// Cancel any pending challenge
		if (inChallenge && currentChallenger != null) {
			cancelChallenge(currentChallenger, currentPlayer, "Rejected");
		}

		// Resolve any active games before logout
		resolveActiveGames();

		// clear all session flags
		loggedIn = false;
		readyToPlay = false;
		inChallenge = false;
		currentChallenger = null;

		GameServer.broadcastAvailablePlayers();

		// unregister from session manager
		SessionManager.unregisterHandler(currentPlayer.getUsername());

		// keep the handler alive, but clear its player
		currentPlayer = null;

		protocolServer.sendLogoutReply("Accepted");
	}

	/**
	 * Applies a move from this client in the active session.
	 *
	 * @param row the row of the move
	 * @param col the column of the move
	 */
	@Override
	public void handleMoveRequest(String gameId, int row, int col) {
		if (!loggedIn) {
			protocolServer.sendErrorNotification("GameNotFound", "Not logged in");
			return;
		}

		GameSession session = SessionManager.getSessionForPlayer(currentPlayer, gameId);
		if (session == null) {
			protocolServer.sendMoveReply(gameId, "GameNotFound");
			return;
		}

		GameSession.MoveResult result = session.applyMove(row, col, currentPlayer);

		switch (result) {
		case ACCEPTED -> {
			// Broadcast new board to opponent
			Player opponent = session.getOpponentOf(currentPlayer);
			withOpponentHandler(opponent, oppHandler -> oppHandler.protocolServer.sendGameState(session.toGameState()));
			protocolServer.sendMoveReply(gameId, "Accepted");
		}
		case WIN -> {
			Player opponent = session.getOpponentOf(currentPlayer);
			processGameEnd(session, currentPlayer, opponent, "win");
			protocolServer.sendMoveReply(gameId, "Accepted");
		}
		case TIMEOUT -> {
			Player opponent = session.getOpponentOf(currentPlayer);
			processGameEnd(session, opponent, currentPlayer, "timeout");
		}
		case INVALID_MOVE -> protocolServer.sendMoveReply(gameId, "InvalidMove");
		case NOT_YOUR_TURN -> protocolServer.sendMoveReply(gameId, "NotYourTurn");
		}
	}

	/**
	 * Handles readiness toggles for lobby broadcast eligibility.
	 *
	 * @param isReady true to enter lobby list, false to leave
	 */
	@Override
	public void handleReadyRequest(boolean isReady) {
		if (!loggedIn) {
			protocolServer.sendReadyReply("Rejected");
			return;
		}
		readyToPlay = isReady;
		protocolServer.sendReadyReply("Accepted");
		GameServer.broadcastAvailablePlayers();
	}

	/**
	 * Processes a registration request by ensuring unique username and minimal
	 * password length, then persists the new player.
	 */
	@Override
	public void handleRegisterRequest(String username, String password, String nationality, LocalDate dateOfBirth) {
		synchronized (players) {
			if (players.stream().anyMatch(p -> p.getUsername().equalsIgnoreCase(username))) {
				protocolServer.sendRegisterReply("UsernameDuplicated");
				return;
			}
			if (password.length() < 8) {
				protocolServer.sendRegisterReply("PasswordInvalid");
				return;
			}
			Player newPlayer = new Player();
			newPlayer.setUsername(username);
			newPlayer.setPassword(password);
			newPlayer.setNationality(nationality);
			newPlayer.setDateOfBirth(dateOfBirth);
			players.add(newPlayer);
			PlayerManager.savePlayers(players, PlayerManager.PLAYERS_FILE);

			currentPlayer = newPlayer;
			loggedIn = true;
			protocolServer.sendRegisterReply("Accepted");
			SessionManager.registerHandler(username, this);
		}
	}

	/**
	 * Replaces this player's photo and persists it.
	 */
	@Override
	public void handleUpdatePhotoRequest(String photoBase64) {
		if (!loggedIn) {
			protocolServer.sendUpdatePhotoReply("Rejected");
			return;
		}
		synchronized (players) {
			currentPlayer.setPhotoBase64(photoBase64);
			PlayerManager.savePlayers(players, PlayerManager.PLAYERS_FILE);
		}
		protocolServer.sendUpdatePhotoReply("Accepted");
	}

	/**
	 * Updates this player's profile fields.
	 */
	@Override
	public void handleUpdateProfileRequest(String newPassword, String newNationality, LocalDate newDateOfBirth,
			String newTheme) {
		if (!loggedIn) {
			protocolServer.sendUpdateProfileReply("Rejected");
			return;
		}

		System.out.println("[DEBUG] Profile update - password: " + (newPassword != null ? "***" : "null")
				+ ", nationality: " + newNationality + ", dateOfBirth: " + newDateOfBirth + ", theme: " + newTheme);

		synchronized (players) {
			if (newPassword != null)
				currentPlayer.setPassword(newPassword);
			if (newNationality != null)
				currentPlayer.setNationality(newNationality);
			if (newDateOfBirth != null)
				currentPlayer.setDateOfBirth(newDateOfBirth);
			if (newTheme != null)
				currentPlayer.setTheme(newTheme);
			PlayerManager.savePlayers(players, PlayerManager.PLAYERS_FILE);
		}
		protocolServer.sendUpdateProfileReply("Accepted");
	}

	/** @return true if the client is currently in a challenge request. */
	public boolean isInChallenge() {
		return inChallenge;
	}

	/** @return true if this client has successfully logged in. */
	public boolean isLoggedIn() {
		return loggedIn;
	}

	/** @return true if the client has declared ready for play. */
	public boolean isReadyToPlay() {
		return readyToPlay;
	}

	/**
	 * Handles the end of a game session, updating player stats and sending
	 * notifications. Now tracks actual time spent by each player instead of total
	 * game duration.
	 * 
	 * @param session       The game session that ended
	 * @param winner        The winning player
	 * @param loser         The losing player
	 * @param gameEndReason Reason for game end (e.g., "win", "forfeit", "timeout",
	 *                      "disconnect")
	 */
	private void processGameEnd(GameSession session, Player winner, Player loser, String gameEndReason) {
		String gameId = session.getGameId();
		Duration totalGameTime = session.getElapsedTime();

		// Calculate actual time spent by each player (this is the key improvement!)
		Duration winnerTimeSpent = calculatePlayerTimeSpent(session, winner);
		Duration loserTimeSpent = calculatePlayerTimeSpent(session, loser);

		// Update winner stats
		winner.updateVictories();
		winner.updateTotalTime(winnerTimeSpent);

		// Update loser stats
		loser.updateDefeats();
		loser.updateTotalTime(loserTimeSpent);

		// Send notifications to both players
		sendGameEndNotifications(gameId, winner, loser, totalGameTime, gameEndReason);

		// Save and cleanup
		PlayerManager.savePlayers(players, PlayerManager.PLAYERS_FILE);
		SessionManager.unregisterSession(gameId);
	}

	// ======================= REFACTORED HELPER METHODS =======================

	/**
	 * Resolves all active games for this player by forfeit. Called on disconnect or
	 * logout.
	 */
	private void resolveActiveGames() {
		if (currentPlayer == null) {
			return;
		}

		Set<GameSession> activeSessions = SessionManager.getSessionsForPlayer(currentPlayer);
		if (activeSessions.isEmpty()) {
			return;
		}

		String playerName = currentPlayer.getUsername();
		System.out.println("[Server] Resolving " + activeSessions.size() + " active games for player: " + playerName);

		for (GameSession session : activeSessions) {
			if (session.isFinished()) {
				continue; // Skip already finished games
			}

			String gameId = session.getGameId();
			Player opponent = session.getOpponentOf(currentPlayer);

			if (opponent == null) {
				continue;
			}

			// Forfeit this game
			session.forfeit(currentPlayer);

			// Notify opponent about disconnection first
			withOpponentHandler(opponent, oppHandler -> oppHandler.protocolServer.sendOpponentDisconnected(gameId,
					"O jogador " + playerName + " desconectou-se"));

			// Process the game end
			processGameEnd(session, opponent, currentPlayer, "disconnect");

			System.out.println("[Server] Game " + gameId + " ended due to forfeit. Winner: " + opponent.getUsername());
		}

		// Clean up all sessions for this player
		SessionManager.unregisterAllSessionsForPlayer(currentPlayer);
	}

	@Override
	public void run() {
		try (InputStream inputStream = socket.getInputStream(); OutputStream outputStream = socket.getOutputStream()) {

			protocolServer = new ServerProtocol(this, inputStream, outputStream);

			while (!socket.isClosed()) {
				protocolServer.receiveRequests();
			}

		} catch (Exception e) {
			String playerName = (currentPlayer != null) ? currentPlayer.getUsername() : "unknown";

			if (e.getMessage().contains("Connection closed") || e.getMessage().contains("Client disconnected")) {
				System.out.println("[Server] Player '" + playerName + "' disconnected");
			} else {
				System.err.println("[Server] Error for player '" + playerName + "': " + e.getMessage());
				if (ServerConfig.DEBUG_MODE) {
					e.printStackTrace();
				}
			}
		} finally {

			// Cancel any pending challenge before cleanup
			if (currentPlayer != null && inChallenge && currentChallenger != null) {
				cancelChallenge(currentChallenger, currentPlayer, "Rejected");
			}
			resolveActiveGames();

			loggedIn = readyToPlay = inChallenge = false;
			currentChallenger = null;

			GameServer.broadcastAvailablePlayers();

			synchronized (GameServer.clientHandlers) {
				GameServer.clientHandlers.remove(this);
			}
			if (currentPlayer != null) {
				SessionManager.unregisterHandler(currentPlayer.getUsername());
			}
			try {
				socket.close();
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Sends game end notifications to both players. Handles different notification
	 * types based on how the game ended.
	 */
	private void sendGameEndNotifications(String gameId, Player winner, Player loser, Duration totalTime,
			String gameEndReason) {
		String winnerName = winner.getUsername();

		// Get handlers for both players
		ServerController winnerHandler = SessionManager.getHandler(winnerName);
		ServerController loserHandler = SessionManager.getHandler(loser.getUsername());

		// Notify winner
		if (winnerHandler != null) {
			winnerHandler.protocolServer.sendProfileUpdateNotification(winner);
			winnerHandler.protocolServer.sendEndGameNotification(gameId, winnerName, totalTime);
		}

		// Notify loser with specific messages based on how the game ended
		if (loserHandler != null) {
			loserHandler.protocolServer.sendProfileUpdateNotification(loser);

			switch (gameEndReason) {
			case "timeout" -> {
				loserHandler.protocolServer.sendErrorNotification("Timeout", "Time limit exceeded");
				loserHandler.protocolServer.sendEndGameNotification(gameId, winnerName, totalTime);
			}
			case "disconnect" -> {
				// Opponent disconnected message was already sent in resolveActiveGames
				loserHandler.protocolServer.sendEndGameNotification(gameId, winnerName, totalTime);
			}
			default -> {
				loserHandler.protocolServer.sendEndGameNotification(gameId, winnerName, totalTime);
			}
			}
		}
	}

	/**
	 * Sends a ListPlayersReply containing the given subset of players.
	 *
	 * @param onlinePlayers players to include in the reply
	 */
	public void sendOnlinePlayersList(List<Player> onlinePlayers) {
		protocolServer.sendListPlayersReply(onlinePlayers);
	}

	/**
	 * Safely executes an operation on an opponent's handler if they're available.
	 * Provides centralized null checking and exception handling for opponent
	 * operations.
	 * 
	 * @param opponent  The opponent player
	 * @param operation The operation to perform on their handler
	 */
	private void withOpponentHandler(Player opponent, java.util.function.Consumer<ServerController> operation) {
		if (opponent == null)
			return;

		ServerController oppHandler = SessionManager.getHandler(opponent.getUsername());
		if (oppHandler != null) {
			try {
				operation.accept(oppHandler);
			} catch (Exception e) {
				System.err.println("[Server] Error executing operation on opponent handler for "
						+ opponent.getUsername() + ": " + e.getMessage());
			}
		}
	}
}
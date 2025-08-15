package core.server;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import core.Player;

/**
 * SessionManager tracks all active Gobang GameSession instances and
 * per‐connection handlers on the server.
 */
public final class SessionManager {

	/** Map from game ID to its GameSession. */
	private static final ConcurrentMap<String, GameSession> sessionsById = new ConcurrentHashMap<>();

	/** Map from player username to the SET of GameSessions they are in. */
	private static final ConcurrentMap<String, Set<GameSession>> sessionsByPlayer = new ConcurrentHashMap<>();

	/** Map from player username to their connection handler. */
	private static final ConcurrentMap<String, ServerController> handlersByUsername = new ConcurrentHashMap<>();

	/**
	 * Creates a new game session between two players. Generates unique ID, creates
	 * session, and registers it.
	 * 
	 * @param blackPlayer the player assigned Black pieces
	 * @param whitePlayer the player assigned White pieces
	 * @return the generated gameId for the new session
	 */
	public static String createGameSession(Player blackPlayer, Player whitePlayer) {
		Objects.requireNonNull(blackPlayer, "blackPlayer must not be null");
		Objects.requireNonNull(whitePlayer, "whitePlayer must not be null");
		Objects.requireNonNull(blackPlayer.getUsername(), "black player username must not be null");
		Objects.requireNonNull(whitePlayer.getUsername(), "white player username must not be null");

		// Generate unique game ID
		String gameId = UUID.randomUUID().toString();

		// Create session
		GameSession session = new GameSession(gameId, blackPlayer, whitePlayer);

		// Register it
		registerSession(session);

		System.out.println("[SessionManager] Created game session: " + gameId + " (" + blackPlayer.getUsername()
				+ " vs " + whitePlayer.getUsername() + ")");

		return gameId;
	}

	/**
	 * Look up the connection handler for a given player.
	 *
	 * @param username the player's username
	 * @return their ServerController, or null if not found
	 */
	public static ServerController getHandler(String username) {
		if (username == null)
			return null;
		return handlersByUsername.get(username);
	}

	/**
	 * Look up a GameSession by its unique ID.
	 *
	 * @param gameId the game identifier
	 * @return the GameSession, or null if no such session is registered
	 */
	public static GameSession getSession(String gameId) {
		if (gameId == null)
			return null;
		return sessionsById.get(gameId);
	}

	/**
	 * Look up a specific GameSession for a given player and gameId.
	 *
	 * @param player the Player whose session to find
	 * @param gameId the specific game ID
	 * @return the GameSession, or null if the player is not in that game
	 */
	public static GameSession getSessionForPlayer(Player player, String gameId) {
		if (player == null || player.getUsername() == null || gameId == null) {
			return null;
		}

		Set<GameSession> playerSessions = sessionsByPlayer.get(player.getUsername());
		if (playerSessions == null) {
			return null;
		}

		return playerSessions.stream().filter(session -> gameId.equals(session.getGameId())).findFirst().orElse(null);
	}

	/**
	 * Get all GameSessions for a given player.
	 *
	 * @param player the Player whose sessions to find
	 * @return Set of GameSessions, or empty set if player has no active games
	 */
	public static Set<GameSession> getSessionsForPlayer(Player player) {
		if (player == null || player.getUsername() == null) {
			return Collections.emptySet();
		}

		Set<GameSession> playerSessions = sessionsByPlayer.get(player.getUsername());
		return playerSessions != null ? Collections.unmodifiableSet(playerSessions) : Collections.emptySet();
	}

	/**
	 * Register the handler for a given username once they log in.
	 *
	 * @param username the player's username
	 * @param handler  the ServerController instance bound to that user
	 */
	public static void registerHandler(String username, ServerController handler) {
		if (username != null && handler != null) {
			handlersByUsername.put(username, handler);
		}
	}

	/**
	 * Register an existing game session under its ID and both participants.
	 *
	 * @param session the GameSession; must have non‐null id, black and white
	 *                players
	 */
	private static void registerSession(GameSession session) {
		String gameId = session.getGameId();
		String blackUsername = session.getBlack().getUsername();
		String whiteUsername = session.getWhite().getUsername();

		// Add to sessions by ID
		sessionsById.put(gameId, session);

		// Add to each player's session set
		sessionsByPlayer.computeIfAbsent(blackUsername, k -> ConcurrentHashMap.newKeySet()).add(session);
		sessionsByPlayer.computeIfAbsent(whiteUsername, k -> ConcurrentHashMap.newKeySet()).add(session);

		System.out.println("[SessionManager] Registered session " + gameId + " for players: " + blackUsername + ", "
				+ whiteUsername);
	}

	/**
	 * Unregister all sessions for a given player (e.g., on disconnect).
	 *
	 * @param player the Player whose sessions to remove
	 */
	public static void unregisterAllSessionsForPlayer(Player player) {
		if (player == null || player.getUsername() == null) {
			return;
		}

		Set<GameSession> playerSessions = sessionsByPlayer.remove(player.getUsername());
		if (playerSessions == null) {
			return;
		}

		// Remove each session and clean up opponent references
		for (GameSession session : playerSessions) {
			String gameId = session.getGameId();
			sessionsById.remove(gameId);

			// Remove from opponent's session set
			Player opponent = session.getOpponentOf(player);
			if (opponent != null) {
				Set<GameSession> opponentSessions = sessionsByPlayer.get(opponent.getUsername());
				if (opponentSessions != null) {
					opponentSessions.remove(session);
					if (opponentSessions.isEmpty()) {
						sessionsByPlayer.remove(opponent.getUsername());
					}
				}
			}
		}

		System.out.println("[SessionManager] Unregistered all sessions for player: " + player.getUsername());
	}

	/**
	 * Unregister a handler (e.g. on disconnect or logout).
	 *
	 * @param username the player's username
	 */
	public static void unregisterHandler(String username) {
		if (username != null) {
			handlersByUsername.remove(username);
		}
	}

	/**
	 * Unregister (remove) the session by its game ID, and remove from both players.
	 *
	 * @param gameId the game identifier
	 */
	public static void unregisterSession(String gameId) {
		GameSession session = sessionsById.remove(gameId);
		if (session == null) {
			return;
		}

		String blackUsername = session.getBlack().getUsername();
		String whiteUsername = session.getWhite().getUsername();

		// Remove from both players' session sets
		Set<GameSession> blackSessions = sessionsByPlayer.get(blackUsername);
		if (blackSessions != null) {
			blackSessions.remove(session);
			if (blackSessions.isEmpty()) {
				sessionsByPlayer.remove(blackUsername);
			}
		}

		Set<GameSession> whiteSessions = sessionsByPlayer.get(whiteUsername);
		if (whiteSessions != null) {
			whiteSessions.remove(session);
			if (whiteSessions.isEmpty()) {
				sessionsByPlayer.remove(whiteUsername);
			}
		}

		System.out.println("[SessionManager] Unregistered session " + gameId);
	}

	private SessionManager() {
		// no instances
	}
}
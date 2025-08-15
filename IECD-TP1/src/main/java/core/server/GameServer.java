package core.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import core.Player;

/**
 * GameServer listens for incoming client connections and manages the lobby.
 * <p>
 * It periodically broadcasts the list of players who are logged in, ready to
 * play, and not currently in a game to each appropriate client handler.
 */
public class GameServer {

	/**
	 * TCP port on which the game server accepts client connections.
	 */
	public static final int PORT = 5025;

	/**
	 * Shared list of all registered players loaded at startup.
	 */
	public static List<Player> registeredPlayers;

	/**
	 * List of active client handlers, one per connected client.
	 */
	public static final List<ServerController> clientHandlers = new ArrayList<>();

	/**
	 * Broadcasts the list of available players to each handler that is logged in.
	 */
	public static void broadcastAvailablePlayers() {
		List<Player> available = getAvailablePlayers();
		synchronized (clientHandlers) {
			for (ServerController handler : clientHandlers) {
				if (handler.isLoggedIn()) {
					handler.sendOnlinePlayersList(available);
				}
			}
		}
	}

	/**
	 * Verifica timeouts em todos os handlers ativos.
	 */
	private static void checkAllTimeouts() {
		synchronized (clientHandlers) {
			for (ServerController handler : clientHandlers) {
				try {
					handler.checkForTimeouts();
				} catch (Exception e) {
					// Log erro mas continuar verificando outros handlers
					if (ServerConfig.DEBUG_MODE) {
						System.err.println("Error checking timeouts for handler: " + e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * Builds and returns the list of players who are logged in, ready to play. and
	 * not currently in a game.
	 * 
	 * @return list of available Player objects for the lobby
	 */
	public static List<Player> getAvailablePlayers() {
		synchronized (clientHandlers) {
			return clientHandlers.stream().filter(handler -> handler.isLoggedIn() && handler.isReadyToPlay())
					.map(ServerController::getCurrentPlayer).filter(player -> player != null).toList();
		}
	}

	/**
	 * Entry point: loads players, starts the lobby broadcaster, and accepts
	 * connections.
	 * 
	 * @param args command-line arguments (ignored)
	 */
	public static void main(String[] args) {
		registeredPlayers = PlayerManager.loadPlayers(PlayerManager.PLAYERS_FILE);
		startLobbyBroadcaster();
		startTimeoutChecker();

		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("Game Server listening on port " + PORT + "...");
			while (true) {
				Socket clientSocket = serverSocket.accept();
				ServerController handler = new ServerController(clientSocket, registeredPlayers);
				synchronized (clientHandlers) {
					clientHandlers.add(handler);
				}
				handler.start();
			}
		} catch (Exception e) {
			System.err.println("Server error: " + e.getMessage());
		} finally {
			PlayerManager.savePlayers(registeredPlayers, PlayerManager.PLAYERS_FILE);
		}
	}

	/**
	 * Starts a background daemon thread that broadcasts the lobby list every 10
	 * seconds.
	 */
	private static void startLobbyBroadcaster() {
		Thread broadcaster = new Thread(() -> {
			try {
				while (true) {
					Thread.sleep(10_000);
					broadcastAvailablePlayers();
				}
			} catch (InterruptedException ignored) {
			}
		});
		broadcaster.setDaemon(true);
		broadcaster.start();
	}

	/**
	 * Starts a background daemon thread that checks for timeouts every second.
	 */
	private static void startTimeoutChecker() {
		Thread timeoutChecker = new Thread(() -> {
			try {
				while (true) {
					Thread.sleep(1_000); // Verificar a cada segundo
					checkAllTimeouts();
				}
			} catch (InterruptedException ignored) {
			}
		});
		timeoutChecker.setDaemon(true);
		timeoutChecker.setName("TimeoutChecker");
		timeoutChecker.start();
	}
}

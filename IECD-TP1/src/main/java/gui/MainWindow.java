package gui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import communication.client.ClientProtocol;
import core.GameState;
import core.Player;
import gui.game.Game;
import gui.lobby.Lobby;
import gui.lobby.PlayerInvite;
import gui.lobby.Profile;
import gui.login.Login;
import gui.login.Register;

/**
 * Main application window for Five in a Row.
 *
 * Manages a CardLayout of panels for login, registration, lobby, and profile
 * editing. Implements ClientProtocolHandler to receive and handle server
 * replies.
 */
public class MainWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ClientProtocol protocolClient;
	private final CardLayout cardLayout;
	private final JPanel cardHolder;

	private final Login loginPanel;
	private final Register registrationPanel;
	private final Lobby lobbyPanel;
	private final Profile editProfilePanel;
	private final Game gamePanel;
	private PlayerInvite playerinvite;
	private PlayerInvite senderInviteDialog;
	private Map<String, Player> lobbyPlayers = new HashMap<>();
	private Player playerInfo;

	/**
	 * Initializes the main window and all panels. Panels are added to a CardLayout
	 * but not yet displayed.
	 */
	public MainWindow() {
		super("Jogo Gobang");

		cardLayout = new CardLayout();
		cardHolder = new JPanel(cardLayout);
		cardHolder.setBorder(new EmptyBorder(5, 5, 5, 5));

		// Pass the JFrameMainWindow instance to the respective class
		loginPanel = new Login(this);
		registrationPanel = new Register(this);
		lobbyPanel = new Lobby(this);
		editProfilePanel = new Profile(this);
		gamePanel = new Game(this, "", "");

		// Add panels to CardLayout
		cardHolder.add(loginPanel, "LOGIN");
		cardHolder.add(registrationPanel, "REGISTRATION");
		cardHolder.add(lobbyPanel, "LOBBY");
		cardHolder.add(editProfilePanel, "EDIT_PROFILE");
		cardHolder.add(gamePanel, "GAME");

		setMinimumSize(new Dimension(800, 700));
		pack();
		setLocationRelativeTo(null);

		getContentPane().add(cardHolder);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	public void clearChallengeState() {
		disposeIncommingChallengeDialog();
		disposeCancelChallengeDialog();
	}

	/**
	 * Cleans up challenge invitation dialog.
	 */
	public void disposeCancelChallengeDialog() {
		System.out.println(
				"JFrameMainWindow: disposeCancelChallengeDialog called. senderInviteDialog: " + senderInviteDialog);
		if (senderInviteDialog != null) {
			senderInviteDialog.disposeCancelWindow();
			senderInviteDialog = null;
		}
	}

	/**
	 * Cleans up incoming challenge dialog.
	 */
	public void disposeIncommingChallengeDialog() {
		System.out.println("JFrameMainWindow: disposeIncommingChallengeDialog called. playerinvite: " + playerinvite);
		if (playerinvite != null) {
			playerinvite.disposeInviteWindow();
			playerinvite = null;
		}
	}

	/**
	 * Gets the current logged-in player's username.
	 * 
	 * @return Current player's username
	 */
	public String getCurrentUsername() {
		return playerInfo.getUsername();
	}

	/**
	 * Retrieves a player from the lobby cache.
	 * 
	 * @param username The username to look up
	 * @return Player object or null if not found
	 */
	public Player getLobbyPlayer(String username) {
		return lobbyPlayers.get(username);
	}

	/**
	 * Provides panels with access to the ProtocolClient.
	 *
	 * @return the bound ProtocolClient instance
	 */
	public ClientProtocol getProtocolClient() {
		return protocolClient;
	}

	public boolean isInChallengeState() {
		return senderInviteDialog != null || playerinvite != null;
	}

	/**
	 * Handles challenge acceptance by cleaning up invitation dialogs.
	 */
	public void onChallengeAccepted() {
		disposeCancelChallengeDialog();
	}

	/**
	 * Called when a game ends to notify the user of the winner and duration, then
	 * returns to the lobby.
	 *
	 * @param gameId         the unique identifier of the finished game
	 * @param winnerUsername the username of the winning player
	 * @param totalTime      the total duration of the game
	 */
	public void onGameEnded(String gameId, String winnerUsername, Duration totalTime) {
		long secondsTotal = totalTime.getSeconds();
		long hours = secondsTotal / 3600;
		long minutes = (secondsTotal % 3600) / 60;
		long seconds = secondsTotal % 60;
		String message = String.format("O Jogo \"%s\" terminou.%n" + "Vencedor: %s%n" + "Duração: %02d:%02d:%02d",
				gameId, winnerUsername, hours, minutes, seconds);
		JOptionPane.showMessageDialog(this, message, "Game Over", JOptionPane.INFORMATION_MESSAGE);

		lobbyPanel.setReady(false);
		showLobbyPanel();
	}

	/**
	 * Handles new game start by initializing game panel.
	 * 
	 * @param gameId      The new game's ID
	 * @param blackPlayer Player object of black stone player
	 * @param whitePlayer Player object of white stone player
	 */
	public void onGameStarted(String gameId, Player blackPlayer, Player whitePlayer) {
		// Agora temos os perfis completos - não precisamos de lookup!
		String blackB64 = blackPlayer.getPhotoBase64();
		String whiteB64 = whitePlayer.getPhotoBase64();

		// Cache os perfis para uso futuro se necessário
		lobbyPlayers.put(blackPlayer.getUsername(), blackPlayer);
		lobbyPlayers.put(whitePlayer.getUsername(), whitePlayer);

		showGamePanel(gameId, blackPlayer.getUsername(), whitePlayer.getUsername(), blackB64, whiteB64);
	}

	/**
	 * Displays incoming game challenge dialog.
	 * 
	 * @param challengerProfile Profile of the challenging player
	 */
	public void onIncomingChallenge(Player challengerProfile) {
		playerinvite = new PlayerInvite(this);
		playerinvite.showInvite(challengerProfile.getUsername(), challengerProfile.getPhotoBase64());
	}

	/**
	 * Handles successful login by updating UI state.
	 * 
	 * @param profile The logged-in player's profile information
	 */
	public void onLoginSuccess(Player profile) {
		this.playerInfo = profile;
		lobbyPlayers.put(profile.getUsername(), profile);
		lobbyPanel.setCurrentPlayer(profile.getUsername());
		System.out.println(profile.getUsername());
		lobbyPanel.updateOwnStats(profile);
		showLobbyPanel();
	}

	/**
	 * Handles successful logout by resetting UI state. Clears credentials and
	 * returns to login screen.
	 */
	public void onLogoutSuccess() {
		lobbyPanel.setCurrentPlayer(null);
		loginPanel.getUsernameTxtField().setText("");
		loginPanel.getPasswordField().setText("");
		showLoginPanel();
	}

	/**
	 * Handles profile photo updates by refreshing UI elements.
	 */
	public void onPhotoUpdated() {
		editProfilePanel.loadProfilePhoto();
		lobbyPanel.setOwnPhoto(playerInfo.getPhotoBase64()); // update in lobby too
	}

	/**
	 * Called when profile is updated from server (after game end). Updates local
	 * player info and refreshes UI stats.
	 */
	public void onProfileUpdated(Player updatedProfile) {
		this.playerInfo = updatedProfile;
		lobbyPanel.updateOwnStats(updatedProfile);
		System.out.println("Profile updated: " + updatedProfile.getUsername() + " (V:" + updatedProfile.getVictories()
				+ ", D:" + updatedProfile.getDefeats() + ")");
	}

	/**
	 * Injects the ProtocolClient for sending requests to the server.
	 *
	 * @param client ProtocolClient bound to input/output streams
	 */
	public void setProtocolClient(ClientProtocol client) {
		this.protocolClient = client;
	}

	/**
	 * Displays the profile editing panel. Loads current player data into the edit
	 * form.
	 */
	public void showEditProfilePanel() {
		editProfilePanel.setCurrentPlayer(playerInfo);
		cardLayout.show(cardHolder, "EDIT_PROFILE");
	}

	/**
	 * Displays the game panel and initializes a new game session.
	 * 
	 * @param blackPlayer   The username of the black stone player
	 * @param whitePlayer   The username of the white stone player
	 * @param blackPhotoB64 Base64 encoded photo of black player
	 * @param whitePhotoB64 Base64 encoded photo of white player
	 */
	public void showGamePanel(String gameId, String blackPlayer, String whitePlayer, String blackPhotoB64,
			String whitePhotoB64) {
		gamePanel.initializeGame(gameId, blackPlayer, whitePlayer);
		gamePanel.setPlayerPhotos(blackPhotoB64, whitePhotoB64);
		cardLayout.show(cardHolder, "GAME");
	}

	/**
	 * Shows invitation dialog when waiting for opponent response.
	 * 
	 * @param opponent Username of challenged player
	 */
	public void showInviteForSender(String opponent) {
		senderInviteDialog = new PlayerInvite(this);
		senderInviteDialog.showInviteForSender(opponent, null);
	}

	/**
	 * Displays the lobby panel with updated player information. Updates the
	 * player's own profile photo in the lobby.
	 */
	public void showLobbyPanel() {
		lobbyPanel.setOwnPhoto(playerInfo.getPhotoBase64());
		cardLayout.show(cardHolder, "LOBBY");
	}

	/**
	 * Shows the login screen.
	 */
	public void showLoginPanel() {
		cardLayout.show(cardHolder, "LOGIN");
		getRootPane().setDefaultButton(loginPanel.btnLogin);
	}

	/**
	 * Shows the registration screen.
	 */
	public void showRegistrationPanel() {
		cardLayout.show(cardHolder, "REGISTRATION");
		getRootPane().setDefaultButton(registrationPanel.btnRegistar);
	}

	/**
	 * Updates the game board with current state.
	 * 
	 * @param gameState The current game state to display
	 */
	public void updateGameBoard(GameState gameState) {
		gamePanel.updateBoard(gameState);
	}

	/**
	 * Called by ClientController when the server returns the lobby list.
	 */
	public void updateLobbyPlayers(List<Player> players) {
		lobbyPlayers.clear();

		// caching players images to access in game panel
		for (Player p : players)
			lobbyPlayers.put(p.getUsername(), p);

		for (Player p : players) {
			if (p.getUsername().equals(playerInfo.getUsername())) {
				this.playerInfo = p; // update local info
				break;
			}
		}
		// filter out yourself
		List<Player> others = players.stream().filter(p -> !p.getUsername().equals(playerInfo.getUsername())).toList();
		lobbyPanel.updatePlayers(others);
		lobbyPanel.updateOwnStats(playerInfo);

	}
}
package core.client;

import java.time.Duration;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import communication.client.ClientProtocolHandler;
import core.GameState;
import core.Player;
import gui.MainWindow;

/**
 * ClientController bridges server protocol events to the Swing UI.
 */
public class ClientController implements ClientProtocolHandler {
	private final MainWindow ui;

	/**
	 * Constructs a controller that dispatches callbacks onto the UI.
	 *
	 * @param ui the main application window
	 */
	public ClientController(MainWindow ui) {
		this.ui = ui;
	}

	/**
	 * Fired when another player challenges this user.
	 */
	@Override
	public void handleChallengeInvitation(Player challengerProfile) {
		SwingUtilities.invokeLater(() -> ui.onIncomingChallenge(challengerProfile));
	}

	/**
	 * Fired when the server replies to our challenge.
	 */
	@Override
	public void handleChallengeReply(String challengeStatus) {
		String friendlyMessage = ClientUtils.getFriendlyStatusMessage(challengeStatus);
		SwingUtilities.invokeLater(() -> {
			ui.clearChallengeState();
			if (!"Accepted".equals(challengeStatus)) {
				JOptionPane.showMessageDialog(ui, "O Desafio foi " + friendlyMessage, "Resposta do Desafio",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
	}

	/**
	 * Fired when the server notifies game end by win.
	 */
	@Override
	public void handleEndGameNotification(String gameId, String winnerUsername, Duration totalTime) {
		SwingUtilities.invokeLater(() -> ui.onGameEnded(gameId, winnerUsername, totalTime));
	}

	/**
	 * Fired on error notifications (timeouts, disconnects).
	 */
	@Override
	public void handleErrorNotification(String errorCode, String description) {

		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(ui, "Erro (" + errorCode + "): " + description,
				"Erro conexão", JOptionPane.ERROR_MESSAGE));
	}

	/**
	 * Fired when a match is started by the server.
	 */
	@Override
	public void handleGameStarted(String gameId, Player blackPlayer, Player whitePlayer) {
		SwingUtilities.invokeLater(() -> {
			ui.onGameStarted(gameId, blackPlayer, whitePlayer);
			ui.clearChallengeState();
		});
	}

	/**
	 * Fired when the server broadcasts the updated game board.
	 */
	@Override
	public void handleGameState(GameState gameState) {
		SwingUtilities.invokeLater(() -> ui.updateGameBoard(gameState));
	}

	@Override
	public void handleLeaderboardReply(List<Player> players) {
		// Not used in the desktop Client
	}

	/**
	 * Fired when the server sends a fresh ListPlayersReply.
	 */
	@Override
	public void handleListPlayersReply(List<Player> players) {
		SwingUtilities.invokeLater(() -> ui.updateLobbyPlayers(players));
	}

	/**
	 * Fired when the server responds to a LoginRequest. On success, updates the UI
	 * to show the lobby.
	 */
	@Override
	public void handleLoginReply(String loginStatus, Player profile) {
		String friendlyMessage = ClientUtils.getFriendlyStatusMessage(loginStatus);
		SwingUtilities.invokeLater(() -> {
			if ("Accepted".equals(loginStatus)) {
				ui.onLoginSuccess(profile);
			} else {
				JOptionPane.showMessageDialog(ui, "Login falhou: " + friendlyMessage, "Erro de Login",
						JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	/**
	 * Fired when the server responds to a LogoutRequest. On success, updates the UI
	 * to return to log in.
	 */
	@Override
	public void handleLogoutReply(String status) {
		String friendlyMessage = ClientUtils.getFriendlyStatusMessage(status);
		SwingUtilities.invokeLater(() -> {
			if ("Accepted".equals(status)) {
				ui.onLogoutSuccess();
			} else {
				JOptionPane.showMessageDialog(ui, "Logout falhou: " + friendlyMessage, "Erro",
						JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	/**
	 * Fired when our MoveRequest is acknowledged or rejected.
	 */
	@Override
	public void handleMoveReply(String moveId, String moveStatus) {
		String friendlyMessage = ClientUtils.getFriendlyStatusMessage(moveStatus);

		SwingUtilities.invokeLater(() -> {
			if (!"Accepted".equals(moveStatus)) {
				// Timeout é um caso especial - jogada rejeitada por tempo esgotado
				if ("Timeout".equals(moveStatus)) {
					JOptionPane.showMessageDialog(ui, "Tempo esgotado! Perdeste por demorar demasiado tempo.",
							"Tempo Esgotado", JOptionPane.WARNING_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(ui, "Jogada falhou: " + friendlyMessage, "Erro da Jogada",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	/**
	 * Fired when the server notifies that the Opponent has Disconnected
	 */
	@Override
	public void handleOpponentDisconnected(String gameId, String description) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(ui, description, "Oponente Desconectou-se", JOptionPane.ERROR_MESSAGE);
		});

	}

	@Override
	public void handleProfileUpdateNotification(Player updatedProfile) {
		SwingUtilities.invokeLater(() -> {
			// Atualizar o perfil local
			ui.onProfileUpdated(updatedProfile);
		});
	}

	/**
	 * Fired when the server responds to a ReadyRequest.
	 */
	@Override
	public void handleReadyReply(String readyStatus) {
		String friendlyMessage = ClientUtils.getFriendlyStatusMessage(readyStatus);
		SwingUtilities.invokeLater(() -> {
			if (!"Accepted".equals(readyStatus)) {
				JOptionPane.showMessageDialog(ui, "Ready failed: " + friendlyMessage, "Ready Error",
						JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	/**
	 * Fired when the server responds to a RegisterRequest. On success, prompts the
	 * user to log in.
	 */
	@Override
	public void handleRegisterReply(String registerStatus) {
		String friendlyMessage = ClientUtils.getFriendlyStatusMessage(registerStatus);
		SwingUtilities.invokeLater(() -> {
			if ("Accepted".equals(registerStatus)) {
				JOptionPane.showMessageDialog(ui, "Registo Bem-Sucedido! Faça início de sessão.",
						"Registo Bem-Sucedido", JOptionPane.INFORMATION_MESSAGE);
				ui.showLoginPanel();
			} else {
				JOptionPane.showMessageDialog(ui, "Registo falhou: " + friendlyMessage, "Erro de Registo",
						JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	/**
	 * Fired when the server responds to an UpdatePhotoRequest.
	 */
	@Override
	public void handleUpdatePhotoReply(String photoStatus) {
		String friendlyMessage = ClientUtils.getFriendlyStatusMessage(photoStatus);
		SwingUtilities.invokeLater(() -> {
			if ("Accepted".equals(photoStatus)) {
				ui.onPhotoUpdated();
			} else {
				JOptionPane.showMessageDialog(ui, "Erro ao atualizar a foto: " + friendlyMessage,
						"Erro do carregamento da foto", JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	/**
	 * Fired when the server responds to an UpdateProfileRequest.
	 */
	@Override
	public void handleUpdateProfileReply(String updateStatus) {
		String friendlyMessage = ClientUtils.getFriendlyStatusMessage(updateStatus);
		SwingUtilities.invokeLater(() -> {
			if ("Accepted".equals(updateStatus)) {
				JOptionPane.showMessageDialog(ui, "Perfil atualizado com sucesso.", "Atualização com Sucesso",
						JOptionPane.INFORMATION_MESSAGE);
				ui.showLobbyPanel();
			} else {
				JOptionPane.showMessageDialog(ui, "Atualização de perfil falhou: " + friendlyMessage,
						"Erro de atualização", JOptionPane.ERROR_MESSAGE);
			}
		});
	}
}
package gui.lobby;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import core.client.ClientUtils;
import gui.MainWindow;

/**
 * Handles player invitation functionality including showing invitation dialogs
 * and managing invitation state between players.
 */

public class PlayerInvite {

	private MainWindow frame;
	private AtomicBoolean inviteCancelled;
	private JDialog inviteDialog;
	private JDialog cancelDialog;

	/**
	 * Constructs a PlayerInvite instance with reference to the main window.
	 * 
	 * @param frame The main application window reference
	 */
	public PlayerInvite(MainWindow frame) {
		this.frame = frame;
		inviteCancelled = new AtomicBoolean(false);
	}

	/**
	 * Closes the cancellation dialog window if it's currently visible.
	 */
	public void disposeCancelWindow() {
		SwingUtilities.invokeLater(() -> {
			if (cancelDialog != null && cancelDialog.isVisible()) {
				cancelDialog.dispose();
			}
		});
	}

	/**
	 * Closes the invitation dialog window if it's currently visible.
	 */
	public void disposeInviteWindow() {
		SwingUtilities.invokeLater(() -> {
			if (inviteDialog != null && inviteDialog.isVisible()) {
				inviteDialog.dispose();
			}
		});
	}

	/**
	 * Checks if the current invitation was cancelled by the sender.
	 * 
	 * @return true if the invitation was cancelled, false otherwise
	 */
	public boolean isInviteCancelled() {
		return inviteCancelled.get();
	}

	/**
	 * Shows an invitation dialog to the receiving player with opponent's
	 * information.
	 * 
	 * @param opponentUsername    The username of the player sending the invitation
	 * @param opponentPhotoBase64 Base64 encoded photo of the opponent player
	 */

	public void showInvite(String opponentUsername, String opponentPhotoBase64) {
		SwingUtilities.invokeLater(() -> {

			// Load and scale opponent's photo or use default if not available
			ImageIcon opponentIcon = ClientUtils.loadAndScaleIconOrDefault(opponentPhotoBase64, 50);

			// Create modal dialog for the invitation
			inviteDialog = new JDialog(frame, "Convite para Jogar", true); // true for modal
			inviteDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			inviteDialog.setLayout(new BorderLayout(10, 10));

			// Create dialog components with Portuguese text
			JLabel messageLabel = new JLabel("Jogador " + opponentUsername + " quer jogar contigo?", opponentIcon,
					JLabel.LEFT);
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton yesButton = new JButton("Sim");
			JButton noButton = new JButton("Não");

			// Handle acceptance of invitation
			yesButton.addActionListener(e -> {
				System.out.println("Jogador aceitou o convite de " + opponentUsername);
				frame.getProtocolClient().sendChallengeReply("Accepted");
				disposeInviteWindow(); // Close the invitation dialog
			});

			// Handle rejection of invitation
			noButton.addActionListener(e -> {
				System.out.println("Jogador rejeitou o convite de " + opponentUsername);
				frame.getProtocolClient().sendChallengeReply("Rejected");
				disposeInviteWindow(); // Close the invitation dialog
			});

			buttonPanel.add(yesButton);
			buttonPanel.add(noButton);

			inviteDialog.add(messageLabel, BorderLayout.CENTER);
			inviteDialog.add(buttonPanel, BorderLayout.SOUTH);

			// Display the dialog
			inviteDialog.pack();
			inviteDialog.setLocationRelativeTo(frame);
			inviteDialog.setVisible(true);
		});
	}

	/**
	 * Shows a waiting dialog for the player who sent the invitation.
	 * 
	 * @param opponent          The username of the opponent being invited
	 * @param opponentPhotoPath Path to the opponent's photo (unused in current
	 *                          implementation)
	 */
	public void showInviteForSender(String opponent, String opponentPhotoPath) {
		inviteCancelled.set(false); // Reset the cancellation flag

		SwingUtilities.invokeLater(() -> {
			// Create non-modal dialog for the inviting player
			cancelDialog = new JDialog(frame, "A enviar convite...", false); // ⚡ false = not modal
			cancelDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

			JPanel panel = new JPanel(new BorderLayout(10, 10));
			JLabel label = new JLabel("A aguardar resposta de " + opponent + "...");
			JButton cancelButton = new JButton("Cancelar Convite");

			// Handle invitation cancellation
			cancelButton.addActionListener(e -> {
				int confirm = JOptionPane.showConfirmDialog(cancelDialog, "Tem certeza que deseja cancelar o convite?",
						"Confirmar Cancelamento", JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION) {
					inviteCancelled.set(true);
					frame.getProtocolClient().sendChallengeReply("Canceled");
					cancelDialog.dispose();
					frame.clearChallengeState();
				}
			});

			// Add components to dialog
			panel.add(label, BorderLayout.CENTER);
			panel.add(cancelButton, BorderLayout.SOUTH);
			panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

			cancelDialog.getContentPane().add(panel);

			// Display the dialog
			cancelDialog.pack();
			cancelDialog.setLocationRelativeTo(frame);
			cancelDialog.setVisible(true);
		});
	}
}
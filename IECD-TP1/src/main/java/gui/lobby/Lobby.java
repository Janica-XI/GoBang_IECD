package gui.lobby;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import core.Player;
import core.client.ClientUtils;
import gui.MainWindow;

public class Lobby extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int PHOTO_SIZE = 50;
	private MainWindow mainFrame;
	private DefaultListModel<String> listaJogadoresModel;
	private JList<String> listaJogadores;
	private JButton btnAtualizar;
	private JButton btnEditarPerfil;
	private JButton btnJogar;
	private JLabel lblBemVindo;
	private JLabel lblEstatisticasProprioJogador;
	private JLabel lblEstatisticasJogadorSelecionado;
	private JLabel lblFotoPerfil;
	private JRadioButton rdbtnReady;
	private JButton btnLogout;

	/**
	 * Create the panel for the list of online players with detailed information and
	 * layout.
	 * 
	 * @param mainFrame The main JFrame of the application.
	 */
	public Lobby(MainWindow frame) {
		this.mainFrame = frame;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 229, 60, 54, 100, 8 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		// Model for the players list
		listaJogadoresModel = new DefaultListModel<>();
		listaJogadoresModel.addElement("Jogador 1 (V: 10, D: 2, T: 01:30:00)");
		listaJogadoresModel.addElement("Jogador 2 (V: 5, D: 8, T: 00:45:30)");
		listaJogadoresModel.addElement("Jogador 3 (V: 12, D: 1, T: 02:15:45)");
		listaJogadoresModel.addElement("Jogador 4 (V: 3, D: 5, T: 00:20:10)");
		listaJogadoresModel.addElement("Jogador 5 (V: 7, D: 3, T: 01:05:20)");

		// Players list (left aligned)
		listaJogadores = new JList<>(listaJogadoresModel);
		listaJogadores.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(listaJogadores);
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.gridheight = 7;
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		add(scrollPane, gbc_scrollPane);

		// "Update List" button (below the list, left aligned)
		btnAtualizar = new JButton("Atualizar Lista");
		btnAtualizar.addActionListener(e -> {
			mainFrame.getProtocolClient().sendListPlayersRequest();
		});

		// Label "Welcome {username}"
		lblBemVindo = new JLabel("Bem-vindo Jogador"); // Placeholder for the username
		lblBemVindo.setFont(new Font("Segoe UI", Font.BOLD, 22));
		GridBagConstraints gbc_lblBemVindo = new GridBagConstraints();
		gbc_lblBemVindo.gridwidth = 3;
		gbc_lblBemVindo.insets = new Insets(5, 5, 5, 5);
		gbc_lblBemVindo.gridx = 1;
		gbc_lblBemVindo.gridy = 3;
		add(lblBemVindo, gbc_lblBemVindo);

		// Own player statistics (placeholder)
		lblEstatisticasProprioJogador = new JLabel("Vitórias: 0, Derrotas: 0, Tempo Total: 00:00:00");
		GridBagConstraints gbc_lblEstatisticasProprioJogador = new GridBagConstraints();
		gbc_lblEstatisticasProprioJogador.gridwidth = 3;
		gbc_lblEstatisticasProprioJogador.insets = new Insets(0, 5, 5, 5);
		gbc_lblEstatisticasProprioJogador.gridx = 1;
		gbc_lblEstatisticasProprioJogador.gridy = 4;
		add(lblEstatisticasProprioJogador, gbc_lblEstatisticasProprioJogador);

		rdbtnReady = new JRadioButton("Estou pronto");
		GridBagConstraints gbc_rdbtnNewRadioButton = new GridBagConstraints();
		gbc_rdbtnNewRadioButton.gridwidth = 4;
		gbc_rdbtnNewRadioButton.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnNewRadioButton.gridx = 1;
		gbc_rdbtnNewRadioButton.gridy = 5;
		add(rdbtnReady, gbc_rdbtnNewRadioButton);

		rdbtnReady.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean isReady = (e.getStateChange() == ItemEvent.SELECTED);
				// send the boolean-ready to the server
				mainFrame.getProtocolClient().sendReadyRequest(isReady);
			}
		});

		// "Play" button (middle right)
		btnJogar = new JButton("Jogar");
		btnJogar.addActionListener(e -> {
			String opponent = listaJogadores.getSelectedValue();
			if (opponent == null) {
				JOptionPane.showMessageDialog(mainFrame, "Por favor seleccione um jogador primeiro",
						"Nenhum Jogador Seleccionado", JOptionPane.WARNING_MESSAGE);
				return;
			}

			if (mainFrame.isInChallengeState()) {
				JOptionPane.showMessageDialog(mainFrame, "Já tem um desafio pendente", "Desafio Pendente",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			// ask the local user: send invitation?
			int choice = JOptionPane.showConfirmDialog(mainFrame, "Desafiar " + opponent + " para uma partida?",
					"Enviar Convite", JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.YES_OPTION) {
				String opponentUsername = opponent.substring(0, opponent.indexOf("(") - 1);
				mainFrame.getProtocolClient().sendChallengeRequest(opponentUsername);
				mainFrame.showInviteForSender(opponent);
			}
		});

		GridBagConstraints gbc_btnJogar = new GridBagConstraints();
		gbc_btnJogar.gridwidth = 4;
		gbc_btnJogar.anchor = GridBagConstraints.CENTER;
		gbc_btnJogar.insets = new Insets(5, 5, 5, 0);
		gbc_btnJogar.gridx = 1;
		gbc_btnJogar.gridy = 6;
		add(btnJogar, gbc_btnJogar);

		GridBagConstraints gbc_btnAtualizar = new GridBagConstraints();
		gbc_btnAtualizar.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnAtualizar.insets = new Insets(0, 5, 5, 5);
		gbc_btnAtualizar.gridx = 0;
		gbc_btnAtualizar.gridy = 8;
		add(btnAtualizar, gbc_btnAtualizar);

		// create an empty JLabel for the photo
		lblFotoPerfil = new JLabel();
		GridBagConstraints gbc_lblFotoPerfil = new GridBagConstraints();
		gbc_lblFotoPerfil.insets = new Insets(0, 0, 5, 5);
		gbc_lblFotoPerfil.gridx = 3;
		gbc_lblFotoPerfil.gridy = 1;
		add(lblFotoPerfil, gbc_lblFotoPerfil);
		// initialize with default
		setOwnPhoto(null);

		// "Edit Profile" button (top right)
		btnEditarPerfil = new JButton("Editar Perfil");
		btnEditarPerfil.addActionListener(e -> {
			mainFrame.showEditProfilePanel();
		});

		GridBagConstraints gbc_btnEditarPerfil = new GridBagConstraints();
		gbc_btnEditarPerfil.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnEditarPerfil.insets = new Insets(5, 5, 5, 5);
		gbc_btnEditarPerfil.gridx = 3;
		gbc_btnEditarPerfil.gridy = 2;
		add(btnEditarPerfil, gbc_btnEditarPerfil);

		// Selected player statistics (placeholder - initially empty)
		lblEstatisticasJogadorSelecionado = new JLabel("Estatísticas: ");
		GridBagConstraints gbc_lblEstatisticasJogadorSelecionado = new GridBagConstraints();
		gbc_lblEstatisticasJogadorSelecionado.gridwidth = 3;
		gbc_lblEstatisticasJogadorSelecionado.insets = new Insets(0, 5, 5, 5);
		gbc_lblEstatisticasJogadorSelecionado.gridx = 1;
		gbc_lblEstatisticasJogadorSelecionado.gridy = 8;
		add(lblEstatisticasJogadorSelecionado, gbc_lblEstatisticasJogadorSelecionado);

		// Add a ListSelectionListener to update the selected player's statistics
		listaJogadores.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				String jogadorSelecionado = listaJogadores.getSelectedValue();
				if (jogadorSelecionado != null) {
					// Extract statistics from the player name (placeholder only)
					String stats = jogadorSelecionado.substring(jogadorSelecionado.indexOf('('));
					lblEstatisticasJogadorSelecionado.setText("Estatísticas: " + stats);
				} else {
					lblEstatisticasJogadorSelecionado.setText("Estatísticas: ");

				}
			}
		});
		btnLogout = new JButton("Logout");
		GridBagConstraints gbc_btnLogout = new GridBagConstraints();
		gbc_btnLogout.insets = new Insets(0, 0, 0, 5);
		gbc_btnLogout.gridx = 3;
		gbc_btnLogout.gridy = 9;
		add(btnLogout, gbc_btnLogout);

		btnLogout.addActionListener(e -> {
			mainFrame.getProtocolClient().sendLogoutRequest();
		});
	}

	/**
	 * Helper method to format player statistics into a string.
	 */
	private String formatPlayerStats(Player player) {
		long hours = player.getTotalTime().toHours();
		long minutes = player.getTotalTime().toMinutesPart();
		long seconds = player.getTotalTime().toSecondsPart();

		return String.format("Vitórias: %d, Derrotas: %d, Tempo Total: %02d:%02d:%02d", player.getVictories(),
				player.getDefeats(), hours, minutes, seconds);
	}

	/**
	 * Change the "Welcome …" label to greet the current user.
	 */
	public void setCurrentPlayer(String username) {
		lblBemVindo.setText("Bem-vindo " + username);
	}

	/**
	 * Decode the user's base64 photo (or fall back), scale to 50px, and set into
	 * our lblFotoPerfil.
	 */
	public void setOwnPhoto(String base64Photo) {
		lblFotoPerfil.setIcon(ClientUtils.loadAndScaleIconOrDefault(base64Photo, PHOTO_SIZE));
	}

	/**
	 * Change the Ready Status of current user.
	 */
	public void setReady(boolean ready) {
		rdbtnReady.setSelected(ready);
		;
	}

	/**
	 * Updates the label with the provided player's statistics.
	 */
	public void updateOwnStats(Player player) {
		if (player != null) {
			String stats = formatPlayerStats(player);
			lblEstatisticasProprioJogador.setText(stats);
		} else {
			lblEstatisticasProprioJogador.setText("Vitórias: 0, Derrotas: 0, Tempo Total: 00:00:00"); // Default if no
																										// player
		}
	}

	/**
	 * Refresh the JList with the server-provided players.
	 */
	public void updatePlayers(List<Player> players) {
		listaJogadoresModel.clear();
		for (Player p : players) {
			String entry = String.format("%s (W:%d, L:%d)", p.getUsername(), p.getVictories(), p.getDefeats());
			listaJogadoresModel.addElement(entry);
		}
	}

}
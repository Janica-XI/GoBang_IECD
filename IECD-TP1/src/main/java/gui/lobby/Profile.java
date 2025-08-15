package gui.lobby;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.toedter.calendar.JDateChooser;

import core.Player;
import core.client.ClientUtils;
import gui.MainWindow;
import gui.login.ListCountry;

public class Profile extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int PHOTO_SIZE = 100;
	private MainWindow frame;
	private JTextField usernameTextField;
	private JPasswordField passwordField;
	private JPasswordField confirmPasswordField;
	private JDateChooser dobChooser;
	private JButton btnGuardar;
	private JButton btnCancelar;
	private JLabel lblusername;
	private JLabel lblPassword;
	private JLabel lblConfirmarPassword;
	private JLabel lblnationality;
	private JLabel lbldob;
	private JLabel lblTitulo;
	private JLabel lblFotoPerfil; // Label to display the profile photo
	private JButton btnAlterarFoto; // Button to change the profile photo
	private Player currentPlayer; // To hold the current player's information
	private JComboBox<String> countryComboBox; // Combo box for country selection
	private ListCountry countryListObj;
	private Map<String, String> countryCodeMap;
	private Map<String, String> countryNameMap;

	/**
	 * Create the panel for editing the player's profile.
	 * 
	 * @param frame The main JFrame of the application.
	 */
	public Profile(MainWindow frame) {
		this.frame = frame;

		this.countryListObj = new ListCountry();
		this.countryCodeMap = countryListObj.getCountryCodeMap();
		this.countryNameMap = countryListObj.getCountryNameMap();

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 200, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; // Increased row heights to accommodate
																				// new components
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
		setLayout(gridBagLayout);

		// Panel title
		lblTitulo = new JLabel("Editar Perfil");
		GridBagConstraints gbc_lblTitulo = new GridBagConstraints();
		gbc_lblTitulo.gridwidth = 3;
		gbc_lblTitulo.insets = new Insets(10, 10, 15, 0);
		gbc_lblTitulo.gridx = 0;
		gbc_lblTitulo.gridy = 0;
		add(lblTitulo, gbc_lblTitulo);

		// Profile photo label
		lblFotoPerfil = new JLabel();
		lblFotoPerfil.setHorizontalAlignment(JLabel.CENTER);
		GridBagConstraints gbc_lblFotoPerfil = new GridBagConstraints();
		gbc_lblFotoPerfil.gridwidth = 3;
		gbc_lblFotoPerfil.insets = new Insets(5, 10, 10, 10);
		gbc_lblFotoPerfil.gridx = 0;
		gbc_lblFotoPerfil.gridy = 1;
		add(lblFotoPerfil, gbc_lblFotoPerfil);

		// Button to change profile photo
		btnAlterarFoto = new JButton("Alterar Foto");
		GridBagConstraints gbc_btnAlterarFoto = new GridBagConstraints();
		gbc_btnAlterarFoto.gridwidth = 3;
		gbc_btnAlterarFoto.insets = new Insets(0, 10, 15, 10);
		gbc_btnAlterarFoto.gridx = 0;
		gbc_btnAlterarFoto.gridy = 2;
		add(btnAlterarFoto, gbc_btnAlterarFoto);

		// Username label
		lblusername = new JLabel("Nome Utilizador:");
		GridBagConstraints gbc_lblusername = new GridBagConstraints();
		gbc_lblusername.anchor = GridBagConstraints.EAST;
		gbc_lblusername.insets = new Insets(5, 10, 5, 5);
		gbc_lblusername.gridx = 0;
		gbc_lblusername.gridy = 3;
		add(lblusername, gbc_lblusername);

		// Username text field (disabled for display)
		usernameTextField = new JTextField(currentPlayer != null ? currentPlayer.getUsername() : "");
		usernameTextField.setEnabled(false); // Make it non-editable
		usernameTextField.setToolTipText(currentPlayer == null ? "Informação do utilizador não disponível" : null);
		GridBagConstraints gbc_usernameTextField = new GridBagConstraints();
		gbc_usernameTextField.insets = new Insets(5, 0, 5, 10);
		gbc_usernameTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_usernameTextField.gridx = 1;
		gbc_usernameTextField.gridy = 3;
		add(usernameTextField, gbc_usernameTextField);
		usernameTextField.setColumns(10);

		// New Password label
		lblPassword = new JLabel("Nova Password:");
		GridBagConstraints gbc_lblPassword = new GridBagConstraints();
		gbc_lblPassword.anchor = GridBagConstraints.EAST;
		gbc_lblPassword.insets = new Insets(5, 10, 5, 5);
		gbc_lblPassword.gridx = 0;
		gbc_lblPassword.gridy = 4;
		add(lblPassword, gbc_lblPassword);

		// New Password field
		passwordField = new JPasswordField();
		passwordField.setToolTipText(currentPlayer == null ? "Informação do utilizador não disponível" : null);
		GridBagConstraints gbc_passwordField = new GridBagConstraints();
		gbc_passwordField.insets = new Insets(5, 0, 5, 10);
		gbc_passwordField.fill = GridBagConstraints.HORIZONTAL;
		gbc_passwordField.gridx = 1;
		gbc_passwordField.gridy = 4;
		add(passwordField, gbc_passwordField);

		// Confirm Password label
		lblConfirmarPassword = new JLabel("Confirmar Password:");
		GridBagConstraints gbc_lblConfirmarPassword = new GridBagConstraints();
		gbc_lblConfirmarPassword.anchor = GridBagConstraints.EAST;
		gbc_lblConfirmarPassword.insets = new Insets(5, 10, 5, 5);
		gbc_lblConfirmarPassword.gridx = 0;
		gbc_lblConfirmarPassword.gridy = 5;
		add(lblConfirmarPassword, gbc_lblConfirmarPassword);

		// Confirm Password field
		confirmPasswordField = new JPasswordField();
		confirmPasswordField.setToolTipText(currentPlayer == null ? "Informação do utilizador não disponível" : null);
		GridBagConstraints gbc_confirmPasswordField = new GridBagConstraints();
		gbc_confirmPasswordField.insets = new Insets(5, 0, 5, 10);
		gbc_confirmPasswordField.fill = GridBagConstraints.HORIZONTAL;
		gbc_confirmPasswordField.gridx = 1;
		gbc_confirmPasswordField.gridy = 5;
		add(confirmPasswordField, gbc_confirmPasswordField);

		// Nationality label
		lblnationality = new JLabel("Nacionalidade:");
		GridBagConstraints gbc_lblnationality = new GridBagConstraints();
		gbc_lblnationality.anchor = GridBagConstraints.EAST;
		gbc_lblnationality.insets = new Insets(5, 10, 5, 5);
		gbc_lblnationality.gridx = 0;
		gbc_lblnationality.gridy = 6;
		add(lblnationality, gbc_lblnationality);

		// Country ComboBox
		String[] countryNames = countryNameMap.values().toArray(new String[0]);
		Arrays.sort(countryNames);
		countryComboBox = new JComboBox<>(countryNames);
		GridBagConstraints gbc_countryComboBox = new GridBagConstraints();
		gbc_countryComboBox.insets = new Insets(5, 0, 5, 10);
		gbc_countryComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_countryComboBox.gridx = 1;
		gbc_countryComboBox.gridy = 6;
		add(countryComboBox, gbc_countryComboBox);

		// Date of birth label
		lbldob = new JLabel("Data de Nascimento:");
		GridBagConstraints gbc_lbldob = new GridBagConstraints();
		gbc_lbldob.anchor = GridBagConstraints.EAST;
		gbc_lbldob.insets = new Insets(5, 10, 5, 5);
		gbc_lbldob.gridx = 0;
		gbc_lbldob.gridy = 7;
		add(lbldob, gbc_lbldob);

		// Date of birth chooser
		dobChooser = new JDateChooser();
		// initialize to current player's DOB if present:
		if (currentPlayer != null && currentPlayer.getDateOfBirth() != null) {
			dobChooser.setDate(java.sql.Date.valueOf(currentPlayer.getDateOfBirth()));
		}
		GridBagConstraints gbc_dobChooser = new GridBagConstraints();
		gbc_dobChooser.insets = new Insets(5, 0, 5, 10);
		gbc_dobChooser.fill = GridBagConstraints.HORIZONTAL;
		gbc_dobChooser.gridx = 1;
		gbc_dobChooser.gridy = 7;
		add(dobChooser, gbc_dobChooser);

		// "Guardar Alterações" button
		btnGuardar = new JButton("Guardar Alterações");
		GridBagConstraints gbc_btnGuardar = new GridBagConstraints();
		gbc_btnGuardar.anchor = GridBagConstraints.EAST;
		gbc_btnGuardar.insets = new Insets(20, 0, 10, 5);
		gbc_btnGuardar.gridx = 1;
		gbc_btnGuardar.gridy = 8;
		add(btnGuardar, gbc_btnGuardar);

		// "Cancelar" / "Voltar" button
		btnCancelar = new JButton("Cancelar");
		GridBagConstraints gbc_btnCancelar = new GridBagConstraints();
		gbc_btnCancelar.anchor = GridBagConstraints.EAST;
		gbc_btnCancelar.insets = new Insets(20, 10, 10, 0);
		gbc_btnCancelar.gridx = 0;
		gbc_btnCancelar.gridy = 8;
		add(btnCancelar, gbc_btnCancelar);

		installListeners();
	}

	/**
	 * Opens file chooser dialog to select a new profile photo Converts selected
	 * image to Base64 and updates player profile
	 */
	private void abrirFileChooserFoto() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "gif"));
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File f = fc.getSelectedFile();
			String path = f.getAbsolutePath();
			System.out.println("[DEBUG] chooser selected file: " + path + "  exists? " + f.exists());

			// encode to Base64
			String b64 = ClientUtils.loadImageAsBase64(path);
			System.out.println("[DEBUG] loaded Base64, length=" + b64.length());

			// store the Base64 on the Player
			currentPlayer.setPhotoBase64(b64);

			// refresh the preview/avatar
			loadProfilePhoto();
		}
	}

	/**
	 * Installs all action listeners for the panel's interactive components
	 */
	private void installListeners() {
		btnAlterarFoto.addActionListener(e -> abrirFileChooserFoto());
		btnCancelar.addActionListener(e -> frame.showLobbyPanel());

		btnGuardar.addActionListener(e -> {
			// password validation
			String newPass = new String(passwordField.getPassword()).trim();
			String confirm = new String(confirmPasswordField.getPassword()).trim();
			if (!newPass.isEmpty() && !newPass.equals(confirm)) {
				JOptionPane.showMessageDialog(this, "Passwords não coincidem", "Erro", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// nationality
			String selectedName = (String) countryComboBox.getSelectedItem();
			if (selectedName == null || selectedName.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Por favor selecione uma nacionalidade", "Erro",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			String newNat = countryCodeMap.get(selectedName);
			if (newNat == null) {
				JOptionPane.showMessageDialog(this, "Nacionalidade inválida", "Erro", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// date‐of‐birth → LocalDate
			Date chosen = dobChooser.getDate();
			if (chosen == null) {
				JOptionPane.showMessageDialog(this, "Data de nascimento não pode estar vazia", "Erro",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			// convert via java.sql.Date to preserve only the date portion
			java.sql.Date sqlDate = new java.sql.Date(chosen.getTime());
			LocalDate newDob = sqlDate.toLocalDate();

			// update our local Player object
			if (!newPass.isEmpty()) {
				currentPlayer.setPassword(newPass);
			}
			currentPlayer.setNationality(newNat);
			currentPlayer.setDateOfBirth(newDob);

			// send request to server
			frame.getProtocolClient().sendUpdateProfileRequest(newPass.isEmpty() ? null : newPass, newNat,
					newDob.toString(), // yyyy-MM-dd
					null // theme (desktop client não suporta)
			);

			// send photo update if changed
			String photoB64 = currentPlayer.getPhotoBase64();
			if (photoB64 != null) {
				frame.getProtocolClient().sendUpdatePhotoRequest(photoB64);
				System.out.println(
						"[DEBUG][Client] → Sending UpdatePhotoRequest; photoBase64 length=" + photoB64.length());
			}
		});
	}

	/**
	 * Loads the player's data into the edit form.
	 */
	private void loadPlayerData() {
		usernameTextField.setText(currentPlayer.getUsername());

		String currentCountryName = countryNameMap.get(currentPlayer.getNationality());
		if (currentCountryName != null) {
			countryComboBox.setSelectedItem(currentCountryName);
		}

		LocalDate dob = currentPlayer.getDateOfBirth();
		if (dob != null) {
			dobChooser.setDate(java.sql.Date.valueOf(dob));
		} else {
			dobChooser.setDate(null);
		}

		loadProfilePhoto();
	}

	/**
	 * Loads and displays the player's profile photo. Loads a default if no photo is
	 * available for the player or if loading fails.
	 */
	public void loadProfilePhoto() {
		String base64 = currentPlayer != null ? currentPlayer.getPhotoBase64() : null;
		ImageIcon profileIcon = ClientUtils.loadAndScaleIconOrDefault(base64, PHOTO_SIZE);

		lblFotoPerfil.setToolTipText(currentPlayer == null ? "Informação do utilizador não disponível" : null);
		lblFotoPerfil.setIcon(profileIcon);
	}

	/**
	 * Sets the current player being edited and refreshes the form
	 * 
	 * @param player The player whose profile is being edited
	 */
	public void setCurrentPlayer(Player player) {
		this.currentPlayer = player;
		loadPlayerData();
	}

}
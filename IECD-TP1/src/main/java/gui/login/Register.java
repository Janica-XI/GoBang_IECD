package gui.login;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.toedter.calendar.JDateChooser;

import gui.MainWindow;

public class Register extends JPanel {

	private static final long serialVersionUID = 1L;
	private MainWindow mainFrame;
	private JTextField usernameTxtField;
	private JPasswordField passwordField;
	private JPasswordField confirmPasswordField;
	public JButton btnRegistar;
	private JButton btnVoltarLogin;
	private JLabel lblusername;
	private JLabel lblPassword;
	private JLabel lblConfirmarPassword;
	private JLabel lblNacionalidade;
	private JDateChooser dateChooser;
	private JComboBox<String> countryComboBox;
	private Map<String, String> countryCodeMap;
	private JLabel usernameErrorLabel;
	private JLabel passwordErrorLabel;
	private JLabel confirmPasswordErrorLabel;

	/**
	 * Creates the Registration panel.
	 * 
	 * @param mainFrame The main JFrame of the application.
	 */
	public Register(MainWindow frame) {
		this.mainFrame = frame;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 200, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 1.0 };
		setLayout(gridBagLayout);

		// ISEL Logo
		ImageIcon image = new ImageIcon(getClass().getResource("/assets/logo-isel-preto.png"));
		JLabel lblImage = new JLabel(image);
		GridBagConstraints gbc_lblImage = new GridBagConstraints();
		gbc_lblImage.gridwidth = 4;
		gbc_lblImage.insets = new Insets(0, 0, 5, 0);
		gbc_lblImage.gridx = 0;
		gbc_lblImage.gridy = 1;
		add(lblImage, gbc_lblImage);

		lblusername = new JLabel("Nome de Utilizador:");
		GridBagConstraints gbc_lblusername = new GridBagConstraints();
		gbc_lblusername.anchor = GridBagConstraints.EAST;
		gbc_lblusername.insets = new Insets(0, 0, 5, 5);
		gbc_lblusername.gridx = 0;
		gbc_lblusername.gridy = 3;
		add(lblusername, gbc_lblusername);

		usernameTxtField = new JTextField();
		GridBagConstraints gbc_usernameTxtField = new GridBagConstraints();
		gbc_usernameTxtField.insets = new Insets(0, 0, 5, 5);
		gbc_usernameTxtField.fill = GridBagConstraints.HORIZONTAL;
		gbc_usernameTxtField.gridx = 1;
		gbc_usernameTxtField.gridy = 3;
		add(usernameTxtField, gbc_usernameTxtField);
		usernameTxtField.setColumns(10);

		usernameErrorLabel = new JLabel("");
		usernameErrorLabel.setForeground(Color.RED);
		usernameErrorLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_usernameErrorLabel = new GridBagConstraints();
		gbc_usernameErrorLabel.anchor = GridBagConstraints.WEST;
		gbc_usernameErrorLabel.insets = new Insets(0, 5, 5, 0);
		gbc_usernameErrorLabel.gridx = 1;
		gbc_usernameErrorLabel.gridy = 4;
		add(usernameErrorLabel, gbc_usernameErrorLabel);

		lblPassword = new JLabel("Password:");
		GridBagConstraints gbc_lblPassword = new GridBagConstraints();
		gbc_lblPassword.anchor = GridBagConstraints.EAST;
		gbc_lblPassword.insets = new Insets(0, 0, 5, 5);
		gbc_lblPassword.gridx = 0;
		gbc_lblPassword.gridy = 5;
		add(lblPassword, gbc_lblPassword);

		passwordField = new JPasswordField();
		GridBagConstraints gbc_passwordField = new GridBagConstraints();
		gbc_passwordField.insets = new Insets(0, 0, 5, 5);
		gbc_passwordField.fill = GridBagConstraints.HORIZONTAL;
		gbc_passwordField.gridx = 1;
		gbc_passwordField.gridy = 5;
		add(passwordField, gbc_passwordField);

		passwordErrorLabel = new JLabel("");
		passwordErrorLabel.setForeground(Color.RED);
		passwordErrorLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_passwordErrorLabel = new GridBagConstraints();
		gbc_passwordErrorLabel.anchor = GridBagConstraints.WEST;
		gbc_passwordErrorLabel.insets = new Insets(0, 5, 5, 0);
		gbc_passwordErrorLabel.gridx = 1;
		gbc_passwordErrorLabel.gridy = 6;
		add(passwordErrorLabel, gbc_passwordErrorLabel);

		lblConfirmarPassword = new JLabel("Confirmar Password:");
		GridBagConstraints gbc_lblConfirmarPassword = new GridBagConstraints();
		gbc_lblConfirmarPassword.anchor = GridBagConstraints.EAST;
		gbc_lblConfirmarPassword.insets = new Insets(0, 0, 5, 5);
		gbc_lblConfirmarPassword.gridx = 0;
		gbc_lblConfirmarPassword.gridy = 7;
		add(lblConfirmarPassword, gbc_lblConfirmarPassword);

		confirmPasswordField = new JPasswordField();
		GridBagConstraints gbc_confirmPasswordField = new GridBagConstraints();
		gbc_confirmPasswordField.insets = new Insets(0, 0, 5, 5);
		gbc_confirmPasswordField.fill = GridBagConstraints.HORIZONTAL;
		gbc_confirmPasswordField.gridx = 1;
		gbc_confirmPasswordField.gridy = 7;
		add(confirmPasswordField, gbc_confirmPasswordField);

		confirmPasswordErrorLabel = new JLabel("");
		confirmPasswordErrorLabel.setForeground(Color.RED);
		confirmPasswordErrorLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_confirmPasswordErrorLabel = new GridBagConstraints();
		gbc_confirmPasswordErrorLabel.anchor = GridBagConstraints.WEST;
		gbc_confirmPasswordErrorLabel.insets = new Insets(0, 5, 5, 0);
		gbc_confirmPasswordErrorLabel.gridx = 1;
		gbc_confirmPasswordErrorLabel.gridy = 8;
		add(confirmPasswordErrorLabel, gbc_confirmPasswordErrorLabel);

		lblNacionalidade = new JLabel("Nacionalidade:");
		GridBagConstraints gbc_lblNacionalidade = new GridBagConstraints();
		gbc_lblNacionalidade.anchor = GridBagConstraints.EAST;
		gbc_lblNacionalidade.insets = new Insets(0, 0, 5, 5);
		gbc_lblNacionalidade.gridx = 0;
		gbc_lblNacionalidade.gridy = 9;
		add(lblNacionalidade, gbc_lblNacionalidade);

		// Initialize and fill country list
		ListCountry countryListObj = new ListCountry();
		Map<String, String> countryNameMap = countryListObj.getCountryNameMap();
		countryCodeMap = countryListObj.getCountryCodeMap(); // Obtain country code map
		String[] countryNames = countryNameMap.values().toArray(new String[0]);
		Arrays.sort(countryNames); // Sort alphabetically

		countryComboBox = new JComboBox<>(countryNames);
		GridBagConstraints gbc_countryComboBox = new GridBagConstraints();
		gbc_countryComboBox.insets = new Insets(0, 0, 5, 5);
		gbc_countryComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_countryComboBox.gridx = 1;
		gbc_countryComboBox.gridy = 9;
		add(countryComboBox, gbc_countryComboBox);

		JLabel lblData = new JLabel("Selecionar Data:");
		GridBagConstraints gbc_lblData = new GridBagConstraints();
		gbc_lblData.anchor = GridBagConstraints.EAST;
		gbc_lblData.insets = new Insets(10, 10, 5, 5);
		gbc_lblData.gridx = 0;
		gbc_lblData.gridy = 10;
		add(lblData, gbc_lblData);

		dateChooser = new JDateChooser();
		dateChooser.getCalendarButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		GridBagConstraints gbc_dateChooser = new GridBagConstraints();
		gbc_dateChooser.insets = new Insets(10, 0, 5, 10);
		gbc_dateChooser.fill = GridBagConstraints.HORIZONTAL;
		gbc_dateChooser.gridx = 1;
		gbc_dateChooser.gridy = 10;
		add(dateChooser, gbc_dateChooser);

		btnRegistar = new JButton("Registar");
		GridBagConstraints gbc_btnRegistar = new GridBagConstraints();
		gbc_btnRegistar.insets = new Insets(0, 0, 5, 5);
		gbc_btnRegistar.gridx = 1;
		gbc_btnRegistar.gridy = 11;
		add(btnRegistar, gbc_btnRegistar);

		btnRegistar.addActionListener(e -> {
			String user = usernameTxtField.getText().trim();
			String pass = new String(passwordField.getPassword());
			String confirm = new String(confirmPasswordField.getPassword());
			String nat = countryCodeMap.get((String) countryComboBox.getSelectedItem());
			Date dateOfBirth = dateChooser.getDate();

			boolean isValid = true;

			// Validate username
			if (user.length() < 3 || user.length() > 8) {
				usernameErrorLabel.setText("Entre 3 e 8 caracteres.");
				isValid = false;
			} else {
				usernameErrorLabel.setText("");
			}

			// Validate password
			if (pass.length() < 8 || pass.length() > 16) {
				passwordErrorLabel.setText("Entre 8 e 16 caracteres.");
				isValid = false;
			} else {
				passwordErrorLabel.setText("");
			}

			// Validate confirm password
			if (!pass.equals(confirm)) {
				confirmPasswordErrorLabel.setText("Passwords não coincidem.");
				isValid = false;
			} else {
				confirmPasswordErrorLabel.setText("");
			}

			if (user.isEmpty() || pass.isEmpty() || confirm.isEmpty() || nat == null || dateOfBirth == null) {
				JOptionPane.showMessageDialog(this, "Por favor preencha todos os campos", "Aviso",
						JOptionPane.WARNING_MESSAGE);
				isValid = false;
			}

			if (isValid) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				String formattedDate = sdf.format(dateOfBirth);
				mainFrame.getProtocolClient().sendRegisterRequest(user, pass, nat, formattedDate);
			}
		});

		btnVoltarLogin = new JButton("Voltar ao Login");
		GridBagConstraints gbc_btnVoltarLogin = new GridBagConstraints();
		gbc_btnVoltarLogin.anchor = GridBagConstraints.WEST;
		gbc_btnVoltarLogin.insets = new Insets(0, 0, 5, 5);
		gbc_btnVoltarLogin.gridx = 0;
		gbc_btnVoltarLogin.gridy = 14;
		add(btnVoltarLogin, gbc_btnVoltarLogin);

		// Action listener for the "Voltar ao Login" button
		btnVoltarLogin.addActionListener(new ActionListener() {
			/**
			 * Handles the action event for the "Voltar ao Login" button to navigate to the
			 * login panel.
			 * 
			 * @param e The ActionEvent that occurred.
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				mainFrame.showLoginPanel(); // Call the method in JFrameMainWindow to show the login panel
			}
		});

	}
}
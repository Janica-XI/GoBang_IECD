package gui.login;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import gui.MainWindow;

public class Login extends JPanel {

	private static final long serialVersionUID = 1L;
	private MainWindow mainFrame;
	public JButton btnLogin;
	private JTextField usernameTxtField;
	private JPasswordField passwordField;
	private JLabel userLabel;
	private JLabel passwordLabel;
	private JLabel registerLink;
	private JPanel buttonsPanel;
	private JLabel credits;
	private JLabel usernameErrorLabel; // New label for username errors
	private JLabel passwordErrorLabel; // New label for password errors

	/**
	 * Creates the Login panel.
	 * 
	 * @param frame The main JFrame of the application.
	 */
	public Login(MainWindow frame) {
		this.mainFrame = frame;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 210, 30, 0 }; // Adjusted column widths
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 20, 0, 17, 0, 17, 17, 20, 0, 0 }; // Adjusted row heights to
																							// reserve space
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 1.0 }; // Weight for horizontal resizing
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 }; // Weight
																												// for
																												// vertical
																												// resizing
		setLayout(gridBagLayout);

		// ISEL Logo
		ImageIcon image = new ImageIcon(getClass().getResource("/assets/logo-isel-preto.png"));
		JLabel lblImage = new JLabel(image);
		GridBagConstraints gbc_lblImage = new GridBagConstraints();
		gbc_lblImage.gridwidth = 5;
		gbc_lblImage.insets = new Insets(0, 0, 5, 0);
		gbc_lblImage.gridx = 0;
		gbc_lblImage.gridy = 1;
		add(lblImage, gbc_lblImage);

		// User Label
		userLabel = new JLabel("Utilizador");
		GridBagConstraints gbc_userLabel = new GridBagConstraints();
		gbc_userLabel.anchor = GridBagConstraints.EAST;
		gbc_userLabel.insets = new Insets(0, 0, 5, 5);
		gbc_userLabel.gridx = 0;
		gbc_userLabel.gridy = 4;
		add(userLabel, gbc_userLabel);

		// Username Text Field
		usernameTxtField = new JTextField();
		usernameTxtField.setToolTipText("Nome de Utilizador");
		GridBagConstraints gbc_usernameTxtField = new GridBagConstraints();
		gbc_usernameTxtField.gridwidth = 2;
		gbc_usernameTxtField.insets = new Insets(0, 0, 5, 5);
		gbc_usernameTxtField.fill = GridBagConstraints.HORIZONTAL;
		gbc_usernameTxtField.gridx = 2;
		gbc_usernameTxtField.gridy = 4;
		add(usernameTxtField, gbc_usernameTxtField);
		usernameTxtField.setColumns(10);

		// Username Error Label
		usernameErrorLabel = new JLabel("");
		usernameErrorLabel.setForeground(Color.RED);
		usernameErrorLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_usernameErrorLabel = new GridBagConstraints();
		gbc_usernameErrorLabel.gridwidth = 2;
		gbc_usernameErrorLabel.anchor = GridBagConstraints.WEST;
		gbc_usernameErrorLabel.insets = new Insets(0, 0, 5, 5);
		gbc_usernameErrorLabel.gridx = 2;
		gbc_usernameErrorLabel.gridy = 5; // Directly below username field
		add(usernameErrorLabel, gbc_usernameErrorLabel);

		// Password Label
		passwordLabel = new JLabel("Password");
		GridBagConstraints gbc_passwordLabel = new GridBagConstraints();
		gbc_passwordLabel.anchor = GridBagConstraints.EAST;
		gbc_passwordLabel.insets = new Insets(0, 0, 5, 5);
		gbc_passwordLabel.gridx = 0;
		gbc_passwordLabel.gridy = 6;
		add(passwordLabel, gbc_passwordLabel);

		// Password Field
		passwordField = new JPasswordField();
		passwordField.setToolTipText("Insira a sua Password");
		GridBagConstraints gbc_passwordField = new GridBagConstraints();
		gbc_passwordField.gridwidth = 2;
		gbc_passwordField.insets = new Insets(0, 0, 5, 5);
		gbc_passwordField.fill = GridBagConstraints.HORIZONTAL;
		gbc_passwordField.gridx = 2;
		gbc_passwordField.gridy = 6;
		add(passwordField, gbc_passwordField);

		// Password Error Label
		passwordErrorLabel = new JLabel("");
		passwordErrorLabel.setForeground(Color.RED);
		passwordErrorLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_passwordErrorLabel = new GridBagConstraints();
		gbc_passwordErrorLabel.gridwidth = 2;
		gbc_passwordErrorLabel.anchor = GridBagConstraints.WEST;
		gbc_passwordErrorLabel.insets = new Insets(0, 0, 5, 5);
		gbc_passwordErrorLabel.gridx = 2;
		gbc_passwordErrorLabel.gridy = 7; // Directly below password field
		add(passwordErrorLabel, gbc_passwordErrorLabel);

		// Panel to hold the button and the link
		buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
		btnLogin = new JButton("Entrar");
		registerLink = new JLabel("Ainda não tenho conta");
		registerLink.setForeground(Color.BLUE);
		registerLink.setFont(new Font("Tahoma", Font.PLAIN, 11));
		registerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		buttonsPanel.add(btnLogin);
		buttonsPanel.add(registerLink);

		GridBagConstraints gbc_buttonsPanel = new GridBagConstraints();
		gbc_buttonsPanel.gridwidth = 3;
		gbc_buttonsPanel.insets = new Insets(5, 0, 5, 5); // Added some top padding
		gbc_buttonsPanel.gridx = 1;
		gbc_buttonsPanel.gridy = 9; // Adjusted position
		add(buttonsPanel, gbc_buttonsPanel);

		// Credits Label
		credits = new JLabel("IECD • Semestre Verão 24/25");
		GridBagConstraints gbc_credits = new GridBagConstraints();
		gbc_credits.gridwidth = 2;
		gbc_credits.insets = new Insets(0, 0, 0, 5);
		gbc_credits.gridx = 2;
		gbc_credits.gridy = 10; // Adjusted position
		add(credits, gbc_credits);

		// Action listener for the "Entrar" button with client-side validation and error
		// labels
		btnLogin.addActionListener(e -> {
			String username = usernameTxtField.getText().trim();
			String password = new String(passwordField.getPassword());

			boolean isValid = true;

			// Validate username
			if (username.length() < 3 || username.length() > 8) {
				usernameErrorLabel.setText("Entre 3 e 8 caracteres.");
				isValid = false;
			} else {
				usernameErrorLabel.setText(""); // Clear any previous error
			}

			// Validate password
			if (password.length() < 8 || password.length() > 16) {
				passwordErrorLabel.setText("Entre 8 e 16 caracteres.");
				isValid = false;
			} else {
				passwordErrorLabel.setText(""); // Clear any previous error
			}

			// If validation passes, send the login request
			if (isValid) {
				mainFrame.getProtocolClient().sendLoginRequest(username, password);
			}
		});

		// Add mouse listener to handle clicks on the register link
		registerLink.addMouseListener(new MouseAdapter() {
			/**
			 * Handles the mouse click event on the register link to navigate to the
			 * registration panel.
			 * 
			 * @param e The MouseEvent that occurred.
			 */
			@Override
			public void mouseClicked(MouseEvent e) {
				mainFrame.showRegistrationPanel(); // Call the method in JFrameMainWindow to show the registration panel
			}

			/**
			 * Handles the mouse entered event to change the link colour for hover effect.
			 */
			@Override
			public void mouseEntered(MouseEvent e) {
				registerLink.setForeground(Color.RED);
			}

			/**
			 * Handles the mouse exited event to restore the link colour.
			 */
			@Override
			public void mouseExited(MouseEvent e) {
				registerLink.setForeground(Color.BLUE);
			}
		});

	}

	public JPasswordField getPasswordField() {
		return passwordField;
	}

	public JTextField getUsernameTxtField() {
		return usernameTxtField;
	}

	public void setPasswordField(JPasswordField passwordField) {
		this.passwordField = passwordField;
	}

	public void setUsernameTxtField(JTextField usernameTxtField) {
		this.usernameTxtField = usernameTxtField;
	}
}
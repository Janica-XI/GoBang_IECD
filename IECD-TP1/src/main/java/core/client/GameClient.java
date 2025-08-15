package core.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import communication.client.ClientProtocol;
import gui.MainWindow;

/**
 * Entry point for the GoBang client application.
 * 
 * This class is responsible for:
 * 
 * Establishing the TCP connection to the game server. Starting the Swing UI on
 * the Event Dispatch Thread. Wiring up a background reader thread to process
 * incoming XML messages.
 */
public class GameClient {

	/** Default hostname of the game server. */
	private static final String SERVER_HOST = "localhost";

	/** Default port number of the game server. */
	private static final int SERVER_PORT = 5025;

	/**
	 * Initializes the main window and networking protocol on the EDT.
	 *
	 * @param in  the InputStream from the server socket
	 * @param out the OutputStream to the server socket
	 */
	private static void initGui(InputStream in, OutputStream out) {
		try {
			// Create the main application window (implements ClientProtocolHandler)
			MainWindow ui = new MainWindow();

			// create the controller and pass the UI in
			ClientController controller = new ClientController(ui);

			// Bind a ProtocolClient to the window for sending/receiving XML messages
			ClientProtocol protocol = new ClientProtocol(controller, in, out);
			ui.setProtocolClient(protocol);

			// Start a background thread to read replies/notifications continuously
			startReaderThread(protocol, ui);

			// Show the login screen and make the window visible
			ui.showLoginPanel();
			ui.setVisible(true);

		} catch (Exception ex) {
			// If UI initialization fails, log and terminate
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		Socket socket = null;
		try {
			// Establishing connection to the server
			socket = new Socket(SERVER_HOST, SERVER_PORT);
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();

			try {
				UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException var4) {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
						| UnsupportedLookAndFeelException var3) {
					var3.printStackTrace();
				}
			}

			// Launch the Swing UI on the Event Dispatch Thread
			SwingUtilities.invokeLater(() -> initGui(in, out));

		} catch (IOException e) {
			System.err.println("Unable to connect to " + SERVER_HOST + ":" + SERVER_PORT + " – " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Spawns a daemon thread to handle server-pushed messages.
	 *
	 * @param protocol the ProtocolClient for parsing incoming XML
	 * @param window   the main application window for showing errors
	 */
	private static void startReaderThread(ClientProtocol protocol, MainWindow window) {
		Thread reader = new Thread(() -> {
			try {
				while (true) {
					protocol.receiveReplies();
				}
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(window,
						"Connection lost: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE));
			}
		}, "GoBang-ReaderThread");
		reader.setDaemon(true);
		reader.start();
	}
}

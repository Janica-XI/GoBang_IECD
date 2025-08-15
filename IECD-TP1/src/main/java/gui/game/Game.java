package gui.game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import core.GameState;
import core.client.ClientUtils;
import gui.MainWindow;

/**
 * Main game view for Gobang. • Displays player names + photos • Shows a 15×15
 * board • Tracks and displays elapsed time • Handles user moves and forfeit
 */
public class Game extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int PHOTO_SIZE = 30;

	// Colors for active turn highlighting
	private static final Color ACTIVE_BLACK_COLOR = new Color(0, 255, 255);
	private static final Color ACTIVE_WHITE_COLOR = new Color(255, 0, 255);

	private final MainWindow frame;
	private final GameBoard board;
	private final JLabel timerLabel;
	private final JButton exitButton;

	private final JPanel playerPanelBlack;
	private final JPanel playerPanelWhite;
	private final JLabel playerBlackLabel;
	private final JLabel playerWhiteLabel;
	private final JLabel blackTimerLabel;
	private final JLabel whiteTimerLabel;

	private String currentGameId;
	private Timer timer;
	private long startTime;
	private boolean isBlackTurn;
	private boolean isMyTurn;

	// Chess-style timer: each player has total time remaining that counts down
	// during their turns
	private Duration blackTimeRemaining = Duration.ofMinutes(5);
	private Duration whiteTimeRemaining = Duration.ofMinutes(5);
	private Instant currentTurnStarted = Instant.now(); // When current player's turn started locally

	/**
	 * Constructs the game panel with initial (possibly empty) player names.
	 * 
	 * @param frame     the main application frame
	 * @param blackName black player name
	 * @param whiteName white player name
	 */
	public Game(MainWindow frame, String blackName, String whiteName) {
		this.frame = frame;
		setLayout(new BorderLayout(5, 5));

		// Top: player info + timer
		JPanel topBar = new JPanel(new BorderLayout(5, 5));
		ImageIcon defaultIcon = ClientUtils.loadAndScaleIconOrDefault(null, PHOTO_SIZE);

		// Black player panel com timer
		playerPanelBlack = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		playerBlackLabel = createPlayerLabel(blackName, defaultIcon, SwingConstants.RIGHT);
		blackTimerLabel = new JLabel("05:00");
		blackTimerLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
		blackTimerLabel.setForeground(Color.BLACK);

		JPanel blackContainer = new JPanel(new BorderLayout());
		blackContainer.add(playerBlackLabel, BorderLayout.CENTER);
		blackContainer.add(blackTimerLabel, BorderLayout.SOUTH);
		playerPanelBlack.add(blackContainer);
		topBar.add(playerPanelBlack, BorderLayout.WEST);

		// White player panel com timer
		playerPanelWhite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		playerWhiteLabel = createPlayerLabel(whiteName, defaultIcon, SwingConstants.LEFT);
		whiteTimerLabel = new JLabel("05:00");
		whiteTimerLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
		whiteTimerLabel.setForeground(Color.BLACK);

		JPanel whiteContainer = new JPanel(new BorderLayout());
		whiteContainer.add(playerWhiteLabel, BorderLayout.CENTER);
		whiteContainer.add(whiteTimerLabel, BorderLayout.SOUTH);
		playerPanelWhite.add(whiteContainer);
		topBar.add(playerPanelWhite, BorderLayout.EAST);

		timerLabel = new JLabel("Tempo Total: 00:00", SwingConstants.CENTER);
		timerLabel.setFont(new Font("Tahoma", Font.PLAIN, 12));
		topBar.add(timerLabel, BorderLayout.CENTER);

		add(topBar, BorderLayout.NORTH);

		// Center: game board
		board = new GameBoard();
		board.setClickHandler((r, c, e) -> handleBoardClick(r, c, e));
		add(board, BorderLayout.CENTER);

		// Bottom: exit button
		JPanel bottomBar = new JPanel(new BorderLayout());
		exitButton = new JButton("Sair do Jogo");
		exitButton.addActionListener(e -> confirmForfeit());
		bottomBar.add(exitButton, BorderLayout.EAST);
		add(bottomBar, BorderLayout.SOUTH);

		startTimer();
	}

	// ─── Chess-style Timer Methods ───────────────────────────────────────

	private void confirmForfeit() {
		int choice = JOptionPane.showConfirmDialog(frame, "Quer desistir da partida?", "Desistir da Partida",
				JOptionPane.YES_NO_OPTION);
		if (choice == JOptionPane.YES_OPTION) {
			frame.getProtocolClient().sendForfeitMatchRequest(currentGameId);
			frame.showLobbyPanel();
		}
	}

	private JLabel createPlayerLabel(String text, Icon icon, int textPos) {
		JLabel lbl = new JLabel(text, icon, SwingConstants.CENTER);
		lbl.setFont(new Font("Tahoma", Font.BOLD, 14));
		lbl.setHorizontalTextPosition(textPos);
		return lbl;
	}

	private void handleBoardClick(int row, int col, int estado) {
		if (!isMyTurn || estado != CellState.FREE)
			return;
		isMyTurn = false;

		// FIXED: Calculate and update the time consumed during my turn
		Duration timeConsumedThisTurn = Duration.between(currentTurnStarted, Instant.now());

		// Update my remaining time based on time consumed
		if (isBlackTurn) {
			blackTimeRemaining = blackTimeRemaining.minus(timeConsumedThisTurn);
			if (blackTimeRemaining.isNegative()) {
				blackTimeRemaining = Duration.ZERO;
			}
		} else {
			whiteTimeRemaining = whiteTimeRemaining.minus(timeConsumedThisTurn);
			if (whiteTimeRemaining.isNegative()) {
				whiteTimeRemaining = Duration.ZERO;
			}
		}

		// Switch turns locally and reset timer for opponent's turn
		isBlackTurn = !isBlackTurn;
		currentTurnStarted = Instant.now();

		updateTurnHighlight();
		int piece = !isBlackTurn ? CellState.BLACK : CellState.WHITE; // Use opposite since we just switched
		board.setCelula(row, col, piece);
		frame.getProtocolClient().sendMoveRequest(currentGameId, row, col);

		// Update display immediately to show correct times
		updateTimerDisplays();
	}

	// ─── Public API ───────────────────────────────────────────────────────

	/** Prepare for a new game: set names, reset state */
	public void initializeGame(String gameId, String blackName, String whiteName) {
		playerBlackLabel.setText(blackName);
		playerWhiteLabel.setText(whiteName);

		this.currentGameId = gameId;

		// Black always goes first in chess-style games
		isBlackTurn = true;

		// Determine if it's my turn based on my username
		String me = frame.getCurrentUsername();
		isMyTurn = me.equals(blackName); // I go first if I'm the black player

		board.clearBoard();
		resetTimer();

		// Reset chess-style timer tracking
		currentTurnStarted = Instant.now();
		blackTimeRemaining = Duration.ofMinutes(5);
		whiteTimeRemaining = Duration.ofMinutes(5);

		updateTurnHighlight();
	}

	private void resetTimer() {
		if (timer != null && timer.isRunning())
			timer.stop();
		startTimer();
	}

	/** Update player photos (base64 or null) */
	public void setPlayerPhotos(String blackB64, String whiteB64) {
		playerBlackLabel.setIcon(ClientUtils.loadAndScaleIconOrDefault(blackB64, PHOTO_SIZE));
		playerWhiteLabel.setIcon(ClientUtils.loadAndScaleIconOrDefault(whiteB64, PHOTO_SIZE));
	}

	// ─── Internal helpers ────────────────────────────────────────────────

	private void startTimer() {
		startTime = System.currentTimeMillis();
		timer = new Timer(1_000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Update total game time display
				long elapsed = System.currentTimeMillis() - startTime;
				long mins = (elapsed / 1_000) / 60;
				long secs = (elapsed / 1_000) % 60;
				timerLabel.setText(String.format("Tempo: %02d:%02d", mins, secs));

				// Update chess-style individual timers
				updateTimerDisplays();
			}
		});
		timer.start();
	}

	/** Render the latest GameState from the server */
	public void updateBoard(GameState state) {
		List<String> rows = state.getBoardRows();
		for (int r = 0; r < rows.size(); r++) {
			String row = rows.get(r);
			for (int c = 0; c < row.length(); c++) {
				char ch = row.charAt(c);
				int code = (ch == 'B') ? CellState.BLACK : (ch == 'W') ? CellState.WHITE : CellState.FREE;
				board.setCelula(r, c, code);
			}
		}

		// Update turn state
		boolean nextIsBlack = "Black".equals(state.getNextPlayerColor());
		boolean turnChanged = (isBlackTurn != nextIsBlack);

		isBlackTurn = nextIsBlack;

		// Determine if it's my turn
		String me = frame.getCurrentUsername();
		String blackName = playerBlackLabel.getText();
		String whiteName = playerWhiteLabel.getText();

		isMyTurn = (nextIsBlack && me.equals(blackName)) || (!nextIsBlack && me.equals(whiteName));

		// IMPORTANT: If turn changed, reset our local turn tracking
		// This ensures we start counting from 0 for the new player's turn
		// But only if we haven't already switched locally (to avoid double-switching)
		if (turnChanged) {
			currentTurnStarted = Instant.now();
			System.out.println("[Game.java] Turn changed via server update. isBlackTurn now: " + isBlackTurn);
		}

		// Update timing with server's authoritative data
		updateTiming(state.getBlackPlayerTimeRemaining(), state.getWhitePlayerTimeRemaining(), state.getTimestamp());

		updateTurnHighlight();
	}

	/**
	 * Updates the timer displays. Only the active player's time counts down.
	 */
	private void updateTimerDisplays() {
		// Calculate how much time has passed since we started tracking current turn
		// locally
		Duration timeSinceCurrentTurnStarted = Duration.between(currentTurnStarted, Instant.now());

		Duration displayBlackTime = blackTimeRemaining;
		Duration displayWhiteTime = whiteTimeRemaining;

		// Only subtract elapsed time from the player whose turn it is
		if (isBlackTurn) {
			displayBlackTime = blackTimeRemaining.minus(timeSinceCurrentTurnStarted);
			if (displayBlackTime.isNegative()) {
				displayBlackTime = Duration.ZERO;
			}
			// White time stays the same (frozen during black's turn)
		} else {
			displayWhiteTime = whiteTimeRemaining.minus(timeSinceCurrentTurnStarted);
			if (displayWhiteTime.isNegative()) {
				displayWhiteTime = Duration.ZERO;
			}
			// Black time stays the same (frozen during white's turn)
		}

		// Update the timer labels
		blackTimerLabel.setText(ClientUtils.formatTimeRemaining(displayBlackTime));
		whiteTimerLabel.setText(ClientUtils.formatTimeRemaining(displayWhiteTime));

		// Update highlighting and warning colors
		updateTimerStyling(displayBlackTime, displayWhiteTime);
	}

	/**
	 * Updates timer styling (highlighting active player, warning colors)
	 */
	private void updateTimerStyling(Duration currentBlackTime, Duration currentWhiteTime) {
		// Reset to default styles
		blackTimerLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
		whiteTimerLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
		blackTimerLabel.setForeground(Color.BLACK);
		whiteTimerLabel.setForeground(Color.BLACK);

		// Highlight and style the active player's timer
		if (isBlackTurn) {
			blackTimerLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
			// Red warning if less than 30 seconds
			if (currentBlackTime.getSeconds() < 30) {
				blackTimerLabel.setForeground(Color.RED);
			}
		} else {
			whiteTimerLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
			// Red warning if less than 30 seconds
			if (currentWhiteTime.getSeconds() < 30) {
				whiteTimerLabel.setForeground(Color.RED);
			}
		}
	}

	// ─── Timer management ────────────────────────────────────────────────

	/**
	 * Updates timing information from server. Server is authoritative - we use this
	 * to sync our local display and avoid drift.
	 */
	public void updateTiming(Duration blackTime, Duration whiteTime, Instant timestamp) {
		// Server is authoritative - update our time remaining values
		this.blackTimeRemaining = blackTime;
		this.whiteTimeRemaining = whiteTime;

		// Reset our local turn tracking to sync with server
		this.currentTurnStarted = Instant.now();

		System.out.println("[Game.java] Server sync - blackTime: " + blackTime + ", whiteTime: " + whiteTime
				+ ", timestamp: " + timestamp);

		// Update display immediately
		SwingUtilities.invokeLater(this::updateTimerDisplays);
	}

	private void updateTurnHighlight() {
		Color blackBg = isMyTurn && isBlackTurn ? ACTIVE_BLACK_COLOR : null;
		Color whiteBg = isMyTurn && !isBlackTurn ? ACTIVE_WHITE_COLOR : null;
		playerPanelBlack.setBackground(blackBg);
		playerPanelWhite.setBackground(whiteBg);
		board.setReadOnly(!isMyTurn);
	}
}
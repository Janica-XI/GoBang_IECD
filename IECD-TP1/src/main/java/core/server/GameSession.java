package core.server;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.GameState;
import core.Player;

/**
 * Encapsulates a single Gobang match on the server.
 * <p>
 * Tracks the two participants, enforces turn order on a fixed 15×15 board,
 * applies moves, detects wins or forfeits, and records elapsed time.
 */
public class GameSession {

	/**
	 * Possible outcomes when attempting to apply a move.
	 */
	public enum MoveResult {
		/** Move successfully applied; game continues. */
		ACCEPTED,
		/** Move out of bounds or cell already occupied. */
		INVALID_MOVE,
		/** Move made by the wrong player (not that player's turn). */
		NOT_YOUR_TURN,
		/** Move caused that player to win the match. */
		WIN,
		/** Player exceeded time limit. */
		TIMEOUT
	}

	/** Fixed dimension of the Gobang board. */
	private static final int BOARD_SIZE = 15;

	/** Tempo inicial por jogador (5 minutos). */
	private static final Duration INITIAL_TIME_PER_PLAYER = Duration.ofMinutes(5);

	/** Unique identifier for this game session. */
	private final String gameId;

	/** Player assigned Black stones (always moves first). */
	private final Player blackPlayer;

	/** Player assigned White stones. */
	private final Player whitePlayer;

	/** 2D board array: '.' empty, 'B' black stone, 'W' white stone. */
	private final char[][] board;

	/** The player whose turn is next. */
	private Player nextPlayer;

	/** Timestamp when the game started. */
	private final Instant gameStartTime;

	/** Timestamp when the game ended, null if still in progress. */
	private Instant gameEndTime;

	/** The player who won, null until the game finishes. */
	private Player winnerPlayer;

	/** Tempo restante para o jogador Black. */
	private Duration blackPlayerTimeRemaining;

	/** Tempo restante para o jogador White. */
	private Duration whitePlayerTimeRemaining;

	/** Timestamp de quando a vez atual começou. */
	private Instant currentTurnStartTime;

	/**
	 * Constructs a new GameSession between two players. Black always makes the
	 * first move.
	 *
	 * @param gameId      unique identifier for this session
	 * @param blackPlayer the player assigned the Black stones
	 * @param whitePlayer the player assigned the White stones
	 */
	public GameSession(String gameId, Player blackPlayer, Player whitePlayer) {
		this.gameId = gameId;
		this.blackPlayer = blackPlayer;
		this.whitePlayer = whitePlayer;
		this.board = new char[BOARD_SIZE][BOARD_SIZE];
		for (char[] row : board) {
			Arrays.fill(row, '.');
		}
		this.nextPlayer = blackPlayer;
		this.gameStartTime = Instant.now();
		this.gameEndTime = null;
		this.winnerPlayer = null;
		this.blackPlayerTimeRemaining = INITIAL_TIME_PER_PLAYER;
		this.whitePlayerTimeRemaining = INITIAL_TIME_PER_PLAYER;
		this.currentTurnStartTime = Instant.now();
	}

	/**
	 * Attempts to apply a move for the given player at (row, col).
	 *
	 * @param row          zero-based row index (0 ≤ row < 15)
	 * @param col          zero-based column index (0 ≤ col < 15)
	 * @param movingPlayer the Player attempting the move
	 * @return a MoveResult indicating acceptance, invalid move, wrong turn, or win
	 */
	public MoveResult applyMove(int row, int col, Player movingPlayer) {
		if (gameEndTime != null) {
			// Já terminou
			return MoveResult.INVALID_MOVE;
		}

		// Verificar timeout ANTES de processar a jogada
		if (isCurrentPlayerTimeExpired()) {
			timeoutCurrentPlayer();
			return MoveResult.TIMEOUT; // Novo resultado!
		}

		if (!nextPlayer.equals(movingPlayer)) {
			return MoveResult.NOT_YOUR_TURN;
		}
		if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
			return MoveResult.INVALID_MOVE;
		}
		if (board[row][col] != '.') {
			return MoveResult.INVALID_MOVE;
		}

		// Atualizar tempo do jogador atual ANTES de fazer a jogada
		updateCurrentPlayerTime();

		// Colocar peça
		char stone = movingPlayer.equals(blackPlayer) ? 'B' : 'W';
		board[row][col] = stone;

		// Verificar vitória
		if (checkWin(row, col)) {
			gameEndTime = Instant.now();
			winnerPlayer = movingPlayer;
			return MoveResult.WIN;
		}

		// Mudar turno (currentTurnStartTime já foi atualizado em
		// updateCurrentPlayerTime)
		nextPlayer = nextPlayer.equals(blackPlayer) ? whitePlayer : blackPlayer;

		return MoveResult.ACCEPTED;
	}

	private boolean checkWin(int row, int col) {
		char placedStone = board[row][col];
		int[][] directions = { { 1, 0 }, { 0, 1 }, { 1, 1 }, { 1, -1 } };
		for (int[] dir : directions) {
			int count = 1 + countInDirection(row, col, dir[0], dir[1], placedStone)
					+ countInDirection(row, col, -dir[0], -dir[1], placedStone);
			if (count >= 5) {
				return true;
			}
		}
		return false;
	}

	private int countInDirection(int row, int col, int dr, int dc, char stone) {
		int r = row + dr, c = col + dc, matches = 0;
		while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == stone) {
			matches++;
			r += dr;
			c += dc;
		}
		return matches;
	}

	/**
	 * Forces a forfeit by the given player; the opponent is declared winner.
	 *
	 * @param forfeitingPlayer the Player who forfeits
	 */
	public void forfeit(Player forfeitingPlayer) {
		if (gameEndTime != null) {
			return; // already finished
		}
		gameEndTime = Instant.now();
		winnerPlayer = forfeitingPlayer.equals(blackPlayer) ? whitePlayer : blackPlayer;
	}

	/**
	 * @return the Player assigned Black stones
	 */
	public Player getBlack() {
		return blackPlayer;
	}

	// ----------- Time-Logic --------------

	/**
	 * @return tempo restante para o jogador Black
	 */
	public Duration getBlackPlayerTimeRemaining() {
		return blackPlayerTimeRemaining;
	}

	/**
	 * @return tempo restante para o jogador atual
	 */
	public Duration getCurrentPlayerTimeRemaining() {
		if (nextPlayer.equals(blackPlayer)) {
			return blackPlayerTimeRemaining;
		} else {
			return whitePlayerTimeRemaining;
		}
	}

	/**
	 * @return the total elapsed time since start, or final duration if finished
	 */
	public Duration getElapsedTime() {
		Instant end = (gameEndTime != null) ? gameEndTime : Instant.now();
		return Duration.between(gameStartTime, end);
	}

	/**
	 * @return the unique identifier of this game session
	 */
	public String getGameId() {
		return gameId;
	}

	/**
	 * @return the next Player in this session
	 */
	public Player getNextPlayer() {
		return nextPlayer;
	}

	/**
	 * Receives a player and returns it's opponent.
	 * 
	 * @param currentPlayer the current player
	 * @return the opponent Player
	 */
	public Player getOpponentOf(Player currentPlayer) {
		return currentPlayer.equals(blackPlayer) ? whitePlayer : blackPlayer;
	}

	/**
	 * @return the Player assigned White stones
	 */
	public Player getWhite() {
		return whitePlayer;
	}

	/**
	 * @return tempo restante para o jogador White
	 */
	public Duration getWhitePlayerTimeRemaining() {
		return whitePlayerTimeRemaining;
	}

	/**
	 * @return the username of the winning player, or null if still in progress
	 */
	public String getWinnerUsername() {
		return (winnerPlayer == null) ? null : winnerPlayer.getUsername();
	}

	/**
	 * Verifica se o jogador atual excedeu o tempo limite.
	 * 
	 * @return true se o tempo esgotou
	 */
	public boolean isCurrentPlayerTimeExpired() {
		if (gameEndTime != null) {
			return false; // Jogo já terminou
		}

		Duration elapsed = Duration.between(currentTurnStartTime, Instant.now());
		Duration remaining = getCurrentPlayerTimeRemaining().minus(elapsed);

		return remaining.isNegative() || remaining.isZero();
	}

	/**
	 * @return true if this game session has finished (win or forfeit)
	 */
	public boolean isFinished() {
		return gameEndTime != null;
	}

	/**
	 * Força timeout para o jogador atual.
	 * 
	 * @return o jogador que ganhou por timeout
	 */
	public Player timeoutCurrentPlayer() {
		if (gameEndTime != null) {
			return winnerPlayer; // Jogo já terminou
		}

		gameEndTime = Instant.now();
		winnerPlayer = nextPlayer.equals(blackPlayer) ? whitePlayer : blackPlayer;

		// Zerar tempo do jogador que perdeu por timeout
		if (nextPlayer.equals(blackPlayer)) {
			blackPlayerTimeRemaining = Duration.ZERO;
		} else {
			whitePlayerTimeRemaining = Duration.ZERO;
		}

		return winnerPlayer;
	}

	// ----------- Internal win-checking logic -----------

	/**
	 * Builds a snapshot of the current board and timing info for messaging. Inclui
	 * informação de tempo para o novo sistema de timer.
	 *
	 * @return a GameState DTO containing gameId, board, timing info and timestamp
	 */
	public GameState toGameState() {
		List<String> rowStrings = new ArrayList<>(BOARD_SIZE);
		for (int r = 0; r < BOARD_SIZE; r++) {
			rowStrings.add(new String(board[r]));
		}

		String nextColor = nextPlayer.equals(blackPlayer) ? "Black" : "White";

		// Calcular tempo restante atual (considerando tempo já gasto na jogada atual)
		Duration currentBlackTime = blackPlayerTimeRemaining;
		Duration currentWhiteTime = whitePlayerTimeRemaining;

		// Se o jogo ainda está ativo, subtrair tempo já gasto na jogada atual
		if (gameEndTime == null && currentTurnStartTime != null) {
			Duration elapsedInCurrentTurn = Duration.between(currentTurnStartTime, Instant.now());

			if (nextPlayer.equals(blackPlayer)) {
				currentBlackTime = blackPlayerTimeRemaining.minus(elapsedInCurrentTurn);
				// Não deixar ficar negativo
				if (currentBlackTime.isNegative()) {
					currentBlackTime = Duration.ZERO;
				}
			} else {
				currentWhiteTime = whitePlayerTimeRemaining.minus(elapsedInCurrentTurn);
				// Não deixar ficar negativo
				if (currentWhiteTime.isNegative()) {
					currentWhiteTime = Duration.ZERO;
				}
			}
		}

		return new GameState(gameId, rowStrings, nextColor, currentBlackTime, currentWhiteTime, Instant.now() // Timestamp
																												// de
																												// quando
																												// este
																												// estado
																												// foi
																												// criado
		);
	}

	/**
	 * Atualiza o tempo gasto pelo jogador atual e muda para o próximo. Deve ser
	 * chamado antes de mudar nextPlayer.
	 */
	private void updateCurrentPlayerTime() {
		if (currentTurnStartTime == null) {
			return;
		}

		Duration elapsed = Duration.between(currentTurnStartTime, Instant.now());

		if (nextPlayer.equals(blackPlayer)) {
			blackPlayerTimeRemaining = blackPlayerTimeRemaining.minus(elapsed);
			// Garantir que não fica negativo
			if (blackPlayerTimeRemaining.isNegative()) {
				blackPlayerTimeRemaining = Duration.ZERO;
			}
		} else {
			whitePlayerTimeRemaining = whitePlayerTimeRemaining.minus(elapsed);
			// Garantir que não fica negativo
			if (whitePlayerTimeRemaining.isNegative()) {
				whitePlayerTimeRemaining = Duration.ZERO;
			}
		}

		// Iniciar timer para o próximo jogador
		currentTurnStartTime = Instant.now();
	}
}

// Modificar GameState.java para incluir informação de tempo:

package core;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * GameState represents a complete snapshot of a Gobang game board, including
 * timing information for the new timer system.
 */
public class GameState {

	/** Unique identifier of the game session. */
	private final String gameId;

	/**
	 * Textual representation of each board row. Each string has length = boardSize,
	 * where: '.' = empty cell, 'B' = Black stone, 'W' = White stone.
	 */
	private final List<String> boardRows;

	/** Color of the player who moves next ("Black" or "White"). */
	private final String nextPlayerColor;

	/** Tempo restante para o jogador Black. */
	private final Duration blackPlayerTimeRemaining;

	/** Tempo restante para o jogador White. */
	private final Duration whitePlayerTimeRemaining;

	/** Timestamp de quando este estado foi criado (para cálculo no cliente). */
	private final Instant timestamp;

	/**
	 * Constructs a new GameState instance.
	 *
	 * @param gameId                   unique identifier for the game session
	 * @param boardRows                list of row strings, one per board row
	 * @param nextPlayerColor          color of the player who will move next
	 * @param blackPlayerTimeRemaining tempo restante para Black
	 * @param whitePlayerTimeRemaining tempo restante para White
	 * @param timestamp                quando este estado foi criado
	 */
	public GameState(String gameId, List<String> boardRows, String nextPlayerColor, Duration blackPlayerTimeRemaining,
			Duration whitePlayerTimeRemaining, Instant timestamp) {
		this.gameId = gameId;
		this.boardRows = List.copyOf(boardRows);
		this.nextPlayerColor = nextPlayerColor;
		this.blackPlayerTimeRemaining = blackPlayerTimeRemaining;
		this.whitePlayerTimeRemaining = whitePlayerTimeRemaining;
		this.timestamp = timestamp;
	}

	/**
	 * @return tempo restante para o jogador Black
	 */
	public Duration getBlackPlayerTimeRemaining() {
		return blackPlayerTimeRemaining;
	}

	/**
	 * @return an unmodifiable list of row strings, each of length boardSize
	 */
	public List<String> getBoardRows() {
		return boardRows;
	}

	/**
	 * @return tempo restante para o jogador atual (que vai jogar)
	 */
	public Duration getCurrentPlayerTimeRemaining() {
		return "Black".equals(nextPlayerColor) ? blackPlayerTimeRemaining : whitePlayerTimeRemaining;
	}

	/**
	 * @return the unique identifier of the game session
	 */
	public String getGameId() {
		return gameId;
	}

	/**
	 * @return the color ("Black" or "White") of the next player to move
	 */
	public String getNextPlayerColor() {
		return nextPlayerColor;
	}

	/**
	 * @return timestamp de quando este estado foi criado
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * @return tempo restante para o jogador White
	 */
	public Duration getWhitePlayerTimeRemaining() {
		return whitePlayerTimeRemaining;
	}

	@Override
	public String toString() {
		return "GameState{" + "gameId='" + gameId + '\'' + ", nextPlayerColor='" + nextPlayerColor + '\''
				+ ", blackTime=" + blackPlayerTimeRemaining + ", whiteTime=" + whitePlayerTimeRemaining + ", timestamp="
				+ timestamp + ", boardRows=" + boardRows + '}';
	}
}
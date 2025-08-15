package gui.game;

/**
 * Defines the three possible states of a Gobang cell: • FREE = empty cell •
 * BLACK = a Black stone (rendered as cyan in the UI) • WHITE = a White stone
 * (rendered as magenta in the UI)
 *
 * Also provides text‐based markers for any console or debug output.
 */
public class CellState {
	/** No stone placed. */
	public static final int FREE = 0;
	/** Black stone; shown in cyan on the GameBoard. */
	public static final int BLACK = 5;
	/** White stone; shown in magenta on the GameBoard. */
	public static final int WHITE = 6;

	/**
	 * Returns a single‐character string for logging or text views: "." → FREE "B" →
	 * BLACK "W" → WHITE
	 *
	 * @param state one of FREE, BLACK or WHITE
	 * @return "." or "B" or "W", or "?" if unknown
	 */
	public static String decodeEstadoString(int state) {
		switch (state) {
		case FREE:
			return ".";
		case BLACK:
			return "B";
		case WHITE:
			return "W";
		default:
			return "?";
		}
	}
}

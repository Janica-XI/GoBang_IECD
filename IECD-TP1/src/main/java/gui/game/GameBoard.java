package gui.game;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * GameBoard is the Gobang board component. Cells are one of: - CellState.FREE:
 * empty - CellState.BLACK: drawn in cyan - CellState.WHITE: drawn in magenta
 * <p>
 * It forwards valid clicks on empty intersections to a ClickHandler, and
 * provides methods to programmatically place or clear stones.
 * 
 * CORRIGIDO: Coordenadas consistentes [row, col] em todo o código
 */
public class GameBoard extends IntersectionBoard {
	private static final long serialVersionUID = 1L;

	private ClickHandler clickHandler;
	private boolean readOnly;

	/**
	 * Constructs an interactive 15×15 Gobang board. Removes the inherited click
	 * behavior and installs a listener that reports clicks on FREE intersections.
	 */
	public GameBoard() {
		super();
		// remove any default mouse listeners
		for (var ml : getMouseListeners()) {
			removeMouseListener(ml);
		}
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (readOnly || clickHandler == null)
					return;
				int[] rc = findIntersection(e.getX(), e.getY());
				int row = rc[0], col = rc[1]; // CORRIGIDO: row=linha, col=coluna
				if (row >= 0 && col >= 0 && getEstado(row, col) == CellState.FREE) {
					clickHandler.clicked(row, col, CellState.FREE); // (row, col)
				}
			}
		});
	}

	/** Clears all stones from the board. */
	public void clearBoard() {
		for (int row = 0; row < INTERSECTIONS; row++) {
			for (int col = 0; col < INTERSECTIONS; col++) {
				setPieceType(row, col, 0);
			}
		}
		repaint();
	}

	/**
	 * CORRIGIDO: Finds the nearest board intersection to the given pixel
	 * 
	 * @return {row, col} or {-1,-1} if none is within half a cell.
	 */
	private int[] findIntersection(int mx, int my) {
		int w = getWidth(), h = getHeight();
		int boardSize = Math.min(w, h) - 40;
		int cellSize = boardSize / GRID_SQUARES;
		int offsetX = (w - cellSize * GRID_SQUARES) / 2;
		int offsetY = (h - cellSize * GRID_SQUARES) / 2;

		for (int row = 0; row < INTERSECTIONS; row++) {
			for (int col = 0; col < INTERSECTIONS; col++) {
				int px = offsetX + col * cellSize; // col determina X (horizontal)
				int py = offsetY + row * cellSize; // row determina Y (vertical)

				if (Point.distance(px, py, mx, my) <= cellSize / 2) {
					return new int[] { row, col }; // RETORNA [row, col]
				}
			}
		}
		return new int[] { -1, -1 };
	}

	/**
	 * Returns the Gobang‐specific state code of a cell.
	 * 
	 * @param row zero-based row (linha vertical)
	 * @param col zero-based column (coluna horizontal)
	 * @return CellState.FREE, BLACK or WHITE
	 */
	public int getEstado(int row, int col) {
		return switch (getPieceType(row, col)) {
		case 2 -> CellState.BLACK;
		case 1 -> CellState.WHITE;
		default -> CellState.FREE;
		};
	}

	/**
	 * Place a stone in the specified cell.
	 * 
	 * @param row    zero-based row (linha vertical)
	 * @param col    zero-based column (coluna horizontal)
	 * @param estado one of CellState.FREE, BLACK, WHITE
	 */
	public void setCelula(int row, int col, int estado) {
		int type = switch (estado) {
		case CellState.BLACK -> 1; // cyan
		case CellState.WHITE -> 2; // magenta
		default -> 0; // empty
		};
		setPieceType(row, col, type); // CORRIGIDO: (row, col)
	}

	/**
	 * Registers a handler to receive click events.
	 * 
	 * @param handler invoked as clicked(row, col, state)
	 */
	public void setClickHandler(ClickHandler handler) {
		this.clickHandler = handler;
	}

	// ───── Internal Helpers ─────

	/**
	 * Enable or disable user interaction.
	 * 
	 * @param ro true to disable clicks
	 */
	public void setReadOnly(boolean ro) {
		this.readOnly = ro;
	}
}
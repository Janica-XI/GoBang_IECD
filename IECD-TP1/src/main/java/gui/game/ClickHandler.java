package gui.game;

/**
 * Callback interface for handling user clicks on the Gobang board.
 * <p>
 * When the player clicks on an intersection, this method is invoked with the
 * row/column of the click and the cell's current state. Implementers should
 * check that the cell is FREE and it's the player's turn before placing a
 * stone.
 * 
 * COORDENADAS: (row, col) onde row=linha vertical, col=coluna horizontal
 */
public interface ClickHandler {

	/**
	 * Invoked when the user clicks (presses) on a board cell.
	 *
	 * @param row    the zero-based row index (linha vertical, 0=topo)
	 * @param col    the zero-based column index (coluna horizontal, 0=esquerda)
	 * @param estado the current state of that cell: CellState.FREE (empty),
	 *               CellState.BLACK (drawn in cyan), or CellState.WHITE (drawn in
	 *               magenta)
	 */
	void clicked(int row, int col, int estado);
}
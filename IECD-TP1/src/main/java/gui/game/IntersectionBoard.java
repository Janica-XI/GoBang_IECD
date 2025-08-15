package gui.game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Low-level Gobang board component: • Draws a 15×15 grid with star-points •
 * Shows hover preview • Renders pieces (1=cyan, 2=magenta)
 * 
 * CORRIGIDO: Coordenadas consistentes entre visualização e lógica - Array
 * interno: [row][col] onde row=0 é topo, col=0 é esquerda - Retorno de métodos:
 * [row, col] consistente
 */
public class IntersectionBoard extends JComponent {
	private static final long serialVersionUID = 1L;

	// Board sizing
	protected static final int GRID_SQUARES = 14; // squares per row/column
	protected static final int INTERSECTIONS = GRID_SQUARES + 1;
	private static final int PADDING = 20; // margin
	private static final int[][] STAR_POINTS = { { 3, 3 }, { 11, 3 }, { 7, 7 }, { 3, 11 }, { 11, 11 } };

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame f = new JFrame("Gobang Board - Fixed Coordinates");
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.add(new IntersectionBoard(), BorderLayout.CENTER);
			f.pack();
			f.setLocationRelativeTo(null);
			f.setVisible(true);
		});
	}
	// State - IMPORTANTE: [row][col] onde row=vertical, col=horizontal
	private final int[][] pieceType = new int[INTERSECTIONS][INTERSECTIONS];

	private int hoverRow = -1, hoverCol = -1;

	public IntersectionBoard() {
		// Click handler toggles state
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				handleClick(e);
			}
		});
		// Hover preview
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				handleHover(e);
			}
		});
	}

	protected int getPieceType(int row, int col) {
		return pieceType[row][col];
	}

	// ─── Public API for GameBoard ─────────────────────────────────────────

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(600, 600);
	}

	private void handleClick(MouseEvent e) {
		var coords = locateIntersection(e.getX(), e.getY());
		int row = coords[0], col = coords[1];
		if (row < 0 || col < 0)
			return;

		if (pieceType[row][col] != 0) {
			pieceType[row][col] = 0;
		} else if (SwingUtilities.isLeftMouseButton(e)) {
			pieceType[row][col] = 1;
		} else if (SwingUtilities.isRightMouseButton(e)) {
			pieceType[row][col] = 2;
		}
		repaint();
	}

	// ─── Interaction handlers ─────────────────────────────────────────────

	private void handleHover(MouseEvent e) {
		var coords = locateIntersection(e.getX(), e.getY());
		if (coords[0] != hoverRow || coords[1] != hoverCol) {
			hoverRow = coords[0];
			hoverCol = coords[1];
			repaint();
		}
	}

	/**
	 * CORRIGIDO: Converte coordenadas do mouse para [row, col]
	 * 
	 * @param mx coordenada X do mouse (horizontal)
	 * @param my coordenada Y do mouse (vertical)
	 * @return array [row, col] onde row=linha vertical, col=coluna horizontal
	 */
	private int[] locateIntersection(int mx, int my) {
		int w = getWidth(), h = getHeight();
		int boardSize = Math.min(w, h) - 2 * PADDING;
		int cell = boardSize / GRID_SQUARES;
		int x0 = (w - boardSize) / 2;
		int y0 = (h - boardSize) / 2;

		// Encontrar a interseção mais próxima
		for (int row = 0; row < INTERSECTIONS; row++) {
			for (int col = 0; col < INTERSECTIONS; col++) {
				int px = x0 + col * cell; // col determina posição X (horizontal)
				int py = y0 + row * cell; // row determina posição Y (vertical)

				if (Point.distance(px, py, mx, my) <= cell / 2) {
					return new int[] { row, col }; // RETORNA [row, col]
				}
			}
		}
		return new int[] { -1, -1 };
	}

	// ─── Helper to map pixel → grid index ─────────────────────────────────

	@Override
	protected void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int w = getWidth(), h = getHeight();
		int boardSize = Math.min(w, h) - 2 * PADDING;
		int cell = boardSize / GRID_SQUARES;
		int x0 = (w - boardSize) / 2;
		int y0 = (h - boardSize) / 2;
		int pieceR = cell / 2 - 4;
		int starR = cell / 6;

		// Background
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, w, h);

		// Grid
		g.setColor(Color.GRAY);
		for (int i = 0; i <= GRID_SQUARES; i++) {
			int x = x0 + i * cell, y = y0 + i * cell;
			g.drawLine(x, y0, x, y0 + GRID_SQUARES * cell);
			g.drawLine(x0, y, x0 + GRID_SQUARES * cell, y);
		}

		// Star-points (row, col)
		for (var p : STAR_POINTS) {
			int sx = x0 + p[1] * cell, sy = y0 + p[0] * cell; // p[1]=col=x, p[0]=row=y
			g.fillOval(sx - starR, sy - starR, 2 * starR, 2 * starR);
		}

		// Hover preview
		if (hoverRow >= 0 && hoverCol >= 0 && pieceType[hoverRow][hoverCol] == 0) {
			int cx = x0 + hoverCol * cell, cy = y0 + hoverRow * cell; // col=x, row=y
			g.setColor(Color.GRAY);
			g.fillOval(cx - pieceR, cy - pieceR, 2 * pieceR, 2 * pieceR);
		}

		// Pieces
		for (int row = 0; row < INTERSECTIONS; row++) {
			for (int col = 0; col < INTERSECTIONS; col++) {
				int t = pieceType[row][col];
				if (t != 0) {
					int cx = x0 + col * cell, cy = y0 + row * cell; // col=x, row=y
					g.setColor(t == 1 ? Color.CYAN : Color.MAGENTA);
					g.fillOval(cx - pieceR, cy - pieceR, 2 * pieceR, 2 * pieceR);
				}
			}
		}
	}

	// ─── Demo ─────────────────────────────────────────────────────────────

	protected void setPieceType(int row, int col, int type) {
		pieceType[row][col] = type;
		repaint();
	}
}
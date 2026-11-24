package checkers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class BoardSquare extends JPanel {
    private final int row;
    private final int col;
    private final GameBoard gameBoard;
    private boolean selected = false;

    public BoardSquare(int row, int col, GameBoard gameBoard) {
        this.row = row;
        this.col = col;
        this.gameBoard = gameBoard;

        if ((row + col) % 2 == 0){
            setBackground(Color.WHITE);
        } else {
            setBackground(Color.GRAY);
        }

        addMouseListener(new MouseAdapter() {
            @Override 
            public void mousePressed(MouseEvent e) {
                handleSquareClick(row, col);
            }
        });
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    public void handleSquareClick(int row, int col) {
        int piece = gameBoard.getPieceAt(row, col);
        int selectedRow = gameBoard.getSelectedRow();
        int selectedCol = gameBoard.getSelectedCol();

        //first click: select any occupied square (server enforces turn and ownership)
        if (selectedRow == -1) {
            if (piece != GameBoard.EMPTY) {
                gameBoard.setSelectedSquare(row, col);
            }
            return;
        }
        //clicking the same square deselects
        if (selectedRow == row && selectedCol == col) {
            gameBoard.clearSelection();
            return;
        }

        //clicking another piece switches selection
        if (piece != GameBoard.EMPTY) {
            gameBoard.setSelectedSquare(row, col);
            return;
        }

        //second click on an empty square: build GameMessage.MOVE and send via Client
        gameBoard.clearSelection();

        if (gameBoard.getClient() != null) {
            GameMessage moveMsg = new GameMessage (
                GameMessage.MOVE,
                gameBoard.getUsername(),
                selectedRow,
                selectedCol,
                row,
                col
            );
            gameBoard.getClient().sendMove(moveMsg.toJson());
        }
    }  

    @Override 
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //draw square background
        boolean isDark = (row + col) % 2 != 0;
        g2.setColor(isDark ? Color.GRAY : Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        //selected square highlight
        if (selected || (row == gameBoard.getSelectedRow() && col == gameBoard.getSelectedCol())) {
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(4));
            g2.drawRect(2, 2, getWidth() - 4, getHeight() - 4);
        }
    
        //draw piece
        int piece = gameBoard.getPieceAt(row, col);
        if (piece == GameBoard.EMPTY) {
            return;
        }

        int padding = Math.min(getWidth(), getHeight()) / 6;
        int diameter = Math.min(getWidth(), getHeight()) - (padding * 2);
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;

        //subtle drop shadow
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(x + 3, y + 3, diameter, diameter);
            
        //classic red and black pieces
        boolean isRed = (piece == GameBoard.RED || piece == GameBoard.RED_KING);
        g2.setColor(isRed ? Color.RED : Color.BLACK);
        g2.fillOval(x, y, diameter, diameter);

        //draw crown for king pieces
        if (piece == GameBoard.RED_KING || piece == GameBoard.BLACK_KING) {
            drawCrown(g2, x, y, diameter);
        }
    }
    private void drawCrown(Graphics2D g2, int x, int y, int size) {
        g2.setColor(Color.YELLOW);
        int crownWidth = (int) (size * 0.5);
        int crownHeight = (int) (size * 0.4);

        int cx = x + (size - crownWidth) / 2;
        int cy = y + (size - crownHeight) / 2 + 3;

        int[] xPoints = {
            cx,                         // bottom left
            cx,                         // left peak top
            cx + crownWidth / 4,        // left inner valley
            cx + crownWidth / 2,        // center peak top
            cx + 3 * crownWidth / 4,    // right inner valley
            cx + crownWidth,            // right peak top
            cx + crownWidth             // bottom right
        };
        int[] yPoints = {
            cy + crownHeight,           // bottom left base
            cy,                         // left peak height
            cy + crownHeight / 2,       // left valley floor
            cy - 2,                     // center peak height
            cy + crownHeight / 2,       // right valley floor
            cy,                         // right peak height
            cy + crownHeight            // bottom right base
        };
        g2.fillPolygon(xPoints, yPoints, 7);
    } 
}
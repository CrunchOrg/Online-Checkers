package main.java.checkers;

import com.google.gson.JsonSyntaxException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

public class GameBoard extends JFrame {
    //piece constants
    public static final int EMPTY = 0;
    public static final int RED = 1;
    public static final int RED_KING = 2;
    public static final int BLACK = 3;
    public static final int BLACK_KING = 4;

    //turn constants
    public static final int TURN_RED = 1;
    public static final int TURN_BLACK = 2;

    private static final int ROWS = 8;
    private static final int COLS = 8;

    private final Client client;

    //transferred GUI componenets from checker.java
    private JFrame mainFrame;
    private JPanel boardPanel;
    private JLabel turnLabel;
    private BoardSquare[][] visualSquares;

    //client-side rendering state only
    private int[][] boardState = new int[ROWS][COLS];
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int myPlayerNumber = 0;
    private String username = "";

    //no-arg constructor 
    public GameBoard() {
        this(null);
    }

    //board connected to the network client
    public GameBoard(Client client) {
        this.client = client;
        createBoard();
        initializeSquares();
        mainFrame.setVisible(true);
    }

    //provides access to associated client
    public Client getClient() {
        return this.client;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    //creates main JFrame, turn label, and 8x8 grid container
    public void createBoard() {
        mainFrame = new JFrame("Checkers");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        turnLabel = new JLabel("Waiting for sever...", SwingConstants.CENTER);
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        turnLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainFrame.add(turnLabel, BorderLayout.NORTH);

        boardPanel = new JPanel(new GridLayout(ROWS, COLS));
        boardPanel.setPreferredSize(new Dimension(600, 600));
        mainFrame.add(boardPanel, BorderLayout.CENTER);

        visualSquares = new BoardSquare[ROWS][COLS];
    }

    //creates all 64 BoardSquareobjects and adds to grid
    public void initializeSquares() {
        boardPanel.removeAll();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                BoardSquare square = new BoardSquare(r, c);
                visualSquares[r][c] = square;
                boardPanel.add(square);
            }
        }
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
    }

    //no-arg refresh method
    public void updateBoard() {
        SwingUtilities.invokeLater(() -> boardPanel.repaint());
    }

    //updates board using state received from server
    public void updateBoard(int[][] newBoardState) {
        SwingUtilities.invokeLater(() -> {
            if (newBoardState != null && newBoardState.length == ROWS && newBoardState[0].length == COLS) {
                for (int r = 0; r < ROWS; r++) {
                    System.arraycopy(newBoardState[r], 0, this.boardState[r], 0, COLS);
                }
            }
            clearSelection();
            boardPanel.repaint();
        });
    }

    //displays newly initializes board from server
    public void resetBoard(int[][] newBoardState) {
        updateBoard(newBoardState);
    }

    //displays whose turn it is based on the server's turn indicator
    public void updateTurnDisplay(int currentTurn) {
        SwingUtilities.invokeLater(() -> {
            String roleTag = (myPlayerNumber == TURN_RED) ? " (You: Red)" : (myPlayerNumber == TURN_BLACK) ? " (You: Black)" : "";

            if (currentTurn == TURN_RED) {
                turnLabel.setText("Red's Turn" + roleTag);
            } else if (currentTurn == TURN_BLACK) {
                turnLabel.setText("Black's Turn" + roleTag);
            } else {
                turnLabel.setText("Waiting for server...");
            }
        });
    }

    //clears currently selected square highlight
    public void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        if (boardPanel != null) {
            boardPanel.repaint();
        }
    }

    //displays game result dialog when server signals game over
    public void showGameOverDialog(String message) {
        SwingUtilities.invokeLater(() -> {
        turnLabel.setText("Game Over: " + message);
        JOptionPane.showMessageDialog(
            mainFrame,
            message,
            "Game Over",
            JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    //displays a skipped-turn notification dialog
    public void showSkippedTurnDialog(String message) {
        SwingUtilities.invokeLater(() ->
        JOptionPane.showMessageDialog(
            mainFrame,
            message,
            "Turn Skipped",
            JOptionPane.INFORMATION_MESSAGE
            )
        );
    }

    //displays status/error feedback from the server
    public void showStatusMessage(String title, String message, int messageType) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(
                mainFrame,
                message,
                title,
                messageType
            )
        );
    }

    //parses raw JSON from the Client socket thread into a GameMessage
    public void handleServerJson(String json) {
        if (json == null || json.isBlank()) {
            return;
        }

        try {
            GameMessage msg = GameMessage.fromJson(json);
            handleServerMessage(msg);
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON from server: " + e.getMessage());
        }
    }

    //handles a deserialized GameMessage sent from server
    public void handleServerMessage(GameMessage msg) {
        if (msg == null || msg.getAction() == null) {
            return;
        }

        switch (msg.getAction()) {
            case GameMessage.WAITING -> SwingUtilities.invokeLater(() ->
                    turnLabel.setText(msg.getMessage() != null ? msg.getMessage() : "Waiting for opponent...")
            );

            case GameMessage.ASSIGN_PLAYER -> {
                this.myPlayerNumber = msg.getPlayerNumber();
                SwingUtilities.invokeLater(() -> {
                    String color = (myPlayerNumber == TURN_RED) ? "Red" : "Black";
                    mainFrame.setTitle("Checkers - Player " + myPlayerNumber + " (" + color + ")");
                });
            }
            case GameMessage.GAME_START, GameMessage.BOARD_UPDATE -> {
                if (msg.getBoardState() != null) {
                    updateBoard(msg.getBoardState());
                }
                updateTurnDisplay(msg.getCurrentTurn());
            }

            case GameMessage.INVALID_MOVE -> showStatusMessage(
                    "Invalid Move",
                    msg.getMessage() != null ? msg.getMessage() : "That move is not allowed.",
                    JOptionPane.WARNING_MESSAGE
            );

            case GameMessage.SKIP_TURN -> {
                updateTurnDisplay(msg.getCurrentTurn());
                showSkippedTurnDialog(
                        msg.getMessage() != null ? msg.getMessage() : "No valid moves available. Turn skipped."
                );
            }
            case GameMessage.GAME_OVER -> {
                if (msg.getBoardState() != null) {
                    updateBoard(msg.getBoardState());
                }
                showGameOverDialog(
                        msg.getMessage() != null ? msg.getMessage() : "Game Over."
                );
            }

            case GameMessage.OPPONENT_LEFT -> {
                String text = msg.getMessage() != null ? msg.getMessage() : "Your opponent disconnected.";
                SwingUtilities.invokeLater(() -> turnLabel.setText(text));
                showStatusMessage("Opponent Left", text, JOptionPane.INFORMATION_MESSAGE);
            }

            case GameMessage.ERROR -> showStatusMessage(
                    "Server Error",
                    msg.getMessage() != null ? msg.getMessage() : "An unexpected error occurred.",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void handleSquareClick(int row, int col) {
        //first click: select any occupied square (server enforces turn and ownership)
        if (selectedRow == -1) {
            if (boardState[row][col] != EMPTY) {
                selectedRow = row;
                selectedCol = col;
                boardPanel.repaint();
            }
            return;
        }
        //clicking the same square deselects
        if (selectedRow == row && selectedCol == col) {
            clearSelection();
            return;
        }

        //clicking another piece switches selection
        if (boardState[row][col] != EMPTY) {
            selectedRow = row;
            selectedCol = col;
            boardPanel.repaint();
            return;
        }

        //second click on an empty square: build GameMessage.MOVE and send via Client
        int fromR = selectedRow;
        int fromC = selectedCol;
        clearSelection();

        if (client != null) {
            GameMessage moveMsg = new GameMessage (
                GameMessage.MOVE,
                username,
                fromR,
                fromC,
                row,
                col
            );
            client.sendMove(moveMsg.toJson());
        }
    }

    //represents one of the 64 graphical squares on the board
    private class BoardSquare extends JPanel {
        private final int row;
        private final int col;

        public BoardSquare(int row, int col) {
            this.row = row;
            this.col = col;

            addMouseListener(new MouseAdapter() {
                @Override 
                public void mousePressed(MouseEvent e) {
                    handleSquareClick(row, col);
                }
            });
        }

        @Override 
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            //draw square background
            boolean isDark = (row + col) % 2 != 0;
            g2.setColor(isDark ? new Color(118, 150, 86) : new Color(238, 238, 210));
            g2.fillRect(0, 0, getWidth(), getHeight());

            //selected square highlight
            if (row == selectedRow && col == selectedCol) {
                g2.setColor(new Color(255, 255, 0, 120));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(3));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }

            //draw piece
            int piece = boardState[row][col];
            if (piece == EMPTY) {
                return;
            }

            int padding = Math.min(getWidth(), getHeight()) / 6;
            int diameter = Math.min(getWidth(), getHeight()) - (padding * 2);
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;

            boolean isRed = (piece == RED || piece == RED_KING);
            g2.setColor(isRed ? new Color(200, 40, 40) : new Color(35,35, 35));
            g2.fillOval(x, y, diameter, diameter);

            g2.setColor(isRed ? new Color(130, 20, 20) : new Color(80, 80, 80));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(x, y, diameter, diameter);

            //draw crown for king pieces
            if (piece == RED_KING || piece == BLACK_KING) {
                drawCrown(g2, getWidth() / 2.0, getHeight() / 2.0, diameter * 0.45);
            }
        }
        private void drawCrown(Graphics2D g2, double cx, double cy, double size) {
            double half = size / 2.0;
            Path2D crown = new Path2D.Double();
            crown.moveTo(cx - half, cy + half * 0.6);
            crown.lineTo(cx - half, cy - half * 0.4);
            crown.lineTo(cx - half * 0.4, cy + half * 0.1);
            crown.lineTo(cx, cy - half * 0.6);
            crown.lineTo(cx + half * 0.4, cy + half * 0.1);
            crown.lineTo(cx + half, cy - half * 0.4);
            crown.lineTo(cx + half, cy + half * 0.6);
            crown.closePath();

            g2.setColor(new Color(255, 215, 0));
            g2.fill(crown);
            g2.setColor(new Color(180, 140, 0));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(crown);
        }
    }    
}


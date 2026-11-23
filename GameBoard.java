package main.java.checkers;

import com.google.gson.JsonSyntaxException;

import javax.swing.*;
import java.awt.*;

public class GameBoard extends JFrame {
    //piece constants
    public static final int EMPTY = 0;
    public static final int RED = 1;
    public static final int BLACK = 2;
    public static final int RED_KING = 3;
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

    public String getUsername() {
        return username;
    }

    public int getPieceAt(int row, int col) {
        return boardState[row][col];
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public int getSelectedCol() {
        return selectedCol;
    }

    public void setSelectedSquare(int row, int col) {
        clearSelection();
        this.selectedRow = row;
        this.selectedCol = col;
        if (visualSquares != null) {
            visualSquares[row][col].setSelected(true);
        }
        if (boardPanel != null) {
            boardPanel.repaint();
        }
    }

    //creates main JFrame, turn label, and 8x8 grid container
    public void createBoard() {
        mainFrame = new JFrame("Checkers");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        turnLabel = new JLabel("Waiting for server...", SwingConstants.CENTER);
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
                BoardSquare square = new BoardSquare(r, c, this);
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
        if (selectedRow != -1 && selectedCol != -1 && visualSquares != null) {
            visualSquares[selectedRow][selectedCol].setSelected(false);
        }
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
}

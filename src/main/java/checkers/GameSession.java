package checkers;

public class GameSession {

	private static final int EMPTY = 0;
    private static final int RED = 1;
    private static final int BLACK = 2;
    private static final int RED_KING = 3;
    private static final int BLACK_KING = 4;    
    private final ClientHandler player1;
    private final ClientHandler player2;
    private int currentTurn = RED;
    private boolean isChainJumping = false;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean gameOver = false;
    
    //8x8 array to represent checkers board
    //0= empty, 1 = p1 piece, 2 = p2 piece
    private final int[][] boardState =  new int[8][8];

    public GameSession(ClientHandler player1, ClientHandler player2) {
        
    	this.player1 = player1;
        this.player2 = player2;

    }
    
    private boolean isPlayerPiece(int piece, int turn){

        if (turn == RED) {

            return (piece == RED || piece == RED_KING);

        }

        if (turn == BLACK) {

            return (piece == BLACK || piece == BLACK_KING);

        }

        return false;
        
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol){
    	
    	//Check starting coordinates
    	if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7) {
    	    return false;
    	}
        
    	//Check destination coordinates 
    	if (toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {

            return false;

        }

        if (boardState[toRow][toCol] != EMPTY) {

            return false;

        }

        int piece = boardState[fromRow][fromCol];

        int rowDiff = toRow - fromRow;

        int colDiff = Math.abs(toCol - fromCol);

        //checkers can only move onto dark squares

        if ((toRow + toCol) % 2 == 0) {

            return false;

        }

        if (colDiff != 1) {

            return false;

        }

        //kings move any direction, red moves up the board (negative change), black moves down (positive change)

        if (piece == RED_KING || piece == BLACK_KING) {

            return (Math.abs(rowDiff) == 1);

        } else if (piece == RED) {

            return (rowDiff == -1);

        } else if (piece == BLACK) {

            return (rowDiff == 1);

        }

        return false;

    }

    private boolean isValidJump(int fromRow, int fromCol, int toRow, int toCol) {
    	
    	//Check starting coordinates
    	if (fromRow < 0 || fromRow > 7 || fromCol < 0 || fromCol > 7) {
    	    return false;
    	}
        
    	//Check destination coordinates
    	if (toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {

            return false;

        }

        if (boardState[toRow][toCol] != EMPTY) {

            return false;

        }

        int piece = boardState[fromRow][fromCol];

        if (piece == EMPTY) {

            return false;

        }

        if ((toRow + toCol) % 2 == 0) {

            return false;

        }

        int rowDiff = toRow - fromRow;

        int colDiff = Math.abs(toCol - fromCol);

        //jumps must be exactly 2 squares out diagonally

        if (colDiff != 2) {

            return false;

        }

        //validate directional movement

        if (piece == RED && rowDiff != -2) {

            return false;

        }

        if (piece == BLACK && rowDiff != 2) {

            return false;

        }

        if ((piece == RED_KING || piece == BLACK_KING) && Math.abs(rowDiff) != 2) {

            return false;

        }

        //find coordinates of square being jumped over

        int middleRow = fromRow + (rowDiff / 2);

        int middleCol = fromCol + (toCol - fromCol) / 2;

        int middlePiece = boardState[middleRow][middleCol];

        //verify the piece being jumped is an enemy

        if (piece == RED || piece == RED_KING) {

            return (middlePiece == BLACK || middlePiece == BLACK_KING);

        } else if (piece == BLACK || piece == BLACK_KING){

            return (middlePiece == RED || middlePiece == RED_KING);

        }

        return false;

    }

    private void executeMove(int fromRow, int fromCol, int toRow, int toCol){

        int piece = boardState[fromRow][fromCol];

        //check for king promotion upon reaching board edge

        if (piece == RED && toRow == 0) {

            piece = RED_KING;

        }

        if (piece == BLACK && toRow == 7) {

            piece = BLACK_KING;

        }

        //update data structure

        boardState[toRow][toCol] = piece;

        boardState[fromRow][fromCol] = EMPTY;

        endTurn();

    }

    private void executeJump(int fromRow, int fromCol, int toRow, int toCol) {

        int piece = boardState[fromRow][fromCol];

        //check for king promotion

        if (piece == RED && toRow == 0) {

            piece = RED_KING;

        }

        if (piece == BLACK && toRow == 7) {

            piece = BLACK_KING;

        }

        //move piece to destination

        boardState[toRow][toCol] = piece;

        boardState[fromRow][fromCol] = EMPTY;

     // Calculate middle coordinates to delete captured piece
        
        int middleRow = fromRow + ((toRow - fromRow) / 2);
        
        int middleCol = fromCol + ((toCol - fromCol) / 2);

        boardState[middleRow][middleCol] = EMPTY;

        if (hasMoreJumps(toRow, toCol)) {

            isChainJumping = true;

            selectedRow = toRow;

            selectedCol = toCol;

        } else {

            endTurn();

        }

    }

    private boolean hasMoreJumps(int r, int c) {

        return isValidJump(r, c, r - 2, c - 2) || 

            isValidJump(r, c, r - 2, c + 2) || 

            isValidJump(r, c, r + 2, c - 2) || 

            isValidJump(r, c, r + 2, c + 2);

    }
    
    private void endTurn() {
        selectedRow = -1;
        selectedCol = -1;
        isChainJumping = false;

        // Check for a winner before changing turns
        if (checkWinCondition()) {
            return;
        }

        // Switch turns
        currentTurn = (currentTurn == RED) ? BLACK : RED;

        // Check whether the upcoming player has available moves
        if (!hasAvailableMoves(currentTurn)) {

            int nextTurn = (currentTurn == RED) ? BLACK : RED;

            // Check whether both players are unable to move
            if (!hasAvailableMoves(nextTurn)) {
            	
                gameOver = true;
                return;
                
            } 
            else {
            	
                currentTurn = nextTurn;
                
            }
        }
    }

    private boolean hasAvailableMoves(int player) {

        for (int r = 0; r < 8; r++) {

            for (int c = 0; c < 8; c++) {

                if (isPlayerPiece(boardState[r][c], player)) {

                    if (isValidMove(r, c, r - 1, c - 1) || isValidMove(r, c, r - 1, c + 1) ||

                        isValidMove(r, c, r + 1, c - 1) || isValidMove(r, c, r + 1, c + 1) ||

                        isValidJump(r, c, r - 2, c - 2) || isValidJump(r, c, r - 2, c + 2) ||

                        isValidJump(r, c, r + 2, c - 2) || isValidJump(r, c, r + 2, c + 2)) {

                        return true;

                    }
                    
                }
                
            }
            
        }
        
        return false;
        
    }	
    
    private void initializeBoard() {
    	//Initializes board so that red jumps up the board and black jumps down the board
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
            	//Ensures that pieces are only being placed on dark squares
                if ((r + c) % 2 == 1) {
                	//Fills rows 0-2 with black pieces
                    if (r < 3) {
                        boardState[r][c] = BLACK;
                    }
                    //Fills rows 5-7 with red pieces
                    else if (r > 4) {
                        boardState[r][c] = RED;
                    }
                    //Leaves the middle rows empty
                    else {
                        boardState[r][c] = EMPTY;
                    }

                }
                else {
                    boardState[r][c] = EMPTY;
                }
            }
        }
    }
    
    	
    	
    
    
    private boolean checkWinCondition() {
    	//Checks if one player has no remaining pieces, resulting in a loss
    	int redRemaining = 0;
    	int blackRemaining = 0;
    	
    	for(int r = 0; r < 8; r++) {
    		for(int c = 0; c < 8; c++) {
    		
    		int currentPiece = boardState[r][c];
    		
    		if(currentPiece == RED || currentPiece == RED_KING) {
    			
    			redRemaining++;
    		}
    		else if(currentPiece == BLACK || currentPiece == BLACK_KING) {
    			
    			blackRemaining++;
    			}
    		}
    	}
    	
    	if(redRemaining == 0 || blackRemaining == 0) {
    		gameOver = true;
    		return true;
    	
    	}
    	
    	return false;
    	
    }
    
    private int getPlayerColor(ClientHandler player) {
    	
    	if(player == player1) {
    		
    		return RED;
    	}
    	else if(player == player2) {
    		
    		return BLACK;
    	}
    	
    	return EMPTY;
    }
    
    //Copy board state to avoid sending the original 
    private int[][] copyBoard() {
        int[][] copy = new int[8][8];

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                copy[r][c] = boardState[r][c];
            }
        }

        return copy;
    }    
    
    //Send an error to a player attempting an invalid move
    private void invalidMove(ClientHandler player, String reason) {
    	
        GameMessage message = new GameMessage(GameMessage.INVALID_MOVE);
        //Give invalid move reason
        message.setMessage(reason);
        //Send invalid move message only to the player who made it
        player.sendMessage(message);
    }
    
    //Update board and turn order for both clients 
    private void updateBoard() {

        // Create a board update message
        GameMessage message = new GameMessage(GameMessage.BOARD_UPDATE);

        // Put current board state in a message
        message.setBoardState(copyBoard());

        // Attach whose turn it is that message
        message.setCurrentTurn(currentTurn);

        // Send the updated board and turn order to both players
        player1.sendMessage(message);
        player2.sendMessage(message);
    }
    
    //Assigns players and sends the starting board
    public synchronized void startGame() {

        //Reset game state
        gameOver = false;
        currentTurn = RED;
        isChainJumping = false;
        selectedRow = -1;
        selectedCol = -1;

        //Initialize the board
        initializeBoard();

        //Create player 1 assignment message
        GameMessage player1Message = new GameMessage(GameMessage.ASSIGN_PLAYER);
        player1Message.setPlayerNumber(1);
        
      //Create player 2 assignment message
        GameMessage player2Message = new GameMessage(GameMessage.ASSIGN_PLAYER);
        player2Message.setPlayerNumber(2);

        //Send player assignment messages to both players
        player1.sendMessage(player1Message);
        player2.sendMessage(player2Message);

        //Notify both players that the game has started
        GameMessage startMessage = new GameMessage(GameMessage.GAME_START);
        startMessage.setCurrentTurn(currentTurn);

        player1.sendMessage(startMessage);
        player2.sendMessage(startMessage);

        //Send the initial board state to both players
        updateBoard();

        System.out.println("Game started! Player 1 is Red. Player 2 is Black.");
    }
    
    //Receives and validates move requests then executes it
    public synchronized void processMove(GameMessage move, ClientHandler sender) {

        // Variables
        int playerColor;
        int fromRow;
        int fromCol;
        int toRow;
        int toCol;
        int rowDistance;
        
        // Check if message is null
        if (move == null) {
            return;
        }

        //Check if the game is over
        if (gameOver) {
            invalidMove(sender, "The game is already over.");
            return;
        }

        //Check if the sender is part of this game
        if (sender != player1 && sender != player2) {
            return;
        }

        //Determine the player's color
        playerColor = getPlayerColor(sender);

        //Check if it is the player's turn, if not send invalid move message
        if (playerColor != currentTurn) {
            invalidMove(sender, "It is not your turn.");
            return;
        }

        //Get the move coordinates
        fromRow = move.getFromRow();
        fromCol = move.getFromColumn();
        toRow = move.getToRow();
        toCol = move.getToColumn();

        //Check if the coordinates are within the board
        if (fromRow < 0 || fromRow > 7 ||
            fromCol < 0 || fromCol > 7 ||
            toRow < 0 || toRow > 7 ||
            toCol < 0 || toCol > 7) {

            invalidMove(sender, "Coordinates are outside the board.");
            return;
        }

        //Check if the starting square contains the player's piece
        if (!isPlayerPiece(boardState[fromRow][fromCol], playerColor)) {
        	
            invalidMove(sender, "You cannot move that piece.");           
            return;
        }

        //Calculate the row distance
        rowDistance = Math.abs(toRow - fromRow);

        //Check if the player is currently chain jumping
        if (isChainJumping) {

            //The player must continue with the same piece
            if (fromRow != selectedRow || fromCol != selectedCol) {
            	
                invalidMove(sender, "You must continue jumping with the same piece.");               
                return;
            }

            //Check if the next jump is valid
            if (!isValidJump(fromRow, fromCol, toRow, toCol)) {
            	
                invalidMove(sender, "Invalid chain jump.");               
                return;
            }

            //Execute the jump
            executeJump(fromRow, fromCol, toRow, toCol);
        }

        //Handle normal moves and jumps
        else {

            //Handle a non-capture move
            if (rowDistance == 1) {

                if (!isValidMove(fromRow, fromCol, toRow, toCol)) {
                	
                    invalidMove(sender, "Invalid move.");                    
                    return;
                }

                executeMove(fromRow, fromCol, toRow, toCol);
            }

            //Handle a capture jump
            else if (rowDistance == 2) {

                if (!isValidJump(fromRow, fromCol, toRow, toCol)) {
                	
                    invalidMove(sender, "Invalid jump.");                    
                    return;
                }

                executeJump(fromRow, fromCol, toRow, toCol);
            }

            //Handles invalid movement distance
            else {
            	
                invalidMove(sender, "Invalid movement distance.");                
                return;
            }
        }

        //Update both clients with the new board state
        updateBoard();

        //Check whether the game has ended
        if (gameOver) {
            notifyGameOver();
        }
    }
    
    //Sends game over to both clients only when game ends
    private void notifyGameOver() {

        //Create game over message
        GameMessage message = new GameMessage(GameMessage.GAME_OVER);
        
        message.setIsGameOver(true);
        
        message.setCurrentTurn(currentTurn);
        
        message.setMessage("Game over!");

        //Send message to both players
        player1.sendMessage(message);
        
        player2.sendMessage(message);
        
        //CONSOLE MESSAGE STRICTLY FOR TESTING
        System.out.println("Game over!");
    }
    
    //Handle a player disconnect 
    public synchronized void handlePlayerDisconnect(ClientHandler player) {

        //Check if game is already over
        if (gameOver) {        	
            return;
            
        }

        //Verify player belongs to this game
        if (player != player1 && player != player2) {
        	
            return;
            
        }
        //End the game
        gameOver = true;

        //Determine which player disconnected
        String disconnectedPlayer;

        if (player == player1) {
        	
            disconnectedPlayer = "Red";
            
        } 
        else {
        	
            disconnectedPlayer = "Black";
        }

        //Create disconnect message
        GameMessage message = new GameMessage(GameMessage.OPPONENT_LEFT);
        
        message.setIsGameOver(true);
        
        message.setMessage(disconnectedPlayer + " player disconnected. The game has ended.");

        //Notify both players
        player1.sendMessage(message);
        
        player2.sendMessage(message);

        //CONSOLE MESSAGE STRICTLY FOR TESTING 
        System.out.println(disconnectedPlayer + " player disconnected. Game ended.");
    }
}
    
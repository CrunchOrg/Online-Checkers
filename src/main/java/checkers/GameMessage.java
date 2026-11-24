package checkers;

import com.google.gson.Gson;

/**
 * GameMessage is the one object sent back and forth between client and server.
 *
 * Every message is converted to a single line of JSON with toJson(), sent over
 * the socket with println(), read with readLine(), and turned back into a
 * GameMessage with fromJson().
 *
 * 
 * Sending:    out.println(msg.toJson());
 * Receiving:  GameMessage msg = GameMessage.fromJson(in.readLine());
 * 
 *
 * How a message works: the action field says what kind of message it is
 * (one of the constants below). Only the fields that action uses are filled in;
 * the rest keep their defaults. Each constant's comment lists the fields it uses.
 *
 * The server decides everything. Clients only send requests (CONNECT, MOVE,
 * RESET, DISCONNECT). The server validates every move and sends back the official
 * board. Clients never send a board, and they redraw only what the server sends.
 *
 * Both sides must use this exact same file. Gson matches JSON keys to field
 * names, so renaming a field on one side silently breaks it on the other.
 */

public class GameMessage {
	//-------------CONSTANTS--------------------
	//----- Client -> Server -----
	/* Player joins the server. Uses username. */
	public static final String CONNECT = "CONNECT";
	/* Player Player requests a move or jump. Uses username, fromRow, fromColumn, toRow, toColumn */
	public static final String MOVE = "MOVE";
	/* Player asks to start a new game. No other fields */
	public static final String RESET = "RESET";
	/* Player is leaving the game. No other fields */
	public static final String DISCONNECT = "DISCONNECT";
	
	//----- Server -> Client ------
	/* Connected, but waiting for an opponent, Uses message (optional) */
	public static final String WAITING = "WAITING";
	/* Tells the client which player it is. Uses playerNumber */
	public static final String ASSIGN_PLAYER = "ASSIGN_PLAYER";
	/* Both players connect; game begins. Users boardState, currentTurn */
	public static final String GAME_START = "GAME_START";
	/* Board changed after a valid move. Uses boardState, currentTurn, message (optional) */
	public static final String BOARD_UPDATE = "BOARD_UPDATE";
	/* Move was rejected. Sent only to the player who made it. Uses message (the reason) */
	public static final String INVALID_MOVE = "INVALID_MOVE";
	/* A player had no legal moves and was skipped. Uses currentTurn, message */
	public static final String SKIP_TURN = "SKIP_TURN";
	/* Game has ended. Uses boardState, message (the result), isGameOver = true */
	public static final String GAME_OVER = "GAME_OVER";
	/* The other player disconnected. Uses message (optional) */
	public static final String OPPONENT_LEFT = "OPPONENT_LEFT";
	/* Something went wrong that isn't an invalid move. Uses message */
	public static final String ERROR = "ERROR";

	// ----- Other -----
	/* Value for a row or column field that isn't being used. -1, because 0 is a real square. */
	public static final int NO_POSITION = -1;
	/* Shared converter for all messages. (thread safe)*/
	private static final Gson GSON = new Gson();

	//--------------FIELDS----------------------
	/* What kind of message this is. One of the actions constants above */
	private String action = null;
	/* Name of the player sending the message. Set by the client on CONNECT and MOVE */
	private String username = null;
	
	/* Move coordinates, used only on MOVE. Row 9 is the top of the board.
	 * NO_POSITOIN (-1) means the field isn't used */
	private int fromRow = NO_POSITION;
	private int fromColumn = NO_POSITION;
	private int toRow = NO_POSITION;
	private int toColumn = NO_POSITION;
	
	/* The full 8x8 board, as a boardState[row][col]. Row 0 is the top.
	 * Sent by the server only; clients never send a board.
	 * Piece codes: 0 = empty, 1 = red, 2 = black, 3 = red king, 4 = black king
	 * Note:Let me know if you would prefer to change this.
	 */
	private int[][] boardState = null;
	/* Whose turn it is. Uses the same numbers as playerNumber */
	private int currentTurn = 0;
	/* Test to show the player: the reason for an INVALID_MOVE, the result on GAME_OVER, as examples */
	private String message = null;
	/* True only on GAME_OVER. Must be set with setIsGameOver(true); no constructor setting */
	private boolean isGameOver = false;
	/* Which player this client is. Sent once on ASSIGN_PLAYER. 0 means not assigned yet. */
	private int playerNumber = 0;

//-------------CONSTRUCTORS-------------------------------
	
	/**
	 * Empty constructor. Required by Gson: fromJson() uses it to build the object,
	 * which is what applies the field defaults above (like NO_POSITION).
	 * Do not remove, even though it is likely nothing else in the project will call it.
	 */
	public GameMessage() {
	}

	/**
	 * Message with only an action, for messages that carry no other data.
	 * Example: new GameMessage(GameMessage.WAITING)
	 */
	public GameMessage(String action) {
		this.action = action;
	}

	/**
	 * Message with an action and text to show the player.
	 * Use for INVALID_MOVE, ERROR, OPPONENT_LEFT, and GAME_OVER.
	 * For GAME_OVER, also call setIsGameOver(true).
	 * Example: new GameMessage(GameMessage.INVALID_MOVE, "Not your turn")
	 */
	public GameMessage(String action, String message) {
		this.action = action;
		this.message = message;
	}
	
	/**
	 * Move request sent by the client.
	 * Example: new GameMessage(GameMessage.MOVE, "Angelo", 5, 0, 4, 1)
	 */
	public GameMessage(String action, String username, int fromRow, int fromColumn, int toRow, int toColumn) {
		this.action = action;
		this.username = username;
		this.fromRow = fromRow;
		this.fromColumn = fromColumn;
		this.toRow = toRow;
		this.toColumn = toColumn;
	}

	/**
	 * Sets every field at once. Mostly useful for testing.
	 * For real messages, prefer a shorter constructor plus setters,
	 * so it's clear which fields that message uses.
	 */
	public GameMessage(String action, String username, int fromRow, int fromColumn, int toRow, int toColumn,
			int[][] boardState, int currentTurn, String message, boolean isGameOver, int playerNumber) {
		this.action = action;
		this.username = username;
		this.fromRow = fromRow;
		this.fromColumn = fromColumn;
		this.toRow = toRow;
		this.toColumn = toColumn;
		this.boardState = boardState;
		this.currentTurn = currentTurn;
		this.message = message;
		this.isGameOver = isGameOver;
		this.playerNumber = playerNumber;
	}
	
	
	//-------------METHODS-----------------------
	/**
	 * Converts this message to JSON so it can be sent over the socket.
	 * The result is always a single line, so send it with println()
	 * and read it with readLine() on the other side.
	 *
	 * Example: out.println(msg.toJson());
	 *
	 * @return this message as one line of JSON
	 */
	public String toJson() {
		return GSON.toJson(this);
	}

	/**
	 * Converts one line of JSON, received from the socket, back into a GameMessage.
	 * Fields missing from the JSON keep their defaults (e.g. NO_POSITION).
	 *
	 * Example: GameMessage msg = GameMessage.fromJson(in.readLine());
	 *
	 * Returns null if json is null, which happens when readLine() hits a
	 * disconnected socket, so check for null before using the result.
	 * Throws com.google.gson.JsonSyntaxException if the text isn't valid JSON.
	 *
	 * @param json one line of JSON produced by toJson()
	 * @return the message, or null if json was null
	 */
	public static GameMessage fromJson(String json) {
		return GSON.fromJson(json, GameMessage.class);
	}

	// ----------GETTERS----------------------------
	public String getAction() {
		return this.action;
	}

	public String getUsername() {
		return this.username;
	}

	public int getFromRow() {
		return this.fromRow;
	}

	public int getFromColumn() {
		return this.fromColumn;
	}

	public int getToRow() {
		return this.toRow;
	}

	public int getToColumn() {
		return this.toColumn;
	}

	public int[][] getBoardState() {
		return this.boardState;
	}

	public int getCurrentTurn() {
		return this.currentTurn;
	}

	public String getMessage() {
		return this.message;
	}

	public boolean getIsGameOver() {
		return this.isGameOver;
	}

	public int getPlayerNumber() {
		return this.playerNumber;
	}

	// -------------------SETTERS-------------------------------
	public void setAction(String action) {
		this.action = action;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setFromRow(int fromRow) {
		this.fromRow = fromRow;
	}

	public void setFromColumn(int fromColumn) {
		this.fromColumn = fromColumn;
	}

	public void setToRow(int toRow) {
		this.toRow = toRow;
	}

	public void setToColumn(int toColumn) {
		this.toColumn = toColumn;
	}

	public void setBoardState(int[][] boardState) {
		this.boardState = boardState;
	}

	public void setCurrentTurn(int currentTurn) {
		this.currentTurn = currentTurn;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setIsGameOver(boolean isGameOver) {
		this.isGameOver = isGameOver;
	}

	public void setPlayerNumber(int playerNumber) {
		this.playerNumber = playerNumber;
	}

	@Override
	public String toString() {
		return toJson();
	}
}

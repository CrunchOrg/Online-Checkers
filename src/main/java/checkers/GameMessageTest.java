package checkers;

import java.util.Arrays;

public class GameMessageTest {
	
	private static int failures = 0;
	
	public static void main(String[] args) {
		testMoveRoundTrip();
		testBoardRoundTrip();
		testGameOver();
		testDefaultsFromPartialJson();
		testJsonIsOneLine();
		testEveryConstructor();
		
		System.out.println(failures == 0
				? "\nAll tests passed."
				: "\n" + failures +" tests(s) FAILED.");
	}
	
	// TEST DATA
    private static int[][] startingBoard() {
        return new int[][] {
            {0, 2, 0, 2, 0, 2, 0, 2},
            {2, 0, 2, 0, 2, 0, 2, 0},
            {0, 2, 0, 2, 0, 2, 0, 2},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 1, 0, 1, 0, 1, 0},
            {0, 1, 0, 1, 0, 1, 0, 1},
            {1, 0, 1, 0, 1, 0, 1, 0}
        };
    }
	
	// TEST METHODS
	
	private static void testMoveRoundTrip() {
		System.out.println("--- testMoveRoundTrip ---");
		GameMessage original = new GameMessage(GameMessage.MOVE, "Stinky", 5, 0, 4, 1);
		GameMessage back = GameMessage.fromJson(original.toJson());
		
		check("move: action", GameMessage.MOVE.equals(back.getAction()));
		check("username", "Stinky".equals(back.getUsername()));
		check("move: fromRow", back.getFromRow() == 5);
		check("move: fromColumn", back.getFromColumn() == 0);
		check("move: toRow", back.getToRow() == 4);
		check("move: toColumn", back.getToColumn() == 1);
		check("no board on a move", back.getBoardState() == null);
	}
	
	private static void testBoardRoundTrip() {
		System.out.println("--- testBoardRoundTrip ---");
		int[][] board = startingBoard();
		// Example move:
		board[5][0] = 0; // red moved from (5,0) -->
		board[4][1] = 1; // --> to ((4,1)
		
		GameMessage original = new GameMessage(GameMessage.BOARD_UPDATE);
		original.setBoardState(board);
		original.setCurrentTurn(2);
		original.setMessage("Black's turn");
		
		GameMessage back = GameMessage.fromJson(original.toJson());
		
		check("action", GameMessage.BOARD_UPDATE.equals(back.getAction()));
		check("board matches", Arrays.deepEquals(board, back.getBoardState()));
		check("moved piece at (4,1)", back.getBoardState()[4][1] == 1);
		check("old square (5,0) empty", back.getBoardState()[5][0] == 0);
		check("currentTurn", back.getCurrentTurn() == 2);
		check("message", "Black's turn".equals(back.getMessage()));
	}
	
	private static void testGameOver() {
        System.out.println("--- testGameOver ---");
        GameMessage original = new GameMessage(GameMessage.GAME_OVER, "Red wins!");
        original.setIsGameOver(true);

        GameMessage back = GameMessage.fromJson(original.toJson());

        check("action", GameMessage.GAME_OVER.equals(back.getAction()));
        check("isGameOver is true", back.getIsGameOver());
        check("message", "Red wins!".equals(back.getMessage()));
    }
	
	private static void testDefaultsFromPartialJson() {
        System.out.println("--- testDefaultsFromPartialJson ---");
        GameMessage msg = GameMessage.fromJson("{\"action\":\"WAITING\"}");

        check("action", GameMessage.WAITING.equals(msg.getAction()));
        check("fromRow is NO_POSITION", msg.getFromRow() == GameMessage.NO_POSITION);
        check("fromColumn is NO_POSITION", msg.getFromColumn() == GameMessage.NO_POSITION);
        check("toRow is NO_POSITION", msg.getToRow() == GameMessage.NO_POSITION);
        check("toColumn is NO_POSITION", msg.getToColumn() == GameMessage.NO_POSITION);
        check("board is null", msg.getBoardState() == null);
        check("isGameOver is false", !msg.getIsGameOver());
    }
	
	private static void testJsonIsOneLine() {
        System.out.println("--- testJsonIsOneLine ---");
        GameMessage msg = new GameMessage(GameMessage.BOARD_UPDATE, "Board\nupdated");
        msg.setBoardState(startingBoard());
        String json = msg.toJson();

        check("no line breaks in JSON", !json.contains("\n") && !json.contains("\r"));
        check("newline inside text survives",
                "Board\nupdated".equals(GameMessage.fromJson(json).getMessage()));
    }
	
	private static void testEveryConstructor() {
        System.out.println("--- testEveryConstructor ---");
        GameMessage[] messages = {
            new GameMessage(),
            new GameMessage(GameMessage.WAITING),
            new GameMessage(GameMessage.INVALID_MOVE, "Not your turn"),
            new GameMessage(GameMessage.MOVE, "Stinky", 2, 1, 3, 2),
            new GameMessage(GameMessage.BOARD_UPDATE, "Moron", 5, 0, 4, 1,
                    startingBoard(), 1, "Red's turn", false, 1)
        };
        
        for (GameMessage original : messages) {
            String json = original.toJson();
            GameMessage back = GameMessage.fromJson(json);
            check("round trip: " + original.getAction(), json.equals(back.toJson()));
        }
    }
	
	//Helper Functions
	
	private static void check(String name, boolean passed) {
		System.out.println((passed ? "PASS " : "FAIL ") + name);
		if (!passed) failures++;
	}
	
	
}


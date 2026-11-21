# Architecture

Rogue Node Checkers is a client-server system. One server process hosts any number of two-player games; each player runs a client process on their own machine. All communication is over TCP sockets on port 1234.

## Components

```
                    SERVER
                      |
         +------------+------------+
         |                         |
    GameSession 1             GameSession 2
         |                         |
    +----+----+               +----+----+
    |         |               |         |
ClientHandler ClientHandler ClientHandler ClientHandler
    |         |               |         |
  Client    Client          Client    Client
    |         |               |         |
GameBoard  GameBoard       GameBoard  GameBoard
    |         |               |         |
BoardSquare BoardSquare   BoardSquare BoardSquare
```

| Class | Runs on | Responsibility |
| --- | --- | --- |
| `Server` | server | Opens the `ServerSocket` on port 1234, accepts connections, pairs every two clients, and creates a `GameSession` for each pair |
| `ClientHandler` | server | One per connected client. Reads that client's messages from its socket, forwards move requests to its `GameSession`, writes messages back, and handles disconnects |
| `GameSession` | server | One game. Owns the authoritative 8x8 board, the current turn, and the rules: move and jump validation, chain jumps, king promotion, win and stalemate detection. Sends the updated board to both players |
| `GameMessage` | both | The message format, converted to and from JSON with Gson (see [protocol.md](protocol.md)) |
| `Client` | client | Connects to the server, listens for server messages, sends move requests, and passes board updates to the GUI |
| `GameBoard` | client | The Swing window: the 8x8 grid of squares, the turn label, and game-over dialogs. Displays the board the server sends |
| `BoardSquare` | client | One square: draws the checker and king crown, highlights the selection, and turns clicks into move requests |
| `checkers` (in `org.Checkers`) | — | The original single-machine game. Its logic is being moved into the classes above, and it is kept until the networked version works |

## Design principles

- **The server is the authority.** It validates and executes every move. Clients send move *requests*; they never change the official board themselves.
- **Each `GameSession` has its own board and game state.** Nothing about a game is `static`, so games cannot interfere with one another.
- **One `ClientHandler` per client**, reading only from its own socket.
- **The GUI only displays.** `GameBoard` and `BoardSquare` show whatever board the server last sent.
- **Every Swing update runs on the Event Dispatch Thread** (`SwingUtilities.invokeLater`), never on the network listener thread.

## Threads

| Thread | Where | Count | Does |
| --- | --- | --- | --- |
| main | server | 1 | Accepts connections and pairs players |
| `ClientHandler` | server | 1 per client | Blocks on its socket's input and forwards each message to its game |
| `GameSession` | server | 1 per game | Runs that game |
| listener | client | 1 | Blocks on the socket and hands each server message to the GUI |
| Event Dispatch Thread | client | 1 | Handles clicks and repaints the board |

Two `ClientHandler` threads can deliver moves to the same `GameSession` at the same moment, so the session processes moves one at a time (a `synchronized` method or lock around move processing). That is what keeps both players seeing the same board.

## Distributed-systems concepts

| Concept | Where it shows up |
| --- | --- |
| Client-server architecture | Independent client processes on separate machines talk only to the server |
| Application-layer protocol | The JSON `GameMessage` format the team designed (see [protocol.md](protocol.md)) |
| Centralized authoritative state | Only `GameSession` decides what happened, so the two clients can never disagree about the board |
| Concurrency | A thread per client and per game; several games run at once on one server |
| Synchronization | Moves into a `GameSession` are processed one at a time, in order |
| Failure handling | A dropped connection is detected by its `ClientHandler`, and the opponent is told the game ended |

## Why TCP

Checkers is turn-based. Every move must arrive, exactly once and in order, or the boards go out of sync. TCP guarantees that; UDP would only make the game faster by an amount nobody would notice, at the cost of rebuilding reliability by hand.

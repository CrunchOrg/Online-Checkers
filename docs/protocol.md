# Protocol

Client and server exchange `GameMessage` objects (Phase 1, by Angelo) as JSON, **one message per line**, over the TCP connection on port 1234.

```java
out.println(msg.toJson());                          // send
GameMessage msg = GameMessage.fromJson(in.readLine()); // receive
```

The **server decides everything.** Clients only send requests (`CONNECT`, `MOVE`, `RESET`, `DISCONNECT`). The server validates every move and sends back the official board. Clients never send a board, and they redraw only what the server sends.

Both sides must use the exact same `GameMessage` class: Gson matches JSON keys to field names, so renaming a field on one side silently breaks the other.

## Fields

Only the fields a message's `action` uses are filled in; the rest keep their defaults.

| Field | Type | Default | Meaning |
| --- | --- | --- | --- |
| `action` | String | `null` | The message type (see below) |
| `username` | String | `null` | The sending player's name. Set by the client on `CONNECT` and `MOVE` |
| `fromRow`, `fromColumn` | int | `-1` | Square the piece moves from. Used only on `MOVE` |
| `toRow`, `toColumn` | int | `-1` | Square the piece moves to. Used only on `MOVE` |
| `boardState` | int[8][8] | `null` | The full board, `boardState[row][col]`. Server only |
| `currentTurn` | int | `0` | Whose turn it is, using the same numbers as `playerNumber` |
| `message` | String | `null` | Text for the player: the reason for an `INVALID_MOVE`, the result on `GAME_OVER` |
| `isGameOver` | boolean | `false` | True only on `GAME_OVER` (set with `setIsGameOver(true)`) |
| `playerNumber` | int | `0` | Which player this client is, sent once on `ASSIGN_PLAYER`. `0` = not assigned yet |

`-1` (`GameMessage.NO_POSITION`) marks an unused row or column, because `0` is a real square.

## Board

- `boardState[row][col]`, 8x8, **row 0 is the top** of the board.
- Piece codes (agreed 11/18): `0` empty, `1` red, `2` black, `3` red king, `4` black king.

## Message types

### Client → server

| `action` | Sent when | Fields used |
| --- | --- | --- |
| `CONNECT` | The player joins the server | `username` |
| `MOVE` | The player requests a move or jump | `username`, `fromRow`, `fromColumn`, `toRow`, `toColumn` |
| `RESET` | The player asks to start a new game | none |
| `DISCONNECT` | The player is leaving | none |

### Server → client

| `action` | Sent when | Fields used |
| --- | --- | --- |
| `WAITING` | Connected, waiting for an opponent | `message` (optional) |
| `ASSIGN_PLAYER` | Tells the client which player it is | `playerNumber` |
| `GAME_START` | Both players are connected; the game begins | `boardState`, `currentTurn` |
| `BOARD_UPDATE` | A valid move changed the board (sent to both players) | `boardState`, `currentTurn`, `message` (optional) |
| `INVALID_MOVE` | A move was rejected (sent only to the player who made it) | `message` (the reason) |
| `SKIP_TURN` | A player had no legal moves and was skipped | `currentTurn`, `message` |
| `GAME_OVER` | The game ended | `boardState`, `message` (the result), `isGameOver = true` |
| `OPPONENT_LEFT` | The other player disconnected | `message` (optional) |
| `ERROR` | Something went wrong that isn't an invalid move | `message` |

## Example

A move request:

```json
{"action":"MOVE","username":"Angelo","fromRow":5,"fromColumn":0,"toRow":4,"toColumn":1,"currentTurn":0,"isGameOver":false,"playerNumber":0}
```

## A game, step by step

1. Each client sends `CONNECT` with its username.
2. The first player gets `WAITING`. When a second player connects, the server pairs them into a `GameSession`.
3. Each client gets `ASSIGN_PLAYER` with its `playerNumber`, then both get `GAME_START` with the starting board.
4. The active player clicks a piece and a destination; the client sends `MOVE`.
5. `GameSession` checks that it is that player's turn, that the piece is theirs, and that the move or jump is legal.
6. Illegal: `INVALID_MOVE` goes to that player only, and the board does not change.
7. Legal: the board is updated (captures and king promotion included), and `BOARD_UPDATE` goes to both players. After a jump that can continue, the same player moves again; otherwise the turn switches.
8. A player with no legal moves gets skipped with `SKIP_TURN`.
9. When the game ends, both players get `GAME_OVER`. If a player disconnects, the other gets `OPPONENT_LEFT`.

## Still to decide

- Which number is red and which is black for `playerNumber` / `currentTurn` (proposed: `1` = red, `2` = black), and which color moves first.

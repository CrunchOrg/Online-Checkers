# Rogue Node Checkers

A networked game of checkers for CS 3700 (Networking & Distributed Computing), Fall 2026, MSU Denver.

Two players run a Java Swing client on their own machines and connect over TCP to a self-built game server. The server is the **authority**: it holds the official board, decides whose turn it is, validates every move, and sends the updated board to both players. Clients only display the board and send move requests.

**Team: The Nodes**

| Member | GitHub |
| --- | --- |
| Angelo Adducci | `CrunchAstro` |
| Juan De La Garza | `jdelagar` |
| Rylin Quintana | `QQuintana` |
| Audrey Western | `audreyw9785` |

## Requirements

- **JDK 22**
- **Maven 3.9+** (Eclipse ships with Maven support; in VS Code, use the Extension Pack for Java)
- Git

Maven downloads the only library dependency (Gson, for JSON messages) automatically.

## Build

From the repository root:

```
mvn compile
```

Run the tests:

```
mvn test
```

## Run

The server and the clients run on the same machine for the demo (the server listens on port **1234**).

1. Start the server in one terminal:

   ```
   mvn compile exec:java@server
   ```

2. Start a client in a second terminal, and a second client in a third:

   ```
   mvn exec:java@client
   ```

The server pairs every two clients that connect into their own game session, so a fifth and sixth client start a separate game.

**From Eclipse:** File > Import > Maven > Existing Maven Projects, select this folder, then right-click `Server.java` > Run As > Java Application, and do the same for `Client.java` (once per player).

## How it works

```
Player 1 Client <--(TCP)--> Game Server <--(TCP)--> Player 2 Client
```

1. Two players connect to the server.
2. The server creates a game session and assigns each client a player (red or black).
3. The server sends the starting board to both clients.
4. The active player clicks a move; the client sends the move request to the server.
5. The server validates it. An illegal move is rejected and the player is asked for another.
6. A legal move updates the server's board, the new board goes to both clients, and the turn switches.
7. This repeats until the server detects that the game is over.

TCP is used instead of UDP because a turn-based game needs every move delivered, in order; speed matters much less than an accurate game state.

More detail:

- [docs/architecture.md](docs/architecture.md): the classes, the threads, and the distributed-systems concepts the project demonstrates
- [docs/protocol.md](docs/protocol.md): the JSON messages exchanged between client and server

## Project structure

| Path | What |
| --- | --- |
| `src/main/java/org/example/Server.java` | Accepts connections on port 1234, pairs players, starts a `GameSession` per pair |
| `src/main/java/org/example/ClientHandler.java` | One per connected client, on its own thread: reads and writes that client's messages |
| `src/main/java/org/example/GameSession.java` | One game: the authoritative board, turns, move validation, captures, kings, win detection |
| `src/main/java/org/example/GameMessage.java` | The message format, serialized to JSON with Gson |
| `src/main/java/org/example/Client.java` | Connects to the server, listens for messages, sends move requests |
| `src/main/java/org/example/GameBoard.java` | The client's Swing window and 8x8 board |
| `src/main/java/org/Checkers/checkers.java` | The original single-machine game, kept as a reference until its code has moved into the classes above |
| `src/test/java/` | Unit tests for each phase's testing checkpoint (added as each phase lands) |
| `docs/` | Architecture and protocol documentation |
| `pom.xml` | Maven build: Java 22, Gson, JUnit 5, and the `server`/`client` run targets |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branches, commit messages, and pull requests.

## Submitting

The final project is submitted as a Git bundle of the whole repository, with its history:

```
git bundle create team_the_nodes.bundle --all
git bundle verify team_the_nodes.bundle
```

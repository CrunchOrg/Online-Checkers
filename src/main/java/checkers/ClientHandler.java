package checkers;

import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private GameSession gameSession;
    private final List<ClientHandler> activeHandlers;
    private String username;

    /* creates handler for a connected client
    * @param socket             the clients socket connection
    * @param activeHandlers     shared list of active handlers on the server
    */
    public ClientHandler(Socket socket, List<ClientHandler> activeHandlers) {
        this.socket = socket;
        this.activeHandlers = activeHandlers;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            closeEverything();
        }
    }

    //overloaded constructor if server tracks activeHandlers outside of ClientHandler
    public ClientHandler(Socket socket) {
        this(socket, null);
    }

    //assigns this client handler to an active GameSession
    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }

    public GameSession getGameSession() {
        return gameSession;
    }

    public String getUsername() {
        return username;
    }

    //continuously listens for incoming JSON messages from this client's socket
    @Override 
    public void run() {
        try {
            while (socket != null && !socket.isClosed()) {
                GameMessage incoming = receiveMessage();
                if (incoming == null) {
                    // stream reacher EOF (client disconnected cleanly)
                    break;
                }
                handleMessage(incoming);

                if (GameMessage.DISCONNECT.equals(incoming.getAction())); {
                    break;
                }
            }
        } finally {
            closeEverything();
        }
    }

    //convert GameMessage to JSON via toJson() and sends it to the connected client
    public synchronized void sendMessage(GameMessage message) {
        if (message == null || socket == null || socket.isClosed() || out == null) {
            return;
        }
       out.println(message.toJson());
       if (out.checkError()) {
        closeEverything();
       }
    }

    // reads a single line of JSON from this client's socket and converts it via GameMessage.fromJson()
    public GameMessage receiveMessage() {
        if (in == null) {
            return null;
        }
        try {
            String line = in.readLine();
            return GameMessage.fromJson(line);
        } catch (JsonSyntaxException e) {
            sendMessage(new GameMessage(GameMessage.ERROR, "Invalid JSON format recieved."));
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    //processes incoming messages and forwards them to the assigned GameSession
    public void handleMessage(GameMessage message) {
        if (message == null || message.getAction() == null) {
            return;
        }

        // capture username when the client sends CONNECT
        if (GameMessage.CONNECT.equals(message.getAction()) && message.getUsername() != null) {
            this.username = message.getUsername();
        }
        
        if (gameSession!= null) {
            gameSession.handleClientMessage(this, message);
        } else {
            sendMessage(new GameMessage(GameMessage.WAITING, "Connected to server. Waiting for a game session..."));
        }
    }

    //removes this handler from any shared connection list, notifies the session, and closes all I/O streams and the socket
    public void closeEverything() {
        if (activeHandlers != null) {
            synchronized (activeHandlers) {
                activeHandlers.remove(this);
            }
        }

        if (gameSession != null) {
            gameSession.handlePlayerDisconnect(this);
            gameSession = null;
        }

        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing client resources: " + e.getMessage());
        }
    }
}

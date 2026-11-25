
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class Server {

    //Server variables
    private ServerSocket serverSocket;
    private ArrayList<ClientHandler> clients;
    private Queue<ClientHandler> waitingPlayers;
    private boolean running;

    //Port number
    private static final int PORT = 1234;


    //Constructor
    public Server() {
    	
        clients = new ArrayList<>();
        waitingPlayers = new LinkedList<>();        
        running = false;
        
    }


    
    //Starts the server and begins listening for client connections
    public void startServer() {

        try {
            //Create server socket using the port above
            serverSocket = new ServerSocket(PORT);

            //Server as running
            running = true;

            System.out.println("Server started on port " + PORT);

            //Begin accepting clients
            acceptConnections();

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }


 //Continuously accepts incoming client connections
    private void acceptConnections() {

        while (running) {
            try {
                //Wait for a client to connect
                Socket socket = serverSocket.accept();

                //Handle the newly connected client
                handleClient(socket);

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        }
    }


    //Handles a newly connected client
    private void handleClient(Socket socket) {

        //Create a ClientHandler for the connected client
        ClientHandler client = new ClientHandler(socket, clients);

        //Add client to the active client list
        synchronized (clients) {
            clients.add(client);
        }

        //Start the client's communication thread
        Thread clientThread = new Thread(client);
        clientThread.start();

        //Add client to the waiting queue
        synchronized (waitingPlayers) {
            waitingPlayers.add(client);
        }

        //Call pairPlayers to check if two players are in queue
        pairPlayers();
    }


    //Pairs waiting players together when two are available
    private synchronized void pairPlayers() {

        //Continuously checks if at least two players are waiting
        while (waitingPlayers.size() >= 2) {

            //Remove the first two players from the queue and assign them to a player
            ClientHandler player1 = waitingPlayers.poll();
            ClientHandler player2 = waitingPlayers.poll();

            //Call createGameSession for the two players
            createGameSession(player1, player2);
        }
    }


    //Creates a new game session
    private void createGameSession(ClientHandler player1, ClientHandler player2) {

        //Create a new GameSession

        //Associate the GameSession with both ClientHandlers

        //Start the game

    }


    //Removes a disconnected client
    public synchronized void removeClient(ClientHandler client) {

        //Remove client from active clients

        //Remove client from waiting player queue

        //Handle any necessary cleanup

    }


    //Stops the server
    public void stopServer() {

        //Set running to false

        //Close server socket

        //Close active client connections

        //Clear client collections

    }

}

package checkers;

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

            //Begin accepting incoming connections
            acceptConnections();

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }


    //Accepts incoming client connections
    private void acceptConnections() {

        //Continue accepting connections while server is running

        //Accept incoming socket

        //Pass socket to handleClient()

    }


    //Handles a newly connected client
    private void handleClient(Socket socket) {

        //Create a ClientHandler for the socket

        //Add ClientHandler to active clients

        //Start ClientHandler thread

        //Add client to waiting player queue

        //Attempt to pair players

    }


    //Pairs two waiting players together
    private synchronized void pairPlayers() {

        //Check if at least two players are waiting

        //Remove first waiting player

        //Remove second waiting player

        //Create a GameSession for both players

        //Start the GameSession

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

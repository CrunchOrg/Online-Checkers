package checkers;

import java.io.*;
import java.net.Socket;
import javax.swing.*;


public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private GameBoard gameBoard;

    public Client(String serverAddress, int port, String username) throws IOException {
        socket = new Socket(serverAddress, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.username = username;
    }



    public void sendMove(String json) {
        out.println(json);
    }

        public void listenForMessages() {
        Thread listener = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("Received: " + line);
                    gameBoard.handleServerJson(line);
                }
            } catch (IOException e) {
                System.out.println("Lost connection to server");
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    public void closeEverything() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        String serverAddress = JOptionPane.showInputDialog("Server address:", "localhost");// Change this to your server's address
        int port = 1234; // Change this to your server's port

        try {
            Client client = new Client(serverAddress, port, username);
    
            client.gameBoard = new GameBoard(client);
            client.gameBoard.setUsername(username);

            client.listenForMessages();

            GameMessage connect = new GameMessage(GameMessage.CONNECT);
            connect.setUsername(username);
            client.sendMove(connect.toJson());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not connect to the server at " + serverAddress + ":" + port + ". Is the server running?");
        }
    }

}
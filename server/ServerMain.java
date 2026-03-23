package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    private static final int PORT = 5000;

    public static void main(String[] args) {

        // Create required objects
        ClientManager clientManager = new ClientManager();
        AuthManager authManager = new AuthManager();
        MessageRouter messageRouter = new MessageRouter(clientManager);

        System.out.println("Server started on port " + PORT);

        try {

            // Start server
            ServerSocket serverSocket = new ServerSocket(PORT);

            // Keep server running forever
            while (true) {

                // Wait for client connection
                Socket socket = serverSocket.accept();

                System.out.println("New client connected: " + socket.getInetAddress());

                // Create handler for this client
                ClientHandler handler = new ClientHandler(socket, clientManager, authManager, messageRouter);

                // Run each client in separate thread
                Thread t = new Thread(handler);
                t.start();
            }

        } catch (IOException e) {

            System.out.println("Server error: " + e.getMessage());
        }
    }
}

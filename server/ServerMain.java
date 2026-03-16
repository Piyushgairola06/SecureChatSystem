package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    private static final int PORT = 5000;

    public static void main(String[] args) {
        ClientManager clientManager = new ClientManager();
        AuthManager authManager = new AuthManager();
        MessageRouter messageRouter = new MessageRouter(clientManager);

        System.out.println("[Server] Starting on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] New connection: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, clientManager, authManager, messageRouter);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Fatal error: " + e.getMessage());
        }
    }
}
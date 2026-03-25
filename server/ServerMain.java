package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    private static final int PORT = 5000;

    public static void main(String[] args) {

        ClientManager  clientManager  = new ClientManager();
        AuthManager    authManager    = new AuthManager();
        MessageRouter  messageRouter  = new MessageRouter(clientManager);

        ServerLogger.log("[Server] Started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket socket = serverSocket.accept();
                ServerLogger.log("[Server] New connection: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket, clientManager, authManager, messageRouter);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            ServerLogger.log("[Server] Fatal error: " + e.getMessage());
        }
    }
}
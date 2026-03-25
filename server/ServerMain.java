package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    private static final int PORT        = 5000;
    private static final int MAX_CLIENTS = 10;

    public static void main(String[] args) {

        ClientManager clientManager = new ClientManager();
        AuthManager   authManager   = new AuthManager();
        MessageRouter messageRouter = new MessageRouter(clientManager);

        ServerLogger.log("[Server] Starting on port " + PORT);
        ServerLogger.log("[Server] Max clients allowed: " + MAX_CLIENTS);

        // Graceful shutdown hook — fires on Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ServerLogger.log("[Server] Shutting down. Notifying all clients...");
            messageRouter.broadcast("[Server] Server is shutting down. Goodbye!", null);
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            ServerLogger.log("[Server] Ready. Waiting for connections...");

            while (true) {
                Socket socket = serverSocket.accept();

                // Reject if server is full
                if (clientManager.getClientCount() >= MAX_CLIENTS) {
                    ServerLogger.log("[Server] Connection rejected (server full): "
                            + socket.getInetAddress());
                    // Tell client before closing
                    socket.getOutputStream().write(
                            "SERVER_FULL\n".getBytes()
                    );
                    socket.close();
                    continue;
                }

                ServerLogger.log("[Server] New connection from: " + socket.getInetAddress()
                        + " | Active: " + (clientManager.getClientCount() + 1) + "/" + MAX_CLIENTS);

                ClientHandler handler = new ClientHandler(
                        socket, clientManager, authManager, messageRouter);

                Thread t = new Thread(handler);
                t.setName("Client-" + socket.getInetAddress() + ":" + socket.getPort());
                t.setDaemon(true);
                t.start();
            }

        } catch (IOException e) {
            ServerLogger.log("[Server] Fatal error: " + e.getMessage());
        }
    }
}
package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ServerMain — Entry point for the Secure Chat Server.
 *
 * Responsibilities:
 * - Starts the TCP server on PORT 5000
 * - Listens for incoming client connections in an infinite loop
 * - Enforces a maximum client limit (MAX_CLIENTS = 10)
 * - Spawns a new ClientHandler thread for each accepted connection
 * - Registers a JVM shutdown hook to notify all clients on Ctrl+C
 *
 * Shared objects (ClientManager, AuthManager, MessageRouter) are created
 * once here and passed into every ClientHandler — they are shared across
 * all client threads, so thread safety in those classes is critical.
 *
 * Author: Member A (Server & Concurrency)
 * Phase:  2 — Core Implementation
 */
public class ServerMain {

    private static final int PORT        = 5000;
    private static final int MAX_CLIENTS = 10;

    public static void main(String[] args) {

        // Create shared objects — one instance shared across all client threads
        ClientManager clientManager = new ClientManager();
        AuthManager   authManager   = new AuthManager();
        MessageRouter messageRouter = new MessageRouter(clientManager);

        ServerLogger.log("[Server] Starting on port " + PORT);
        ServerLogger.log("[Server] Max clients allowed: " + MAX_CLIENTS);

        // Shutdown hook — fires when the server process is killed (Ctrl+C)
        // Broadcasts a goodbye message to all connected clients before exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ServerLogger.log("[Server] Shutting down. Notifying all clients...");
            messageRouter.broadcast("[Server] Server is shutting down. Goodbye!", null);
        }));

        // try-with-resources ensures ServerSocket is closed on exit
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            ServerLogger.log("[Server] Ready. Waiting for connections...");

            while (true) {

                // accept() blocks here until a client connects
                Socket socket = serverSocket.accept();

                // Reject connection if server is at capacity
                if (clientManager.getClientCount() >= MAX_CLIENTS) {
                    ServerLogger.log("[Server] Connection rejected (server full): "
                            + socket.getInetAddress());
                    socket.getOutputStream().write("SERVER_FULL\n".getBytes());
                    socket.close();
                    continue;
                }

                ServerLogger.log("[Server] New connection from: " + socket.getInetAddress()
                        + " | Active: " + (clientManager.getClientCount() + 1) + "/" + MAX_CLIENTS);

                // Create a handler for this client and run it on its own thread
                ClientHandler handler = new ClientHandler(
                        socket, clientManager, authManager, messageRouter);

                Thread t = new Thread(handler);
                t.setName("Client-" + socket.getInetAddress() + ":" + socket.getPort());
                t.setDaemon(true); // thread dies automatically when main thread exits
                t.start();
            }

        } catch (IOException e) {
            ServerLogger.log("[Server] Fatal error: " + e.getMessage());
        }
    }
}
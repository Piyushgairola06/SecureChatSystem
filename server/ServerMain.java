package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    private static final int PORT       = 5000;
    private static final int MAX_CLIENTS = 10;

    public static void main(String[] args) {

        ClientManager  clientManager  = new ClientManager();
        ChannelManager channelManager = new ChannelManager();
        AuthManager    authManager    = new AuthManager();
        MessageRouter  messageRouter  = new MessageRouter(clientManager, channelManager);

        ServerLogger.log("[Server] ═══════════════════════════════════");
        ServerLogger.log("[Server] Secure Chat System  –  Phase 3");
        ServerLogger.log("[Server] Port: " + PORT + "  |  Max clients: " + MAX_CLIENTS);
        ServerLogger.log("[Server] Features: AES-128, SHA-256, Channels, Rate-Limit");
        ServerLogger.log("[Server] ═══════════════════════════════════");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            ServerLogger.log("[Server] Ready. Waiting for connections...");

            while (true) {
                if (clientManager.getCount() >= MAX_CLIENTS) {
                    Thread.sleep(500);
                    continue;
                }
                Socket clientSocket = serverSocket.accept();
                ServerLogger.log("[Server] Incoming connection: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(
                    clientSocket, clientManager, messageRouter, authManager, channelManager
                );
                new Thread(handler).start();
            }

        } catch (IOException e) {
            ServerLogger.log("[Server] Fatal: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

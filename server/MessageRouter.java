package server;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


public class MessageRouter {

    private final ClientManager clientManager;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public MessageRouter(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    /**
     * Routes an incoming message from a sender.
     * /msg <target> <text>  → private
     * anything else         → broadcast
     */
    public void routeMessage(String sender, String message) {
        if (message.startsWith("/msg ")) {
            handlePrivate(sender, message);
        } else {
            String formatted = format(sender, message);
            broadcast(formatted, sender);
            ServerLogger.log(formatted);
        }
    }

    private void handlePrivate(String sender, String message) {
        // Expected format: /msg <username> <message text>
        String[] parts = message.split(" ", 3);
        if (parts.length < 3) {
            ClientHandler senderHandler = clientManager.getClient(sender);
            if (senderHandler != null)
                senderHandler.sendMessage("[Server] Usage: /msg <username> <message>");
            return;
        }

        String target   = parts[1];
        String text     = parts[2];
        ClientHandler targetHandler = clientManager.getClient(target);
        ClientHandler senderHandler = clientManager.getClient(sender);

        if (targetHandler == null) {
            if (senderHandler != null)
                senderHandler.sendMessage("[Server] User '" + target + "' is not online.");
            return;
        }

        String formatted = "[PM][" + LocalTime.now().format(TIME_FMT) + "] " + sender + " → " + target + ": " + text;
        targetHandler.sendMessage(formatted);
        if (senderHandler != null) senderHandler.sendMessage(formatted);
        ServerLogger.log(formatted);
    }

    /** Sends a message to all connected clients, optionally excluding one. */
    public void broadcast(String message, String excludeUsername) {
        for (ClientHandler handler : clientManager.getAllClients()) {
            handler.sendMessage(message);
        }
    }

    private String format(String sender, String message) {
        return "[" + LocalTime.now().format(TIME_FMT) + "] " + sender + ": " + message;
    }
}
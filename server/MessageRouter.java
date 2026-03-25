package server;

import java.util.Collection;

public class MessageRouter {

    private ClientManager clientManager;

    public MessageRouter(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public void routeMessage(String sender, String message) {
        if (message.startsWith("/msg ")) {
            sendPrivateMessage(sender, message);
        } else {
            broadcast(sender + ": " + message, sender);
        }
    }

    private void sendPrivateMessage(String sender, String message) {
        // format: /msg <username> <message text>
        String[] parts = message.split(" ", 3);

        ClientHandler senderHandler = clientManager.getClient(sender);

        if (parts.length < 3) {
            if (senderHandler != null)
                senderHandler.sendMessage("[Server] Usage: /msg <username> <message>");
            return;
        }

        String target  = parts[1];
        String text    = parts[2];

        ClientHandler targetHandler = clientManager.getClient(target);

        if (targetHandler == null) {
            if (senderHandler != null)
                senderHandler.sendMessage("[Server] User not online: " + target);
            return;
        }

        String formatted = "[Private] " + sender + " -> " + target + ": " + text;

        targetHandler.sendMessage(formatted);
        if (senderHandler != null) senderHandler.sendMessage(formatted);

        ServerLogger.log(formatted);
    }

    // excludeUsername: the sender — they don't receive their own broadcast
    public void broadcast(String message, String excludeUsername) {
        Collection<ClientHandler> allClients = clientManager.getAllClients();

        for (ClientHandler client : allClients) {
            // Skip the sender
            if (excludeUsername != null && client == clientManager.getClient(excludeUsername))
                continue;
            client.sendMessage(message);
        }

        ServerLogger.log("[Broadcast] " + message);
    }
}
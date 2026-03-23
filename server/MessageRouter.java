package server;

import java.util.Collection;

public class MessageRouter {

    private ClientManager clientManager;

    public MessageRouter(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    // --------------------------------------
    // Route message
    // --------------------------------------
    public void routeMessage(String sender, String message) {

        if (message.startsWith("/msg ")) {

            sendPrivateMessage(sender, message);

        } else {

            String formatted = sender + ": " + message;

            broadcast(formatted, sender);
        }
    }

    // --------------------------------------
    // Private message
    // --------------------------------------
    private void sendPrivateMessage(String sender, String message) {

        // format: /msg username message
        String[] parts = message.split(" ", 3);

        if (parts.length < 3) {

            ClientHandler senderHandler = clientManager.getClient(sender);

            if (senderHandler != null) {
                senderHandler.sendMessage("Usage: /msg <username> <message>");
            }

            return;
        }

        String target = parts[1];
        String text = parts[2];

        ClientHandler targetHandler = clientManager.getClient(target);
        ClientHandler senderHandler = clientManager.getClient(sender);

        if (targetHandler == null) {

            if (senderHandler != null) {
                senderHandler.sendMessage("User not online: " + target);
            }

            return;
        }

        String finalMessage = "[Private] " + sender + " -> " + target + ": " + text;

        // send to receiver
        targetHandler.sendMessage(finalMessage);

        // also show to sender
        if (senderHandler != null) {
            senderHandler.sendMessage(finalMessage);
        }
    }

    // --------------------------------------
    // Broadcast message
    // --------------------------------------
    public void broadcast(String message, String excludeUsername) {

        Collection<ClientHandler> allClients = clientManager.getAllClients();

        for (ClientHandler client : allClients) {

            client.sendMessage(message);
        }
    }
}

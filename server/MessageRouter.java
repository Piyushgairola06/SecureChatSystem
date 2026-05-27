package server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class MessageRouter {

    private final ClientManager clientManager;
    private final ChannelManager channelManager;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public MessageRouter(ClientManager clientManager, ChannelManager channelManager) {
        this.clientManager = clientManager;
        this.channelManager = channelManager;
    }

    /**
     * Route a message from sender.
     *   /msg <user> <text>   → private message
     *   #<channel> <text>    → channel message (sender must be a member)
     *   anything else        → broadcast
     */
    public void routeMessage(String sender, String message) {
        String ts = "[" + LocalDateTime.now().format(FMT) + "]";

        if (message.startsWith("/msg ")) {
            handlePrivate(sender, message, ts);

        } else if (message.startsWith("#")) {
            handleChannel(sender, message, ts);

        } else {
            handleBroadcast(sender, message, ts);
        }
    }

    // ── private ─────────────────────────────────────────────────────────────

    private void handlePrivate(String sender, String raw, String ts) {
        // /msg <username> <text>
        String[] parts = raw.split(" ", 3);
        ClientHandler senderH = clientManager.getClient(sender);

        if (parts.length < 3) {
            if (senderH != null) senderH.sendMessage("[Server] Usage: /msg <username> <text>");
            return;
        }
        String target = parts[1];
        String text   = parts[2];
        ClientHandler targetH = clientManager.getClient(target);

        if (targetH == null) {
            if (senderH != null) senderH.sendMessage("[Server] '" + target + "' is not online.");
            return;
        }
        targetH.sendMessage(ts + " [PM from " + sender + "] " + text);
        if (senderH != null) senderH.sendMessage(ts + " [PM to " + target + "] " + text);
        ServerLogger.log("[PM] " + sender + " → " + target + ": " + text);
    }

    private void handleChannel(String sender, String raw, String ts) {
        // #<channel> <text>
        int space = raw.indexOf(' ');
        ClientHandler senderH = clientManager.getClient(sender);

        if (space == -1) {
            if (senderH != null) senderH.sendMessage("[Server] Usage: #<channel> <text>");
            return;
        }
        String channel = raw.substring(1, space);
        String text    = raw.substring(space + 1);

        if (!channelManager.isMember(channel, sender)) {
            if (senderH != null)
                senderH.sendMessage("[Server] You are not in #" + channel + ". Use /join " + channel);
            return;
        }

        String formatted = ts + " [#" + channel + "] [" + sender + "] " + text;
        Set<String> members = channelManager.getMembers(channel);
        for (String member : members) {
            ClientHandler h = clientManager.getClient(member);
            if (h != null) h.sendMessage(formatted);
        }
        ServerLogger.log("[#" + channel + "] " + sender + ": " + text);
    }

    private void handleBroadcast(String sender, String raw, String ts) {
        String formatted = ts + " [" + sender + "] " + raw;
        for (ClientHandler h : clientManager.getAllClients()) {
            if (!h.getUsername().equals(sender)) {
                h.sendMessage(formatted);
            }
        }
        ServerLogger.log("[Broadcast] " + sender + ": " + raw);
    }
}

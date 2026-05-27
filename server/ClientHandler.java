package server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientManager clientManager;
    private final MessageRouter messageRouter;
    private final AuthManager authManager;
    private final ChannelManager channelManager;

    private BufferedReader reader;
    private PrintWriter writer;
    private String username;

    // ── Rate limiting (Phase 3) ──────────────────────────────────────────────
    private static final int  RATE_LIMIT   = 10;   // max messages
    private static final long RATE_WINDOW  = 5000; // per 5 seconds (ms)
    private final Deque<Long> msgTimestamps = new ArrayDeque<>();

    public ClientHandler(Socket socket,
                         ClientManager clientManager,
                         MessageRouter messageRouter,
                         AuthManager authManager,
                         ChannelManager channelManager) {
        this.socket = socket;
        this.clientManager = clientManager;
        this.messageRouter = messageRouter;
        this.authManager = authManager;
        this.channelManager = channelManager;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            if (!handleLogin()) {
                socket.close();
                return;
            }

            String encryptedLine;
            while ((encryptedLine = reader.readLine()) != null) {
                String input = EncryptionUtil.decrypt(encryptedLine);
                processClientInput(input);
            }
        } catch (IOException e) {
            // client disconnected abruptly – handled in finally
        } finally {
            disconnect();
        }
    }

    // ── Login handshake ─────────────────────────────────────────────────────

    private boolean handleLogin() throws IOException {
        writer.println("LOGIN"); // prompt client

        String action   = reader.readLine(); // "LOGIN" or "REGISTER"
        String uname    = reader.readLine();
        String password = reader.readLine();

        if (action == null || uname == null || password == null) return false;

        uname = uname.trim();

        boolean ok;
        if (action.equalsIgnoreCase("REGISTER")) {
            ok = authManager.register(uname, password);
            if (!ok) { writer.println("REJECT Username already taken."); return false; }
        } else {
            ok = authManager.authenticate(uname, password);
            if (!ok) { writer.println("REJECT Invalid credentials."); return false; }
        }

        if (clientManager.isOnline(uname)) {
            writer.println("REJECT Already logged in from another session.");
            return false;
        }

        this.username = uname;
        clientManager.addClient(username, this);
        writer.println("OK");
        sendMessage("[Server] Welcome, " + username + "! Type /help for commands.");

        // Notify others
        for (ClientHandler h : clientManager.getAllClients()) {
            if (!h.getUsername().equals(username)) {
                h.sendMessage("[Server] *** " + username + " has joined ***");
            }
        }
        ServerLogger.log("[Login] " + username + " from " + socket.getInetAddress());
        return true;
    }

    // ── Command dispatch ─────────────────────────────────────────────────────

    private void processClientInput(String input) {
        if (input == null || input.isBlank()) return;

        if (isRateLimited()) {
            sendMessage("[Server] ⚠ Rate limit hit. Please slow down (max 10 msg / 5 sec).");
            return;
        }

        switch (input.split(" ")[0].toLowerCase()) {

            case "/quit":
                disconnect();
                break;

            case "/list":
                sendMessage("[Server] Online: " + clientManager.getOnlineList());
                break;

            case "/help":
                sendMessage(helpText());
                break;

            case "/join":
                handleJoin(input);
                break;

            case "/leave":
                handleLeave(input);
                break;

            case "/channels":
                sendMessage("[Server] Channels: " + channelManager.getChannelList());
                break;

            default:
                messageRouter.routeMessage(username, input);
        }
    }

    private void handleJoin(String input) {
        String[] parts = input.split(" ", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            sendMessage("[Server] Usage: /join <channel>");
            return;
        }
        String channel = parts[1].trim();
        channelManager.joinChannel(channel, username);
        sendMessage("[Server] Joined #" + channel + ". Use  #" + channel + " <text>  to chat.");
        // Notify existing members
        for (String member : channelManager.getMembers(channel)) {
            ClientHandler h = clientManager.getClient(member);
            if (h != null && !member.equals(username)) {
                h.sendMessage("[Server] " + username + " joined #" + channel);
            }
        }
    }

    private void handleLeave(String input) {
        String[] parts = input.split(" ", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            sendMessage("[Server] Usage: /leave <channel>");
            return;
        }
        String channel = parts[1].trim();
        channelManager.leaveChannel(channel, username);
        sendMessage("[Server] Left #" + channel);
    }

    // ── Rate limiting ────────────────────────────────────────────────────────

    private boolean isRateLimited() {
        long now = System.currentTimeMillis();
        while (!msgTimestamps.isEmpty() && now - msgTimestamps.peekFirst() > RATE_WINDOW) {
            msgTimestamps.pollFirst();
        }
        if (msgTimestamps.size() >= RATE_LIMIT) return true;
        msgTimestamps.addLast(now);
        return false;
    }

    // ── Public API used by MessageRouter ─────────────────────────────────────

    public void sendMessage(String plainText) {
        writer.println(EncryptionUtil.encrypt(plainText));
    }

    public String getUsername() {
        return username;
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    private void disconnect() {
        if (username != null) {
            clientManager.removeClient(username);
            channelManager.leaveAllChannels(username);
            for (ClientHandler h : clientManager.getAllClients()) {
                h.sendMessage("[Server] *** " + username + " has left ***");
            }
            ServerLogger.log("[Disconnect] " + username);
            username = null;
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    private String helpText() {
        return  "──────────── Commands ────────────\n" +
                "  <message>                broadcast\n" +
                "  /msg <user> <text>       private message\n" +
                "  #<channel> <text>        channel message\n" +
                "  /join <channel>          join / create channel\n" +
                "  /leave <channel>         leave channel\n" +
                "  /channels                list active channels\n" +
                "  /list                    list online users\n" +
                "  /quit                    disconnect\n" +
                "──────────────────────────────────";
    }
}

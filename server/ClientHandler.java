package server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler implements Runnable {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private Socket        socket;
    private ClientManager clientManager;
    private AuthManager   authManager;
    private MessageRouter messageRouter;

    private PrintWriter    out;
    private BufferedReader in;

    private String username;

    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    public ClientHandler(Socket socket, ClientManager clientManager,
                         AuthManager authManager, MessageRouter messageRouter) {
        this.socket        = socket;
        this.clientManager = clientManager;
        this.authManager   = authManager;
        this.messageRouter = messageRouter;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            if (!handleLogin()) {
                socket.close();
                return;
            }

            clientManager.addClient(username, this);

            sendMessage("[Server] Welcome, " + username + "! Type /help for commands.");
            messageRouter.broadcast("[Server] " + username + " joined the chat.", username);

            String line;
            while ((line = in.readLine()) != null) {
                String message = EncryptionUtil.decrypt(line);
                processMessage(message);
            }

        } catch (IOException e) {
            ServerLogger.log("[Server] Connection lost: " + username);
        } finally {
            disconnect();
        }
    }

    // --------------------------------------------------
    // LOGIN / REGISTER
    // --------------------------------------------------
    private boolean handleLogin() throws IOException {
        out.println("LOGIN");

        String mode = in.readLine();
        String user = in.readLine();
        String pass = in.readLine();

        if (mode == null || user == null || pass == null) return false;

        user = user.trim();
        pass = pass.trim();

        // Empty check
        if (user.isEmpty() || pass.isEmpty()) {
            out.println("REJECT Username and password cannot be empty");
            return false;
        }

        // Username format: 3-20 chars, letters/digits/underscore only
        // Also blocks colon which would break users.txt parsing
        if (!user.matches("[a-zA-Z0-9_]{3,20}")) {
            out.println("REJECT Username must be 3-20 characters: letters, digits, underscores only");
            return false;
        }

        // Password min length
        if (pass.length() < 4) {
            out.println("REJECT Password must be at least 4 characters");
            return false;
        }

        if ("REGISTER".equals(mode)) {
            if (!authManager.register(user, pass)) {
                out.println("REJECT Username already taken");
                ServerLogger.log("[Auth] Register failed (taken): " + user);
                return false;
            }
            ServerLogger.log("[Auth] Registered: " + user);

        } else {
            if (!authManager.authenticate(user, pass)) {
                out.println("REJECT Invalid username or password");
                ServerLogger.log("[Auth] Failed login: " + user);
                return false;
            }
        }

        if (clientManager.isOnline(user)) {
            out.println("REJECT User already logged in");
            return false;
        }

        username = user;
        out.println("OK");
        ServerLogger.log("[Auth] Logged in: " + username);
        return true;
    }

    // --------------------------------------------------
    // PROCESS MESSAGE
    // --------------------------------------------------
    private void processMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        message = message.trim();

        // ---- Built-in commands ----

        if (message.equalsIgnoreCase("/quit")) {
            try { socket.close(); } catch (IOException ignored) {}
            return;
        }

        if (message.equalsIgnoreCase("/help")) {
            sendMessage("[Server] Commands:");
            sendMessage("  /list            - show online users");
            sendMessage("  /msg <user> <text> - private message");
            sendMessage("  /quit            - disconnect");
            return;
        }

        if (message.equalsIgnoreCase("/list")) {
            java.util.Set<String> online = clientManager.getAllUsernames();
            if (online.isEmpty()) {
                sendMessage("[Server] No users online.");
            } else {
                sendMessage("[Server] Online (" + online.size() + "): "
                        + String.join(", ", online));
            }
            return;
        }

        // ---- Message length cap ----
        if (message.length() > MAX_MESSAGE_LENGTH) {
            sendMessage("[Server] Message too long. Max " + MAX_MESSAGE_LENGTH + " characters.");
            return;
        }

        // ---- Route to MessageRouter ----
        messageRouter.routeMessage(username, message);
    }

    // --------------------------------------------------
    // SEND MESSAGE
    // --------------------------------------------------
    public void sendMessage(String message) {
        out.println(EncryptionUtil.encrypt(message));
    }

    // --------------------------------------------------
    // DISCONNECT
    // --------------------------------------------------
    private void disconnect() {
        if (!disconnected.compareAndSet(false, true)) return;

        try {
            if (username != null) {
                clientManager.removeClient(username);
                messageRouter.broadcast("[Server] " + username + " left the chat.", username);
                ServerLogger.log("[Server] Disconnected: " + username);
            }
            socket.close();
        } catch (IOException e) {
            ServerLogger.log("[Server] Error on disconnect: " + e.getMessage());
        }
    }
}
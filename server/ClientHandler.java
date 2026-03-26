package server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ClientHandler — Manages the full lifecycle of one connected client.
 *
 * One instance is created per client and runs on its own dedicated thread.
 * It handles everything from login/registration through to disconnection.
 *
 * Responsibilities:
 * - Manages the login/register handshake with the client
 * - Validates username format and password length before auth
 * - Reads incoming encrypted messages in a loop
 * - Processes built-in commands: /help, /list, /quit
 * - Forwards chat messages to MessageRouter for routing
 * - Sends encrypted outgoing messages via sendMessage()
 * - Cleans up cleanly on disconnect (only once, via AtomicBoolean guard)
 *
 * Handshake protocol (both sides must follow this exactly):
 *   Server → Client : "LOGIN"
 *   Client → Server : "LOGIN" or "REGISTER"
 *   Client → Server : username
 *   Client → Server : password
 *   Server → Client : "OK" or "REJECT <reason>"
 *
 * Author: Member A (Server & Concurrency)
 * Phase:  2 — Core Implementation
 */
public class ClientHandler implements Runnable {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private Socket        socket;
    private ClientManager clientManager;
    private AuthManager   authManager;
    private MessageRouter messageRouter;

    private PrintWriter    out;
    private BufferedReader in;

    private String username;

    // Guards against disconnect() being called twice (once from /quit, once from finally)
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    public ClientHandler(Socket socket, ClientManager clientManager,
                         AuthManager authManager, MessageRouter messageRouter) {
        this.socket        = socket;
        this.clientManager = clientManager;
        this.authManager   = authManager;
        this.messageRouter = messageRouter;
    }

    /**
     * Thread entry point — runs for the entire lifetime of the client connection.
     * Exits when the client disconnects or an IOException occurs.
     */
    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Attempt login/register — close and exit if it fails
            if (!handleLogin()) {
                socket.close();
                return;
            }

            // Register in the active user map BEFORE broadcasting "joined"
            // so the new user is already findable if someone replies immediately
            clientManager.addClient(username, this);

            sendMessage("[Server] Welcome, " + username + "! Type /help for commands.");
            messageRouter.broadcast("[Server] " + username + " joined the chat.", username);

            // Main message loop — runs until client disconnects
            String line;
            while ((line = in.readLine()) != null) {
                String message = EncryptionUtil.decrypt(line);
                processMessage(message);
            }

        } catch (IOException e) {
            ServerLogger.log("[Server] Connection lost: " + username);
        } finally {
            // Always clean up, even if an exception occurred mid-loop
            disconnect();
        }
    }

    // --------------------------------------------------
    // LOGIN / REGISTER HANDSHAKE
    // --------------------------------------------------

    /**
     * Handles the initial login or registration handshake.
     * Reads mode, username, and password. Validates format, then
     * delegates to AuthManager for credential checking.
     *
     * @return true if login/register succeeded, false otherwise
     */
    private boolean handleLogin() throws IOException {

        // Signal to the client that the server is ready
        out.println("LOGIN");

        String mode = in.readLine(); // "LOGIN" or "REGISTER"
        String user = in.readLine();
        String pass = in.readLine();

        if (mode == null || user == null || pass == null) return false;

        user = user.trim();
        pass = pass.trim();

        // Basic empty check
        if (user.isEmpty() || pass.isEmpty()) {
            out.println("REJECT Username and password cannot be empty");
            return false;
        }

        // Username must be 3-20 chars, letters/digits/underscore only
        // This also blocks colons which would corrupt the users.txt format (user:hash)
        if (!user.matches("[a-zA-Z0-9_]{3,20}")) {
            out.println("REJECT Username must be 3-20 characters: letters, digits, underscores only");
            return false;
        }

        // Minimum password length
        if (pass.length() < 4) {
            out.println("REJECT Password must be at least 4 characters");
            return false;
        }

        if ("REGISTER".equals(mode)) {
            // register() returns false if username is already taken
            if (!authManager.register(user, pass)) {
                out.println("REJECT Username already taken");
                ServerLogger.log("[Auth] Register failed (taken): " + user);
                return false;
            }
            ServerLogger.log("[Auth] Registered: " + user);

        } else {
            // authenticate() checks SHA-256 hash against stored hash
            if (!authManager.authenticate(user, pass)) {
                out.println("REJECT Invalid username or password");
                ServerLogger.log("[Auth] Failed login: " + user);
                return false;
            }
        }

        // Prevent the same account from being logged in twice simultaneously
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
    // MESSAGE PROCESSING
    // --------------------------------------------------

    /**
     * Handles a decrypted message from the client.
     * Intercepts built-in commands before passing to MessageRouter.
     */
    private void processMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        message = message.trim();

        // /quit — close socket and let finally handle cleanup
        // We deliberately do NOT call disconnect() here to avoid double-disconnect.
        // Closing the socket causes readLine() to return null, exiting the loop,
        // and the finally block calls disconnect() exactly once.
        if (message.equalsIgnoreCase("/quit")) {
            try { socket.close(); } catch (IOException ignored) {}
            return;
        }

        // /help — show available commands to the requesting client only
        if (message.equalsIgnoreCase("/help")) {
            sendMessage("[Server] Commands:");
            sendMessage("  /list              - show online users");
            sendMessage("  /msg <user> <text> - private message");
            sendMessage("  /quit              - disconnect");
            return;
        }

        // /list — show all currently online usernames
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

        // Enforce message length cap to prevent abuse
        if (message.length() > MAX_MESSAGE_LENGTH) {
            sendMessage("[Server] Message too long. Max " + MAX_MESSAGE_LENGTH + " characters.");
            return;
        }

        // Pass to MessageRouter — it decides broadcast vs private message
        messageRouter.routeMessage(username, message);
    }

    // --------------------------------------------------
    // SEND MESSAGE
    // --------------------------------------------------

    /**
     * Encrypts and sends a message to this client.
     * Called by MessageRouter and ClientHandler itself for server notices.
     */
    public void sendMessage(String message) {
        out.println(EncryptionUtil.encrypt(message));
    }

    // --------------------------------------------------
    // DISCONNECT
    // --------------------------------------------------

    /**
     * Cleans up when a client disconnects for any reason.
     * The AtomicBoolean ensures this runs at most once per client,
     * preventing double removal and duplicate "left the chat" broadcasts.
     */
    private void disconnect() {
        // compareAndSet(false, true) returns true only the first time
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
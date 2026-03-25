package server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ClientManager clientManager;
    private AuthManager authManager;
    private MessageRouter messageRouter;

    private PrintWriter out;
    private BufferedReader in;

    private String username;

    // Guard against double disconnect() calls
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    public ClientHandler(Socket socket, ClientManager clientManager,
                         AuthManager authManager, MessageRouter messageRouter) {
        this.socket = socket;
        this.clientManager = clientManager;
        this.authManager = authManager;
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

            // Register BEFORE broadcasting "joined" so the new user
            // is already in the map if anyone replies immediately
            clientManager.addClient(username, this);

            sendMessage("[Server] Welcome, " + username + "!");
            messageRouter.broadcast("[Server] " + username + " joined the chat.", username);

            String line;
            while ((line = in.readLine()) != null) {
                String message = EncryptionUtil.decrypt(line);
                processMessage(message);
            }

        } catch (IOException e) {
            ServerLogger.log("[Server] Connection lost: " + username);
        } finally {
            // Only disconnect once — guards against /quit path + finally path
            disconnect();
        }
    }

    // --------------------------------------------------
    // LOGIN / REGISTER
    // --------------------------------------------------
    private boolean handleLogin() throws IOException {
        out.println("LOGIN");  // server ready signal (same for both flows)

        String mode = in.readLine(); // "LOGIN" or "REGISTER"
        String user = in.readLine();
        String pass = in.readLine();

        if (mode == null || user == null || pass == null) return false;

        user = user.trim();
        pass = pass.trim();

        if (user.isEmpty() || pass.isEmpty()) {
            out.println("REJECT Username and password cannot be empty");
            return false;
        }

        if ("REGISTER".equals(mode)) {
            boolean registered = authManager.register(user, pass);
            if (!registered) {
                out.println("REJECT Username already taken");
                ServerLogger.log("[Auth] Register failed (taken): " + user);
                return false;
            }
            ServerLogger.log("[Auth] New user registered: " + user);
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
        ServerLogger.log("[Auth] Login success: " + username);
        return true;
    }

    // --------------------------------------------------
    // PROCESS MESSAGE
    // --------------------------------------------------
    private void processMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;

        message = message.trim();

        if (message.equalsIgnoreCase("/quit")) {
            // Let the finally block handle cleanup — don't call disconnect() here
            // Just close the socket so readLine() returns null and exits the loop cleanly
            try {
                socket.close();
            } catch (IOException ignored) {}
            return;
        }

        messageRouter.routeMessage(username, message);
    }

    // --------------------------------------------------
    // SEND MESSAGE
    // --------------------------------------------------
    public void sendMessage(String message) {
        out.println(EncryptionUtil.encrypt(message));
    }

    // --------------------------------------------------
    // DISCONNECT (called only once via AtomicBoolean guard)
    // --------------------------------------------------
    private void disconnect() {
        if (!disconnected.compareAndSet(false, true)) return;

        try {
            if (username != null) {
                clientManager.removeClient(username);
                messageRouter.broadcast("[Server] " + username + " left the chat.", username);
                ServerLogger.log("[Server] " + username + " disconnected.");
            }
            socket.close();
        } catch (IOException e) {
            ServerLogger.log("[Server] Error closing connection: " + e.getMessage());
        }
    }
}
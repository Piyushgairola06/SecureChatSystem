package server;

import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientManager clientManager;
    private final AuthManager authManager;
    private final MessageRouter messageRouter;

    private PrintWriter out;
    private BufferedReader in;
    private String username;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

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
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            if (!handleLogin()) {
                socket.close();
                return;
            }

            sendMessage("[Server] Welcome, " + username + "! Type /msg <user> <text> for private messages.");
            messageRouter.broadcast("[Server] " + username + " has joined the chat.", null);
            clientManager.addClient(username, this);

            String line;
            while ((line = in.readLine()) != null) {
                String decrypted = EncryptionUtil.decrypt(line);
                processClientInput(decrypted);
            }

        } catch (IOException e) {
            System.out.println("[ClientHandler] Connection lost: " + username);
        } finally {
            disconnect();
        }
    }

    private boolean handleLogin() throws IOException {
        // Handshake is plain text — encryption starts after login
        out.println("LOGIN");
        String user = in.readLine();
        String pass = in.readLine();

        if (user == null || pass == null) return false;

        if (authManager.authenticate(user, pass)) {
            if (clientManager.isOnline(user)) {
                out.println("REJECT Already logged in.");
                return false;
            }
            username = user;
            out.println("OK");
            return true;
        } else {
            out.println("REJECT Invalid credentials.");
            return false;
        }
    }

    private void processClientInput(String input) {
        if (input == null || input.isBlank()) return;

        String timestamp = "[" + LocalTime.now().format(TIME_FMT) + "]";

        if (input.startsWith("/quit")) {
            disconnect();
        } else {
            messageRouter.routeMessage(username, input);
        }
    }

    public void sendMessage(String message) {
        String encrypted = EncryptionUtil.encrypt(message);
        out.println(encrypted);
    }

    private void disconnect() {
        try {
            if (username != null) {
                clientManager.removeClient(username);
                messageRouter.broadcast("[Server] " + username + " has left the chat.", null);
            }
            socket.close();
        } catch (IOException ignored) {}
    }
}
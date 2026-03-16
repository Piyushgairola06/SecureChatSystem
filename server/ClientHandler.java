package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ClientManager clientManager;
    private AuthManager authManager;
    private MessageRouter messageRouter;

    private PrintWriter out;
    private BufferedReader in;

    private String username;

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

            // Setup input/output streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Login step
            boolean loginSuccess = handleLogin();

            if (!loginSuccess) {
                socket.close();
                return;
            }

            // Welcome message
            sendMessage("[Server] Welcome " + username);

            // Inform others
            messageRouter.broadcast("[Server] " + username + " joined the chat", username);

            // Add client to active list
            clientManager.addClient(username, this);

            // Listen for client messages
            String line;

            while ((line = in.readLine()) != null) {

                String message = EncryptionUtil.decrypt(line);

                processMessage(message);
            }

        } catch (IOException e) {

            System.out.println("Connection lost with " + username);

        } finally {

            disconnect();
        }
    }

    // --------------------------------------
    // LOGIN
    // --------------------------------------
    private boolean handleLogin() throws IOException {

        out.println("LOGIN");

        String user = in.readLine();
        String pass = in.readLine();

        if (user == null || pass == null) {
            return false;
        }

        boolean valid = authManager.authenticate(user, pass);

        if (!valid) {
            out.println("REJECT Invalid username or password");
            return false;
        }

        if (clientManager.isOnline(user)) {
            out.println("REJECT User already logged in");
            return false;
        }

        username = user;

        out.println("OK");

        return true;
    }

    // --------------------------------------
    // PROCESS CLIENT MESSAGE
    // --------------------------------------
    private void processMessage(String message) {

        if (message == null) {
            return;
        }

        message = message.trim();

        if (message.length() == 0) {
            return;
        }

        // Quit command
        if (message.equals("/quit")) {

            disconnect();
            return;
        }

        // Send message through router
        messageRouter.routeMessage(username, message);
    }

    // --------------------------------------
    // SEND MESSAGE TO CLIENT
    // --------------------------------------
    public void sendMessage(String message) {

        String encrypted = EncryptionUtil.encrypt(message);

        out.println(encrypted);
    }

    // --------------------------------------
    // DISCONNECT
    // --------------------------------------
    private void disconnect() {

        try {

            if (username != null) {

                clientManager.removeClient(username);

                messageRouter.broadcast("[Server] " + username + " left the chat", username);
            }

            socket.close();

        } catch (IOException e) {

            System.out.println("Error closing connection");
        }
    }
}

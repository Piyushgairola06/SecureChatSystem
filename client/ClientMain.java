package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {

    private static final String HOST = "localhost";
    private static final int    PORT = 5000;

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔══════════════════════════╗");
        System.out.println("║      Secure Chat v2       ║");
        System.out.println("╚══════════════════════════╝");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();

        boolean isRegister = choice.equals("2");

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        if (isRegister) {
            System.out.print("Confirm Password: ");
            String confirm = scanner.nextLine().trim();
            if (!confirm.equals(password)) {
                System.out.println("[Client] Passwords do not match.");
                return;
            }
        }

        Socket socket;
        try {
            socket = new Socket(HOST, PORT);
        } catch (IOException e) {
            System.out.println("[Client] Cannot connect to server. Is it running?");
            return;
        }

        PrintWriter    out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String signal = in.readLine();

        // Server full
        if ("SERVER_FULL".equals(signal)) {
            System.out.println("[Client] Server is full. Try again later.");
            socket.close();
            return;
        }

        if (!"LOGIN".equals(signal)) {
            System.out.println("[Client] Unexpected signal: " + signal);
            socket.close();
            return;
        }

        out.println(isRegister ? "REGISTER" : "LOGIN");
        out.println(username);
        out.println(password);

        String response = in.readLine();
        if (response == null || response.startsWith("REJECT")) {
            System.out.println("[Client] " + (isRegister ? "Registration" : "Login")
                    + " failed: " + response);
            socket.close();
            return;
        }

        System.out.println("[Client] " + (isRegister ? "Registered & connected!" : "Connected!"));
        System.out.println("[Client] /help for commands");

        Thread receiver = new Thread(new ClientReceiver(in));
        receiver.setDaemon(true);
        receiver.start();

        new ClientSender(out, scanner).run();

        socket.close();
    }
}
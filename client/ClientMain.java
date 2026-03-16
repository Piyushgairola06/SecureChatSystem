package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {

    private static final String HOST = "localhost";
    private static final int    PORT = 5000;

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        Socket socket = new Socket(HOST, PORT);
        PrintWriter  out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Server sends "LOGIN" first
        String signal = in.readLine();
        if (!"LOGIN".equals(signal)) {
            System.out.println("[Client] Unexpected server signal: " + signal);
            socket.close();
            return;
        }

        // Send credentials plain (they travel before encryption is fully bootstrapped)
        out.println(username);
        out.println(password);

        String response = in.readLine();
        if (response == null || response.startsWith("REJECT")) {
            System.out.println("[Client] Login failed: " + response);
            socket.close();
            return;
        }

        System.out.println("[Client] Connected. Type /quit to exit.");

        // Start receiver thread
        Thread receiver = new Thread(new ClientReceiver(in));
        receiver.setDaemon(true);
        receiver.start();

        // Sender runs on main thread
        new ClientSender(out, scanner).run();

        socket.close();
    }
}
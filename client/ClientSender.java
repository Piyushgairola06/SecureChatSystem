package client;

import server.EncryptionUtil;
import java.io.PrintWriter;
import java.util.Scanner;

public class ClientSender implements Runnable {

    private final PrintWriter out;
    private final Scanner     scanner;

    public ClientSender(PrintWriter out, Scanner scanner) {
        this.out     = out;
        this.scanner = scanner;
    }

    @Override
    public void run() {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            out.println(EncryptionUtil.encrypt(line));
            if (line.equalsIgnoreCase("/quit")) break;
        }
    }
}
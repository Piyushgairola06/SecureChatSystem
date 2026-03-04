package client;

import server.EncryptionUtil;
import java.io.BufferedReader;
import java.io.IOException;

public class ClientReceiver implements Runnable {

    private final BufferedReader in;

    public ClientReceiver(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(EncryptionUtil.decrypt(line));
            }
        } catch (IOException e) {
            System.out.println("[Client] Disconnected from server.");
        }
    }
}
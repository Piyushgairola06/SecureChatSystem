package client;

import server.EncryptionUtil;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Continuously reads encrypted messages from the server,
 * decrypts them, and dispatches them to the UI callback.
 */
public class ClientReceiver implements Runnable {

    private final BufferedReader reader;
    private final MessageCallback callback;
    private volatile boolean running = true;

    public ClientReceiver(BufferedReader reader, MessageCallback callback) {
        this.reader   = reader;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                String decrypted = EncryptionUtil.decrypt(line);
                callback.onMessage(decrypted);
            }
        } catch (IOException e) {
            if (running) callback.onMessage("[Disconnected from server]");
        }
        running = false;
    }

    public void stop() {
        running = false;
    }

    /** Callback interface so the receiver can push messages to any UI. */
    public interface MessageCallback {
        void onMessage(String message);
    }
}

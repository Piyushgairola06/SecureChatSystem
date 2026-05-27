package client;

import server.EncryptionUtil;

import java.io.PrintWriter;

/**
 * Sends an encrypted message to the server.
 * Stateless utility – called from the UI thread (EDT-safe).
 */
public class ClientSender {

    private final PrintWriter writer;

    public ClientSender(PrintWriter writer) {
        this.writer = writer;
    }

    /** Encrypt and send one message line. */
    public void send(String message) {
        writer.println(EncryptionUtil.encrypt(message));
    }
}

package server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ServerLogger — Centralised logging utility for the chat server.
 *
 * All server-side events (connections, auth results, messages, errors)
 * are routed through this class. Each call writes a timestamped line
 * to both the console and storage/chatlog.txt.
 *
 * Why static:
 * Being fully static means any class can call ServerLogger.log() without
 * holding a reference. This avoids passing a logger instance through every
 * constructor and keeps call sites clean.
 *
 * Known limitation:
 * The file is opened and closed on every log() call. This is fine for
 * academic use. A production system would use a buffered async log queue
 * (e.g. java.util.logging or Log4j) to avoid the I/O overhead per message.
 *
 * Author: Member A (Server & Concurrency)
 * Phase:  2 — Core Implementation
 */
public class ServerLogger {

    private static final String LOG_FILE = "storage/chatlog.txt";

    // Timestamp format: 2026-03-26 14:35:02
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Logs a message to both stdout and chatlog.txt with a timestamp prefix.
     *
     * Example output:
     *   [2026-03-26 14:35:02] [Auth] Logged in: alice
     *
     * @param message the event description to log
     */
    public static void log(String message) {
        String timestamped = "[" + LocalDateTime.now().format(FORMATTER) + "] " + message;

        // Print to server console
        System.out.println(timestamped);

        // Append to log file — try-with-resources guarantees the writer is closed
        // even if an exception occurs mid-write
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(timestamped);
        } catch (IOException e) {
            // Can't use log() here (infinite loop risk) — print directly
            System.out.println("[Logger] Failed to write log: " + e.getMessage());
        }
    }
}
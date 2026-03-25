package server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {

    private static final String LOG_FILE = "storage/chatlog.txt";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String message) {
        String timestamped = "[" + LocalDateTime.now().format(FORMATTER) + "] " + message;

        System.out.println(timestamped);

        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(timestamped);
        } catch (IOException e) {
            System.out.println("[Logger] Error writing log: " + e.getMessage());
        }
    }
}
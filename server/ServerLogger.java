package server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {

    private static final String LOG_FILE = "storage/chatlog.txt";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String message) {
        String line = "[" + LocalDateTime.now().format(FMT) + "] " + message;
        System.out.println(line);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[Logger ERROR] " + e.getMessage());
        }
    }
}

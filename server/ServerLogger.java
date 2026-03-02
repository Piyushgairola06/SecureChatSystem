package server;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {

    private static final String LOG_FILE = "storage/chatlog.txt";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Static-only utility — no instantiation needed
    private ServerLogger() {}

    public static synchronized void log(String message) {
        String entry = "[" + LocalDateTime.now().format(DT_FMT) + "] " + message;
        System.out.println(entry);   // console
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println(entry);
        } catch (IOException e) {
            System.err.println("[ServerLogger] Could not write log: " + e.getMessage());
        }
    }
}
package server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ServerLogger {

    private static final String LOG_FILE = "storage/chatlog.txt";

    // --------------------------------------
    // Log message
    // --------------------------------------
    public static void log(String message) {

        // Print on console
        System.out.println(message);

        // Save to file
        try {

            PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true));

            writer.println(message);

            writer.close();

        } catch (IOException e) {

            System.out.println("Error writing log file");
        }
    }
}

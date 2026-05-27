package server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.locks.ReentrantLock;

public class AuthManager {

    private static final String USERS_FILE = "storage/users.txt";
    private static final ReentrantLock lock = new ReentrantLock();

    /** Returns true if username+password match a stored record. */
    public boolean authenticate(String username, String password) {
        String hashed = hash(password);
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(hashed)) {
                    return true;
                }
            }
        } catch (IOException e) {
            ServerLogger.log("[AuthManager] Read error: " + e.getMessage());
        }
        return false;
    }

    /** Returns true if registration succeeded; false if username already exists. */
    public boolean register(String username, String password) {
        lock.lock();
        try {
            // Check for existing username
            try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":", 2);
                    if (parts.length >= 1 && parts[0].equals(username)) {
                        return false;
                    }
                }
            } catch (IOException e) {
                ServerLogger.log("[AuthManager] Read error during register: " + e.getMessage());
                return false;
            }
            // Append new user
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
                writer.write(username + ":" + hash(password));
                writer.newLine();
            }
            ServerLogger.log("[AuthManager] Registered new user: " + username);
            return true;
        } catch (IOException e) {
            ServerLogger.log("[AuthManager] Write error: " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    /** SHA-256 hex digest. */
    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input; // should never happen
        }
    }
}

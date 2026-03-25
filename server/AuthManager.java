package server;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    private static final String USERS_FILE = "storage/users.txt";

    // ConcurrentHashMap for thread-safe access
    private ConcurrentHashMap<String, String> credentials;

    public AuthManager() {
        credentials = new ConcurrentHashMap<>();
        loadUsers();
    }

    public boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;

        String storedHash = credentials.get(username);
        if (storedHash == null) return false;

        return storedHash.equals(hash(password));
    }

    public boolean register(String username, String password) {
        if (credentials.containsKey(username)) return false;

        String hashedPassword = hash(password);
        credentials.put(username, hashedPassword);
        saveUser(username, hashedPassword);
        return true;
    }

    private void loadUsers() {
        File file = new File(USERS_FILE);

        if (!file.exists()) {
            ServerLogger.log("[AuthManager] users.txt not found. Starting with empty list.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(":");
                if (parts.length == 2) {
                    credentials.put(parts[0], parts[1]);
                }
            }
            ServerLogger.log("[AuthManager] Loaded " + credentials.size() + " users.");
        } catch (IOException e) {
            ServerLogger.log("[AuthManager] Error reading users file: " + e.getMessage());
        }
    }

    private void saveUser(String username, String hashedPassword) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE, true))) {
            writer.println(username + ":" + hashedPassword);
        } catch (IOException e) {
            ServerLogger.log("[AuthManager] Error saving user: " + e.getMessage());
        }
    }

    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            ServerLogger.log("[AuthManager] Hash algorithm error");
            return null;
        }
    }
}
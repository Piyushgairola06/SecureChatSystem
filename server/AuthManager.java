package server;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    // Path relative to project root — adjust if needed
    private static final String USERS_FILE = "storage/users.txt";
    private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<>();

    public AuthManager() {
        loadUsers();
    }

    /**
     * Validates username + plain-text password against stored SHA-256 hash.
     */
    public boolean authenticate(String username, String password) {
        if (username == null || password == null) return false;
        String storedHash = credentials.get(username.trim());
        if (storedHash == null) return false;
        return storedHash.equals(hash(password.trim()));
    }

    /**
     * Registers a new user. Returns false if username already exists.
     * Call this from an admin utility or when first setting up users.txt manually.
     */
    public boolean register(String username, String password) {
        if (credentials.containsKey(username)) return false;
        String hashed = hash(password);
        credentials.put(username, hashed);
        persistUser(username, hashed);
        return true;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            System.out.println("[AuthManager] users.txt not found — starting empty.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    credentials.put(parts[0].trim(), parts[1].trim());
                }
            }
            System.out.println("[AuthManager] Loaded " + credentials.size() + " user(s).");
        } catch (IOException e) {
            System.err.println("[AuthManager] Error reading users.txt: " + e.getMessage());
        }
    }

    private void persistUser(String username, String hashedPassword) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE, true))) {
            pw.println(username + ":" + hashedPassword);
        } catch (IOException e) {
            System.err.println("[AuthManager] Could not write to users.txt: " + e.getMessage());
        }
    }

    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
package server;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class AuthManager {

    // File where usernames and hashed passwords are stored
    private static final String USERS_FILE = "storage/users.txt";

    // Stores username -> hashed password
    private HashMap<String, String> credentials;

    public AuthManager() {
        credentials = new HashMap<String, String>();
        loadUsers();
    }

    // -------------------------------------------------------
    // Authenticate user
    // -------------------------------------------------------
    public boolean authenticate(String username, String password) {

        if (username == null || password == null) {
            return false;
        }

        String storedHash = credentials.get(username);

        if (storedHash == null) {
            return false;
        }

        String enteredHash = hash(password);

        if (storedHash.equals(enteredHash)) {
            return true;
        } else {
            return false;
        }
    }

    // -------------------------------------------------------
    // Register new user
    // -------------------------------------------------------
    public boolean register(String username, String password) {

        if (credentials.containsKey(username)) {
            return false; // user already exists
        }

        String hashedPassword = hash(password);

        credentials.put(username, hashedPassword);

        saveUser(username, hashedPassword);

        return true;
    }

    // -------------------------------------------------------
    // Load users from file
    // -------------------------------------------------------
    private void loadUsers() {

        File file = new File(USERS_FILE);

        if (!file.exists()) {
            System.out.println("users.txt not found. Starting with empty list.");
            return;
        }

        try {

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.length() == 0) {
                    continue;
                }

                String[] parts = line.split(":");

                if (parts.length == 2) {
                    String username = parts[0];
                    String passwordHash = parts[1];

                    credentials.put(username, passwordHash);
                }
            }

            reader.close();

        } catch (IOException e) {
            System.out.println("Error reading users file");
        }
    }

    // -------------------------------------------------------
    // Save new user to file
    // -------------------------------------------------------
    private void saveUser(String username, String hashedPassword) {

        try {

            PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE, true));

            writer.println(username + ":" + hashedPassword);

            writer.close();

        } catch (IOException e) {
            System.out.println("Error saving user");
        }
    }

    // -------------------------------------------------------
    // SHA-256 Hash Function
    // -------------------------------------------------------
    public static String hash(String input) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] bytes = md.digest(input.getBytes());

            String result = "";

            for (int i = 0; i < bytes.length; i++) {
                result += String.format("%02x", bytes[i]);
            }

            return result;

        } catch (NoSuchAlgorithmException e) {

            System.out.println("Hash algorithm error");
            return null;
        }
    }
}

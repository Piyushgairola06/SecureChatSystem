package server;

import java.util.HashMap;
import java.util.Collection;

public class ClientManager {

    // Stores username -> ClientHandler
    private HashMap<String, ClientHandler> clients;

    public ClientManager() {
        clients = new HashMap<String, ClientHandler>();
    }

    // -----------------------------------
    // Add new client
    // -----------------------------------
    public void addClient(String username, ClientHandler handler) {

        clients.put(username, handler);

        System.out.println("Client added: " + username);
        System.out.println("Total online users: " + clients.size());
    }

    // -----------------------------------
    // Remove client
    // -----------------------------------
    public void removeClient(String username) {

        clients.remove(username);

        System.out.println("Client removed: " + username);
        System.out.println("Total online users: " + clients.size());
    }

    // -----------------------------------
    // Get specific client
    // -----------------------------------
    public ClientHandler getClient(String username) {

        return clients.get(username);
    }

    // -----------------------------------
    // Get all clients
    // -----------------------------------
    public Collection<ClientHandler> getAllClients() {

        return clients.values();
    }

    // -----------------------------------
    // Check if user is online
    // -----------------------------------
    public boolean isOnline(String username) {

        return clients.containsKey(username);
    }
}

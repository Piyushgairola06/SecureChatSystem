package server;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {

    // ConcurrentHashMap for thread-safe multi-client access
    private ConcurrentHashMap<String, ClientHandler> clients;

    public ClientManager() {
        clients = new ConcurrentHashMap<>();
    }

    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        ServerLogger.log("[ClientManager] Added: " + username + " | Online: " + clients.size());
    }

    public void removeClient(String username) {
        clients.remove(username);
        ServerLogger.log("[ClientManager] Removed: " + username + " | Online: " + clients.size());
    }

    public ClientHandler getClient(String username) {
        return clients.get(username);
    }

    public Collection<ClientHandler> getAllClients() {
        return clients.values();
    }

    public boolean isOnline(String username) {
        return clients.containsKey(username);
    }
}
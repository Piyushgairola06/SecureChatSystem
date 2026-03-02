package server;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {

    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        System.out.println("[ClientManager] Added: " + username + " | Online: " + clients.size());
    }

    public void removeClient(String username) {
        clients.remove(username);
        System.out.println("[ClientManager] Removed: " + username + " | Online: " + clients.size());
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
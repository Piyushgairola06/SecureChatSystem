package server;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {

    private ConcurrentHashMap<String, ClientHandler> clients;

    public ClientManager() {
        clients = new ConcurrentHashMap<>();
    }

    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        ServerLogger.log("[ClientManager] + " + username
                + " | Online: " + clients.size());
    }

    public void removeClient(String username) {
        clients.remove(username);
        ServerLogger.log("[ClientManager] - " + username
                + " | Online: " + clients.size());
    }

    public ClientHandler getClient(String username) {
        return clients.get(username);
    }

    public Collection<ClientHandler> getAllClients() {
        return clients.values();
    }

    // Returns all currently online usernames
    public Set<String> getAllUsernames() {
        return clients.keySet();
    }

    public boolean isOnline(String username) {
        return clients.containsKey(username);
    }

    public int getClientCount() {
        return clients.size();
    }
}
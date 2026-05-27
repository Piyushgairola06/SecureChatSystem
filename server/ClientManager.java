package server;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {

    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        ServerLogger.log("[ClientManager] + " + username + " | online: " + clients.size());
    }

    public void removeClient(String username) {
        clients.remove(username);
        ServerLogger.log("[ClientManager] - " + username + " | online: " + clients.size());
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

    public int getCount() {
        return clients.size();
    }

    public String getOnlineList() {
        if (clients.isEmpty()) return "(nobody online)";
        return String.join(", ", clients.keySet());
    }
}

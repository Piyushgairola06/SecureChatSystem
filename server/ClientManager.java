package server;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClientManager — Thread-safe registry of all currently connected clients.
 *
 * Maintains a ConcurrentHashMap of username → ClientHandler for all active
 * connections. Used by ClientHandler (to add/remove users) and MessageRouter
 * (to look up recipients for routing).
 *
 * Why ConcurrentHashMap:
 * Multiple ClientHandler threads call addClient(), removeClient(), and
 * getAllClients() simultaneously. A plain HashMap is not thread-safe and
 * would risk data corruption or ConcurrentModificationException under
 * concurrent access. ConcurrentHashMap handles this internally without
 * needing external synchronization blocks.
 *
 * Author: Member A (Server & Concurrency)
 * Phase:  2 — Core Implementation
 */
public class ClientManager {

    // username → their ClientHandler (contains the output stream to reach them)
    private ConcurrentHashMap<String, ClientHandler> clients;

    public ClientManager() {
        clients = new ConcurrentHashMap<>();
    }

    /**
     * Registers a newly authenticated client.
     * Called by ClientHandler after successful login/register.
     */
    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        ServerLogger.log("[ClientManager] + " + username
                + " | Online: " + clients.size());
    }

    /**
     * Removes a client on disconnect.
     * Called by ClientHandler.disconnect().
     */
    public void removeClient(String username) {
        clients.remove(username);
        ServerLogger.log("[ClientManager] - " + username
                + " | Online: " + clients.size());
    }

    /**
     * Looks up a specific client by username.
     * Returns null if the user is not online.
     * Used by MessageRouter for private message delivery.
     */
    public ClientHandler getClient(String username) {
        return clients.get(username);
    }

    /**
     * Returns all active ClientHandlers.
     * Used by MessageRouter.broadcast() to send to everyone.
     */
    public Collection<ClientHandler> getAllClients() {
        return clients.values();
    }

    /**
     * Returns all online usernames.
     * Used by ClientHandler to respond to the /list command.
     */
    public Set<String> getAllUsernames() {
        return clients.keySet();
    }

    /**
     * Checks if a user is currently logged in.
     * Used by ClientHandler to prevent duplicate logins.
     */
    public boolean isOnline(String username) {
        return clients.containsKey(username);
    }

    /**
     * Returns the current number of connected clients.
     * Used by ServerMain to enforce the MAX_CLIENTS limit.
     */
    public int getClientCount() {
        return clients.size();
    }
}
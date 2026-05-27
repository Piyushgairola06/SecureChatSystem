# Integration Contract – Phase 2

This document defines how modules interact with each other.
All team members must follow these method signatures strictly.

---

# 1. ClientManager (Owned by Member A)

Purpose:
Maintain active connected clients using thread-safe storage.

Data Structure:
ConcurrentHashMap<String, ClientHandler> clients

Methods:

public void addClient(String username, ClientHandler handler)

public void removeClient(String username)

public ClientHandler getClient(String username)

public Collection<ClientHandler> getAllClients()

Notes:
- Only ClientManager modifies the map.
- No other module directly accesses the map.

# 2. AuthManager (Owned by Member C)

Purpose:
Validate login credentials.

Method:

public boolean authenticate(String username, String password)

Returns:
true  → if valid
false → if invalid

Notes:
- Uses users.txt for credential storage.
- Password hashing handled internally.
- No other module performs credential validation.


 # 3. MessageRouter (Owned by Member B)

Purpose:
Route messages to correct recipients.

Method:

public void routeMessage(String sender, String message)

Behavior:
- If message starts with "/msg":
    → Private message
- Otherwise:
    → Broadcast message

MessageRouter will use:

ClientManager.getClient(username)
ClientManager.getAllClients()

Notes:
- MessageRouter does not handle sockets directly.
- It uses ClientHandler.sendMessage() to send messages.

# 4. ClientHandler (Owned by Member A)

Purpose:
Handle communication for one client.

Important Methods:

public void sendMessage(String message)

private void processClientInput(String input)

Workflow:
1. Receive login credentials
2. Call AuthManager.authenticate()
3. If valid → Add to ClientManager
4. Continuously read messages
5. Pass messages to MessageRouter.routeMessage()
6. On disconnect → remove client from ClientManager

# 5. EncryptionUtil (Owned by Member C)

Purpose:
Provide encryption utilities.

Methods:

public static String encrypt(String message)

public static String decrypt(String encryptedMessage)

Notes:
- Used inside ClientHandler before sending messages.
- Router does not manage encryption logic.
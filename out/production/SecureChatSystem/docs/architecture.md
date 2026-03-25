# Secure Multi-Client Chat and File Sharing System
## System Architecture (Phase 2 – Academic Version)

---

## 1. Architecture Overview

The system follows a client–server architecture.

- A central server manages all communication.
- Multiple clients connect to the server using TCP sockets.
- Each client is handled by a dedicated thread.
- The server routes messages to appropriate recipients.

This design ensures concurrent communication and modular separation of responsibilities.

---

## 2. High-Level Components

### Server Side

1. ServerMain
   - Starts the server.
   - Listens for incoming client connections.
   - Creates a new ClientHandler thread per client.

2. ClientHandler
   - Manages communication with one client.
   - Handles login authentication.
   - Reads incoming messages.
   - Sends messages to other clients via MessageRouter.
   - Detects client disconnection.

3. ClientManager
   - Maintains active users.
   - Uses ConcurrentHashMap for thread-safe operations.
   - Provides lookup methods for message routing.

4. MessageRouter
   - Handles broadcast messaging.
   - Handles private messaging.
   - Adds formatting (username + timestamp).

5. AuthManager
   - Validates username/password.
   - Uses file-based storage (users.txt).
   - Implements password hashing.

6. EncryptionUtil
   - Provides AES encryption and decryption.
   - Ensures secure message transfer.

---

### Client Side

1. ClientMain
   - Connects to server.
   - Handles login.
   - Starts sender and receiver threads.

2. ClientSender
   - Sends user input to server.

3. ClientReceiver
   - Continuously listens for incoming messages.

---

## 3. Thread Model

The system uses a Thread-per-Client model:

- Each client connection is assigned a dedicated thread.
- This ensures parallel communication.
- Shared data structures use thread-safe collections.

---

## 4. Data Storage (Phase 2)

File-based storage is used:

- storage/users.txt → User credentials
- storage/chatlog.txt → Chat logs

Database integration is optional for future upgrade.

---

## 5. Design Principles

- Separation of Concerns
- Modular Design
- Thread Safety
- Scalability-ready architecture
- Upgrade path for internet deployment
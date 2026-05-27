# Secure Multi-Client Chat System

A console-based multi-client chat application built with Java TCP sockets.
Supports real-time messaging between multiple clients with AES-128 encryption
and SHA-256 password hashing.

---

## Team

| Member | Responsibility                                                                    |
|--------|-----------------------------------------------------------------------------------|
| Member A | Server core, concurrency, ClientHandler, ClientManager                            |
| Member B | Messaging & routing (MessageRouter)                                               |
| Member C | Security & authentication (AuthManager, EncryptionUtil) ,helped in client handler |
| Member D | Client application (ClientMain, ClientReceiver, ClientSender)                     |

---

## Project Structure

```
SecureChatSystem/
├── client/
│   ├── ClientMain.java        # Entry point, login/register UI
│   ├── ClientReceiver.java    # Thread: reads incoming messages
│   └── ClientSender.java      # Thread: sends user input
├── server/
│   ├── ServerMain.java        # Entry point, accepts connections
│   ├── ClientHandler.java     # Manages one client per thread
│   ├── ClientManager.java     # Registry of online users
│   ├── MessageRouter.java     # Broadcast and private messaging
│   ├── AuthManager.java       # Login, register, SHA-256 hashing
│   ├── EncryptionUtil.java    # AES-128 CBC encrypt/decrypt
│   └── ServerLogger.java      # Timestamped logging to file
├── storage/
│   ├── users.txt              # username:SHA256hash per line
│   └── chatlog.txt            # Timestamped server event log
├── out/                       # Compiled .class files (gitignored)
├── docs/
│   ├── architecture.md
│   ├── integration-contract.md
│   └── phase-plan.md
└── README.md
```

---

## Requirements

- Java JDK 11 or higher
- No external libraries — standard Java only

---

## How to Run

### 1. Compile

From the project root directory:

```bash
javac -d out server/*.java client/*.java
```

### 2. Start the Server

```bash
java -cp out server.ServerMain
```

You should see:
```
[2026-03-26 14:00:00] [Server] Starting on port 5000
[2026-03-26 14:00:00] [Server] Max clients allowed: 10
[2026-03-26 14:00:00] [Server] Ready. Waiting for connections...
```

### 3. Start a Client (open a new terminal for each client)

```bash
java -cp out client.ClientMain
```

Repeat in separate terminals to simulate multiple users.

---

## First Time Setup

`storage/users.txt` and `storage/chatlog.txt` must exist before running.
If they don't, create them:

```bash
# Windows
echo. > storage\users.txt
echo. > storage\chatlog.txt

# Mac/Linux
touch storage/users.txt storage/chatlog.txt
```

On first run, use **Register** to create an account.
Registered credentials are saved to `storage/users.txt` as:
```
username:sha256hashedpassword
```

---

## Usage

```
=== Secure Chat v2 ===
1. Login
2. Register
Choice: 2
Username: alice
Password: mypassword
Confirm Password: mypassword
[Client] Registered & connected!
[Client] /help for commands
```

### Commands

| Command | Description |
|---------|-------------|
| `hello everyone` | Broadcast to all other online users |
| `/msg <username> <text>` | Send a private message |
| `/list` | Show all currently online users |
| `/help` | Show available commands |
| `/quit` | Disconnect from the server |

---

## Architecture

**Client-Server model over TCP sockets.**

- `ServerMain` listens on port 5000, spawns one thread per client
- Each `ClientHandler` thread manages its client's full lifecycle
- `ClientManager` holds a `ConcurrentHashMap` of online users
- `MessageRouter` routes messages — broadcast skips the sender, `/msg` delivers privately
- All messages after login are AES-128 CBC encrypted
- Passwords are SHA-256 hashed before storage — never stored in plaintext

### Login Handshake Protocol

```
Server → Client : "LOGIN"
Client → Server : "LOGIN" or "REGISTER"
Client → Server : username
Client → Server : password
Server → Client : "OK" or "REJECT <reason>"
```

After `OK`, every message in both directions is AES encrypted.

---

## Security Notes

- AES key and IV are hardcoded in `EncryptionUtil.java` (acceptable for Phase 2)
- Credentials are sent in plaintext during the handshake before encryption is active
- Production upgrade path: TLS/SSL sockets to replace manual AES

---

## Known Limitations

- No message history — clients joining mid-conversation miss earlier messages
- File-based user storage with no write locking (suitable for academic use)
- No reconnection support — disconnect requires re-login
- AES key is shared and hardcoded — not a real key exchange

---

## Phase Plan

| Phase | Status        | Description |
|-------|---------------|-------------|
| Phase 1 | ✅ Complete    | Architecture design, module planning, GitHub setup |
| Phase 2 | ✅ Complete    | Core server, auth, encryption, messaging, console client |
| Phase 3 | Upcoming      | JavaFX GUI, file transfer, group channels, rate limiting |
| Post-eval | will consider | Cloud deployment (AWS/Oracle), public IP, resume version |
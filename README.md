# C2 Project

Demo Client-Server C2 system implemented in Java.

## Directory Structure

C2Exercise/
│
├─ src/
│   ├─ Server.java
│   ├─ Client.java
│   ├─ ClientHandler.java
│   └─ VigenereCipher.java
│
└─ bin/   (will create after compilation)


Quickstart:

Open a terminal in the project root directory and run:

# 1. Compile
javac -d bin src/*.java

# 2. Enter bin
cd bin

# 3. Start server (Make sure the server is started before running any clients)
java Server

# 3. Start client (other terminal)
java Client

Server CLI Commands:

show_clients_status 	        Displays the status of all connected clients (alive/dead).
kill_client <client_id>	        Terminates the client with the specified ID.
run <client_id> <bash command>	Executes a bash-style command on the client with the given ID.
exit	                        Stops the server and all threads gracefully.



*****
Note on Cross-Platform Support

Currently, command execution is implemented for Windows using PowerShell.
If additional development time were available, I would extend the system to fully support Linux and macOS using their native shells (e.g., /bin/bash or /bin/zsh) and implement automatic OS detection on the client side.
*****

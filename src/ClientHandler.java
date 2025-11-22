import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * Handles the connection and communication with a single client.
 *
 * This class runs on its own thread and is responsible for:
 * - Reading encrypted messages coming from the client
 * - Decrypting heartbeat messages and command outputs
 * - Sending encrypted commands from the server to the client
 * - Tracking whether the client is still alive using heartbeat timestamps
 * - Closing the connection and cleaning up when the client disconnects or is
 * killed
 *
 * Each connected client has exactly one ClientHandler instance.
 */

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final int id;
    private volatile boolean isAlive;
    private volatile long lastHeartbeatTime;

    private final PrintWriter out;
    private final BufferedReader in;

    private final VigenereCipher cipher = new VigenereCipher("secretkey");

    // Constructor: sets up I/O streams and initial state.
    public ClientHandler(Socket clientSocket, int id) {
        this.clientSocket = clientSocket;
        this.id = id;
        this.isAlive = true;
        this.lastHeartbeatTime = System.currentTimeMillis();

        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize client IO streams", e);
        }
    }

    // Returns true if socket is still open and the client hasn't been marked dead.
    public boolean isAlive() {
        return isAlive && !clientSocket.isClosed();
    }

    // Sends an encrypted command to the client not waiting for response -
    // non-blocking).
    public void sendCommand(String command) {
        if (!isAlive()) {
            System.out.println("Client " + id + " is not reachable");
            return;
        }
        out.println(cipher.encrypt(command));
    }

    // Forces the client connection to close and marks it as dead.
    public void kill() {
        try {
            isAlive = false;
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Called when a heartbeat is received to update the timestamp.
    public void updateHeartbeat() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    /**
     * Main loop of the handler:
     * - Reads encrypted messages from the client
     * - Decrypts them
     * - Detects heartbeat messages
     * - Prints command results sent by the client
     *
     * When the client disconnects or the socket closes, the handler removes itself
     * from the server’s clients list.
     */
    public void run() {
        try {
            String encryptdMessage;

            while ((encryptdMessage = in.readLine()) != null) {
                if (encryptdMessage.isEmpty()) {
                    continue;
                }
                String message = cipher.decrypt(encryptdMessage);
                if (message.equals("heartbeat")) {
                    updateHeartbeat();
                    continue; // optional for not printing heartbeat repeatedly
                }
                System.out.println("Response from client " + id + ": " + message);
            }

        } catch (SocketException e) {
            if (!isAlive) {
                System.out.println("Client " + id + " killed");
            } else {
                System.out.println("Client " + id + " disconnected");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isAlive = false;
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Server.clients.remove(id);
            }
        }
    }

    public int getId() {
        return this.id;
    }

    public String toString() {
        return "client ID = " + this.id + " Status = " + (this.isAlive ? "alive" : "dead");
    }
}
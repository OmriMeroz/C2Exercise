import java.io.*;
import java.net.*;



public class Client {

    static final VigenereCipher cipher = new VigenereCipher("secretkey");

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 5000)) {
            System.out.println("Connected to server");
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

            String encryptCommand;
            while ((encryptCommand = in.readLine()) != null) {
                String command = cipher.decrypt(encryptCommand);
                if (command.equals("KILL")) {
                    System.out.println("Server ordered termination. Closing client.");
                    socket.close();
                    break;
                } else if (command.equals("ECHO")){
                    System.out.println("Server ordered echo response from the client.");
                    String returnMsg = "Hello World";
                    System.out.println(returnMsg);
                    out.println(cipher.encrypt(returnMsg));
                    System.out.println("sending back to server " + returnMsg);

                }
            }
    
            socket.close();
        } catch (Exception e) {
            System.out.println("Server disconnected");
        } 
       


    }
}

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockchainClientRunnable implements Runnable {
    static ExecutorService executor = Executors.newFixedThreadPool(16);

    private String reply;
    private int serverNumber;
    private String serverName;
    private int portNumber;
    private String message;

    public BlockchainClientRunnable(int serverNumber, String serverName, int portNumber, String message) {
        this.serverNumber = serverNumber;
        this.serverName = serverName;
        this.portNumber = portNumber;
        this.message = message;

        this.reply = "Server" + serverNumber + ": " + serverName + " " + portNumber + "\n"; // header string
    }

    public void run() {
        try {
            Socket sck = new Socket(serverName, portNumber);
            sck.setSoTimeout(2000);
            if (sck.isBound()) {
                clientHandler(sck.getInputStream(), sck.getOutputStream(), message);
            }
        } catch (SocketTimeoutException e) {
            //TODO Handle timeout
            defError();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    public String getReply() {
        return reply;
    }

    private static void defError() {
        System.out.print("Server not available\n\n");
    }

    private void clientHandler(InputStream serverInputStream, OutputStream serverOutputStream, String message) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(serverInputStream));
        PrintWriter outWriter = new PrintWriter(serverOutputStream, true);
        try {
            // Send command to server
            outWriter.write(message + "\n\n");

            // Prepare for incoming
            char incomingChar;
            int newLineCount = 0;
            while ((incomingChar = ((char) inputReader.read())) != -1) {
                // Check for END-OF-TRANSMISSION signature
                if (incomingChar == '\n') {
                    newLineCount++;
                } else {
                    newLineCount = 0;
                }
                stringBuilder.append(incomingChar);
                if (newLineCount == 2) break;

                // Send 'terminate connection' command to server
                outWriter.write("cc\n\n");
            }
        } finally {
            outWriter.close();
            inputReader.close();
        }
        this.reply += stringBuilder.toString();
        return;
    }
}
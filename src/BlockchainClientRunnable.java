import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockchainClientRunnable implements Runnable {
    static ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(16);
    static ExecutorCompletionService<String> executor = new ExecutorCompletionService<>(threadPoolExecutor);

    private String reply;
    private String alt_message;
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
        this.alt_message = reply;
    }

    public void run() {
        Socket sck = new Socket();
        try {
            // Create new socket, timeout after 200 ms if not yet connected.
            // Timeout if connection is idle for 2000 ms.
            // Pre-condition undefined error state
            defError();
            sck.connect(new InetSocketAddress(serverName, portNumber), 200);
            sck.setSoTimeout(2000);
            if (sck.isBound()) {
                // Set valid state
                reply = alt_message;
                clientHandler(sck.getInputStream(), sck.getOutputStream(), message);
            }
        } catch (SocketTimeoutException e) {
            //e.printStackTrace();
        } catch (UnknownHostException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        } finally {
            try {
                sck.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return;
    }

    public String getReply() {
        return reply;
    }

    public String getAltReply() {
        return alt_message + "Server is not available\n\n";
    }

    private void defError() {
        //System.out.print("Server not available\n\n");
        reply += "Server is not available\n\n";
        return;
    }

    private void clientHandler(InputStream serverInputStream, OutputStream serverOutputStream, String message) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(serverInputStream));
        PrintWriter outWriter = new PrintWriter(serverOutputStream, true);
        try {
            // Send command to server
            outWriter.println(message);
            outWriter.flush();

            // Prepare for incoming
            char incomingChar;
            int newLineCount = 0;
            while ((incomingChar = (char) inputReader.read()) != -1) {
                // Check for END-OF-TRANSMISSION signature
                if (incomingChar == '\n') {
                    newLineCount++;
                } else {
                    newLineCount = 0;
                }
                stringBuilder.append(incomingChar);
                if (newLineCount == 2) break;

                // Send 'terminate connection' command to server
            }
            outWriter.println("cc");
            outWriter.flush();
        } finally {
            outWriter.close();
            inputReader.close();
        }
        this.reply += stringBuilder.toString();
        return;
    }
}
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Pattern;

public class BlockchainServerRunnable implements Runnable {

    private Socket clientSocket;
    private Blockchain blockchain;

    public BlockchainServerRunnable(Socket clientSocket, Blockchain blockchain) {
        this.clientSocket = clientSocket;
        this.blockchain = blockchain;

    }

    public void run() {
        if (BlockchainServer.FLAG_DEBUG) {
            System.out.println(String.format("Thread running ID: %d", Thread.currentThread().getId()));
        }

        try {
            clientSocket.setSoTimeout(2000);
            serverHandler(blockchain, clientSocket.getInputStream(), clientSocket.getOutputStream());
        } catch (IOException e) {
            if (BlockchainServer.FLAG_DEBUG) {
                System.err.println(String.format("IO Exception occurred! Thread ID: %d.", Thread.currentThread().getId()));
            }
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (BlockchainServer.FLAG_DEBUG) {
            System.out.println(String.format("Thread released ID: %d", Thread.currentThread().getId()));
        }
    }

    // implement any helper method here if you need any

    public void serverHandler(Blockchain blockchain, InputStream clientInputStream, OutputStream clientOutputStream) throws IOException {

        BufferedReader inputReader = new BufferedReader(
                new InputStreamReader(clientInputStream));
        PrintWriter outWriter = new PrintWriter(clientOutputStream, false);

        try {
            String line;
            boolean exitSignalReceived = false;

            while (!exitSignalReceived) {
                if ((line = inputReader.readLine()) != null) {
                    // Do stuff when there is data
                    String[] lineComponents = line.split(Pattern.quote("|"));
                    if (lineComponents.length >= 1) {
                        switch (lineComponents[0]) {
                            case "tx":
                                // Add transaction (if valid).
                                if (blockchain.addTransaction(line)) {
                                    if (BlockchainServer.FLAG_DEBUG) System.out.println("Successful TX");
                                    outWriter.print("Accepted\n\n");
                                    outWriter.flush();

                                } else {
                                    if (BlockchainServer.FLAG_DEBUG) {
                                        System.out.println(String.format("Unsuccessful TX: %s", line));
                                    }
                                    outWriter.print("Rejected\n\n");
                                    outWriter.flush();
                                }
                                break;
                            case "pb":
                                // Print current blockchain.
                                outWriter.print(blockchain.toString() + "\n");
                                outWriter.flush();
                                break;
                            case "cc":
                                // Close connection
                                if (BlockchainServer.FLAG_DEBUG) System.out.println("Client closed connection");
                                exitSignalReceived = true;
                                break;
                            default:
                                if (BlockchainServer.FLAG_DEBUG)
                                    System.out.println(String.format("Unknown Command %s", line));
                                outWriter.print("Error\n\n");
                                outWriter.flush();
                                break;
                        }
                    }
                } else {
                    if (BlockchainServer.FLAG_DEBUG) System.out.println("Null received");
                    break;
                }
            }
        } catch (SocketException se) {
            System.err.print(se.toString());
            System.err.println("Socket Exception: Connection was reset.");
        } finally {
            inputReader.close();
            outWriter.close();
        }

    }
}

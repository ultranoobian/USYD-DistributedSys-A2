import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BlockchainServer {
    public static boolean FLAG_DEBUG = false;

    public static void main(String[] args) {
        int portNumber;
        Blockchain blockchain = new Blockchain();

        // Check number of arguments, exits if too few
        if (args.length < 1) {
            System.out.print("Usage: BlockchainServer <port> [debug:true|false}");
            return;
        } else {
            // Try to parse port number
            try {
                portNumber = Integer.parseInt(args[0]);
                if (portNumber < 1024 || portNumber > 65535) {
                    System.out.print("Usage: BlockchainServer <port> [debug:true|false}");
                    return;
                }


            } catch (NumberFormatException nfe) {
                System.out.print("Error: Invalid port number!");
                return;
            }

            if (args.length == 2) {
                try {
                    FLAG_DEBUG = Boolean.parseBoolean(args[1]);
                    if (FLAG_DEBUG) System.out.println("Logging is enabled");
                } catch (ArrayIndexOutOfBoundsException outOfBounds) {
                    System.err.println(outOfBounds);
                }
            }
        }


        PeriodicCommitRunnable pcr = new PeriodicCommitRunnable(blockchain);
        Thread pct = new Thread(pcr);
        pct.start();

        // implement your code here
        Executor executor = Executors.newFixedThreadPool(32);
        Socket clientSocket;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            serverSocket.setSoTimeout(2000);
            if (FLAG_DEBUG) System.out.println(String.format("Attempting to start listening on port %s", portNumber));
            while ((clientSocket = serverSocket.accept()).isBound()) {
                Runnable ncs = new BlockchainServerRunnable(clientSocket, blockchain);
                executor.execute(ncs);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        // Do not modify below this line
        pcr.setRunning(false);
        try {
            pct.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    // implement any helper method here if you need any
}

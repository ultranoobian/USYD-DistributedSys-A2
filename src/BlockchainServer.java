import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BlockchainServer {

    public static void main(String[] args) {

        if (args.length != 1) {
            return;
        }

        int portNumber = Integer.parseInt(args[0]);
        Blockchain blockchain = new Blockchain();


        PeriodicCommitRunnable pcr = new PeriodicCommitRunnable(blockchain);
        Thread pct = new Thread(pcr);
        pct.start();

        // implement your code here
        pcr.setRunning(false);
        try {
            pct.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // implement any helper method here if you need any
}
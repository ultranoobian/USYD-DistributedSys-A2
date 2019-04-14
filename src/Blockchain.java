import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Blockchain {

    private Block head;
    private ArrayList<Transaction> pool;
    private int length;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Blockchain() {
        pool = new ArrayList<>();
        length = 0;
    }

    public Block getHead() {
        return head;
    }

    public void setHead(Block head) {
        this.head = head;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public ArrayList<Transaction> getPool() {
        return pool;
    }

    public void setPool(ArrayList<Transaction> pool) {
        this.pool = pool;
    }

    public boolean addTransaction(String txString) {
        String[] tokens = txString.split("\\|");
        if (tokens.length != 3) {
            return false;
        }
        if (!tokens[0].equals("tx")) {
            return false;
        }
        Transaction transaction = new Transaction();
        transaction.setSender(tokens[1]);
        transaction.setContent(tokens[2]);
        if (!transaction.isValid()) {
            return false;
        }
        pool.add(transaction);
        return true;
    }

    public boolean commit(int nonce) {
        if (pool.size() == 0) {
            return false;
        }

        Block newBlock = new Block();
        if (head == null) {
            newBlock.setPreviousHash(new byte[32]);
        } else {
            newBlock.setPreviousHash(head.calculateHash());
        }
        newBlock.setTransactions(pool);
        byte[] hash = newBlock.calculateHashWithNonce(nonce);
        String hashString = Base64.getEncoder().encodeToString(hash);
        if (hashString.startsWith("A")) {
            newBlock.setPreviousBlock(head);
            head = newBlock;
            pool = new ArrayList<>();
            length += 1;
            return true;
        }
        return false;
    }

    public String toString() {
        String cutOffRule;
        String poolString;
        String blockString;

        // Use a read-lock to avoid blockchain being modified while generating string
        lock.readLock().lock();
        try {
            cutOffRule = new String(new char[81]).replace("\0", "-") + "\n";
            poolString = "";
            for (Transaction tx : pool) {
                poolString += tx.toString();
            }

            blockString = "";
            Block bl = head;
            while (bl != null) {
                blockString += bl.toString();
                bl = bl.getPreviousBlock();
            }
        } finally {
            lock.readLock().unlock();
        }

        return "Pool:\n"
                + cutOffRule
                + poolString
                + cutOffRule
                + blockString;
    }
}

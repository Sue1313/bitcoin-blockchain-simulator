import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BitcoinSimulation {

    public static void main(String[] args) {
        Agent su = new Agent("Su", 100.0);
        Agent malak = new Agent("Malak", 50.0);
        Agent betul = new Agent("Betul", 30.0);

        Miner miner1 = new Miner("Miner1", 10.0);
        Blockchain blockchain = new Blockchain();

        su.sendCoins(malak, 20.0, blockchain);
        malak.sendCoins(betul, 10.0, blockchain);

        blockchain.processPendingTransactions();
        miner1.mine(blockchain);

        List<Block> blocks = blockchain.getBlocks();
        for (Block block : blocks) {
            String blockHash = block.calculateHash();
            System.out.println("Block Hash: " + blockHash);
        }
    }

    public static class Agent {
        private String name;
        private double balance;

        public Agent(String name, double balance) {
            this.name = name;
            this.balance = balance;
        }

        public String getName() {
            return name;
        }

        public double getBalance() {
            return balance;
        }

        public void sendCoins(Agent recipient, double amount, Blockchain blockchain) {
            if (this.balance >= amount) {
                this.balance -= amount;
                recipient.balance += amount;
                Transaction transaction = new Transaction(this.name, recipient.getName(), amount);
                blockchain.addPendingTransaction(transaction);
                System.out.println(this.name + " sent " + amount + " coins to " + recipient.getName());
            } else {
                System.out.println(this.name + " does not have sufficient balance to send coins.");
            }
        }
    }

    public static class Transaction {
        private String sender;
        private String recipient;
        private double amount;
        private boolean verified;

        public Transaction(String sender, String recipient, double amount) {
            this.sender = sender;
            this.recipient = recipient;
            this.amount = amount;
            this.verified = false;
        }

        public String getSender() {
            return sender;
        }

        public String getRecipient() {
            return recipient;
        }

        public double getAmount() {
            return amount;
        }

        public boolean isVerified() {
            return verified;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }
    }

    public static class Block {
        private List<Transaction> transactions;
        private String previousHash;
        private int nonce;

        public Block(List<Transaction> transactions, String previousHash, int nonce) {
            this.transactions = transactions;
            this.previousHash = previousHash;
            this.nonce = nonce;
        }

        public String calculateHash() {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                StringBuilder data = new StringBuilder();
                for (Transaction transaction : transactions) {
                    data.append(transaction.getSender())
                        .append(transaction.getRecipient())
                        .append(transaction.getAmount())
                        .append(transaction.isVerified());
                }
                String dataString = data.toString() + previousHash + nonce;
                byte[] hashBytes = digest.digest(dataString.getBytes(StandardCharsets.UTF_8));
                
                StringBuilder hash = new StringBuilder();
                for (byte b : hashBytes) {
                    hash.append(String.format("%02x", b));
                }
                return hash.toString();
            } catch (NoSuchAlgorithmException e) {
                return "";
            }
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }
    }

    public static class Blockchain {
        private List<Block> blocks;
        private Map<String, Transaction> pendingTransactions;

        public Blockchain() {
            this.blocks = new ArrayList<>();
            this.pendingTransactions = new HashMap<>();
            
            // Genesis Block
            Block genesisBlock = new Block(new ArrayList<>(), "0000", 0);
            this.blocks.add(genesisBlock);
        }

        public void addPendingTransaction(Transaction transaction) {
            this.pendingTransactions.put(transaction.getSender(), transaction);
        }

        public boolean processPendingTransactions() {
            List<Transaction> verifiedTransactions = new ArrayList<>();
            for (Transaction t : pendingTransactions.values()) {
                if (verifyTransaction(t)) {
                    verifiedTransactions.add(t);
                }
            }

            if (verifiedTransactions.isEmpty()) {
                return false;
            }

            Block lastBlock = getLastBlock();
            String previousHash = lastBlock.calculateHash();
            int nonce = 0;

            Block newBlock = new Block(verifiedTransactions, previousHash, nonce);
            while (!newBlock.calculateHash().startsWith("0000")) {
                nonce++;
                newBlock = new Block(verifiedTransactions, previousHash, nonce);
            }

            addBlock(newBlock);

            for (Transaction t : verifiedTransactions) {
                pendingTransactions.remove(t.getSender());
            }

            return true;
        }

        public boolean verifyTransaction(Transaction transaction) {
            if (getBalance(transaction.getSender()) >= transaction.getAmount()) {
                transaction.setVerified(true);
                return true;
            }
            return false;
        }

        public double getBalance(String agentName) {
            double balance = 0.0;
            for (Block block : blocks) {
                for (Transaction t : block.getTransactions()) {
                    if (t.getSender().equals(agentName)) {
                        balance -= t.getAmount();
                    }
                    if (t.getRecipient().equals(agentName)) {
                        balance += t.getAmount();
                    }
                }
            }
            return balance;
        }

        public Block getLastBlock() {
            return blocks.get(blocks.size() - 1);
        }

        public void addBlock(Block block) {
            this.blocks.add(block);
        }

        public List<Block> getBlocks() {
            return blocks;
        }
    }

    public static class Miner {
        private String name;
        private double reward;

        public Miner(String name, double reward) {
            this.name = name;
            this.reward = reward;
        }

        public String getName() {
            return name;
        }

        public double getReward() {
            return reward;
        }

        public void mine(Blockchain blockchain) {
            List<Transaction> transactions = new ArrayList<>(blockchain.pendingTransactions.values());
            Block lastBlock = blockchain.getLastBlock();
            String previousHash = lastBlock.calculateHash();
            int nonce = 0;

            Block newBlock = new Block(transactions, previousHash, nonce);
            while (!newBlock.calculateHash().startsWith("0000")) {
                nonce++;
                newBlock = new Block(transactions, previousHash, nonce);
            }

            Transaction rewardTransaction = new Transaction("SYSTEM", this.name, this.reward);
            transactions.add(rewardTransaction);
            
            blockchain.addBlock(newBlock);
            blockchain.pendingTransactions.clear();

            System.out.println("New block mined by " + this.name);
        }
    }
}

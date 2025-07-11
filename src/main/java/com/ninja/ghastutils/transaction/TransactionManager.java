package com.ninja.ghastutils.transaction;

import com.ninja.ghastutils.utils.LogManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TransactionManager {
    private final Map<String, Transaction> transactions = new HashMap<>();
    private final Map<UUID, Map<String, Transaction>> playerTransactions = new HashMap<>();

    public void registerTransaction(Transaction transaction) {
        this.transactions.put(transaction.getId(), transaction);
        UUID playerId = transaction.getPlayerId();
        this.playerTransactions.computeIfAbsent(playerId, k -> new HashMap<>()).put(transaction.getId(), transaction);
        String transactionId = transaction.getId();
        LogManager.debug("Registered transaction: " + transactionId + " for player " + String.valueOf(playerId));
    }

    public Transaction getTransaction(String id) {
        return this.transactions.get(id);
    }

    public Map<String, Transaction> getPlayerTransactions(UUID playerId) {
        return this.playerTransactions.getOrDefault(playerId, new HashMap<>());
    }

    public boolean completeTransaction(String id, boolean success, String message) {
        Transaction transaction = this.transactions.get(id);
        if (transaction == null) {
            return false;
        } else {
            transaction.complete(success, message);
            return true;
        }
    }

    public void cleanup(long maxAge) {
        long cutoff = System.currentTimeMillis() - maxAge;
        int removed = 0;

        for (Map.Entry<String, Transaction> entry : new HashMap<>(this.transactions).entrySet()) {
            Transaction transaction = entry.getValue();
            if (transaction.isCompleted() && transaction.getTimestamp() < cutoff) {
                this.transactions.remove(entry.getKey());
                UUID playerId = transaction.getPlayerId();
                Map<String, Transaction> playerTxs = this.playerTransactions.get(playerId);
                if (playerTxs != null) {
                    playerTxs.remove(entry.getKey());
                    if (playerTxs.isEmpty()) {
                        this.playerTransactions.remove(playerId);
                    }
                }

                ++removed;
            }
        }

        if (removed > 0) {
            LogManager.debug("Cleaned up " + removed + " old transactions");
        }
    }

    public int getTransactionCount() {
        return this.transactions.size();
    }

    public int getPlayerCount() {
        return this.playerTransactions.size();
    }
}
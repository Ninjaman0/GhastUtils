
package com.ninja.ghastutils.transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Transaction {
    private final String id;
    private final UUID playerId;
    private final TransactionType type;
    private final double amount;
    private final String source;
    private final long timestamp;
    private final Map<String, String> metadata;
    private boolean completed;
    private boolean successful;
    private String message;
    private long completionTime;

    public Transaction(UUID playerId, TransactionType type, double amount, String source) {
        this.id = this.generateId(playerId);
        this.playerId = playerId;
        this.type = type;
        this.amount = amount;
        this.source = source;
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap();
        this.completed = false;
        this.successful = false;
        this.message = "";
    }

    private String generateId(UUID playerId) {
        String var10000 = playerId.toString().substring(0, 8);
        return var10000 + "-" + System.currentTimeMillis() + "-" + (int)(Math.random() * (double)1000.0F);
    }

    public String getId() {
        return this.id;
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public TransactionType getType() {
        return this.type;
    }

    public double getAmount() {
        return this.amount;
    }

    public String getSource() {
        return this.source;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public boolean isSuccessful() {
        return this.successful;
    }

    public String getMessage() {
        return this.message;
    }

    public long getCompletionTime() {
        return this.completionTime;
    }

    public void complete(boolean successful, String message) {
        this.completed = true;
        this.successful = successful;
        this.message = message;
        this.completionTime = System.currentTimeMillis();
    }

    public void setMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    public String getMetadata(String key) {
        return (String)this.metadata.get(key);
    }

    public boolean hasMetadata(String key) {
        return this.metadata.containsKey(key);
    }

    public Map<String, String> getAllMetadata() {
        return new HashMap(this.metadata);
    }
}

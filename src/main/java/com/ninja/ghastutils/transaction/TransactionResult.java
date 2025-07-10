
package com.ninja.ghastutils.transaction;

public class TransactionResult {
    private final boolean success;
    private final String message;
    private final double amount;
    private String transactionId;

    public TransactionResult(boolean success, String message, double amount) {
        this.success = success;
        this.message = message;
        this.amount = amount;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public double getAmount() {
        return this.amount;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}

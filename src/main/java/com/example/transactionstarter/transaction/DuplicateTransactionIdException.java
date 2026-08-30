package com.example.transactionstarter.transaction;

public class DuplicateTransactionIdException extends RuntimeException {

    public DuplicateTransactionIdException(String transactionId) {
        super("Transaction with ID already exists: " + transactionId);
    }
}

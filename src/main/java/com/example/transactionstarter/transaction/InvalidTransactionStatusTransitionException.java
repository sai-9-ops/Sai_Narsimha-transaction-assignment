package com.example.transactionstarter.transaction;

public class InvalidTransactionStatusTransitionException extends RuntimeException {

    public InvalidTransactionStatusTransitionException(TransactionStatus from, TransactionStatus to) {
        super("Invalid status transition: cannot transition from " + from + " to " + to);
    }
}

package com.example.transactionstarter.transaction;

import jakarta.validation.constraints.NotNull;

public class UpdateTransactionStatusRequest {

    @NotNull(message = "Status is required")
    private TransactionStatus status;

    public UpdateTransactionStatusRequest() {
    }

    public UpdateTransactionStatusRequest(TransactionStatus status) {
        this.status = status;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
}

package com.example.transactionstarter.transaction;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateTransactionRequest {

    @NotBlank(message = "Transaction ID is required")
    @Size(min = 1, max = 100, message = "Transaction ID must be between 1 and 100 characters")
    private String transactionId;

    @NotBlank(message = "Customer ID is required")
    @Size(min = 1, max = 100, message = "Customer ID must be between 1 and 100 characters")
    private String customerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 integer digits and 4 fractional digits")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO 4217 code (e.g., USD, EUR)")
    private String currency;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    public CreateTransactionRequest() {
    }

    public CreateTransactionRequest(String transactionId, String customerId, BigDecimal amount,
                                    String currency, TransactionType transactionType) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.transactionType = transactionType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
}

package com.example.transactionstarter.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class TransactionService {

    private static final Set<TransactionStatus> TERMINAL_STATUSES = Set.of(
            TransactionStatus.COMPLETED,
            TransactionStatus.FAILED,
            TransactionStatus.CANCELLED
    );

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        if (transactionRepository.existsByTransactionId(request.getTransactionId())) {
            throw new DuplicateTransactionIdException(request.getTransactionId());
        }

        validateBusinessRules(request);

        Transaction transaction = new Transaction(
                request.getTransactionId(),
                request.getCustomerId(),
                request.getAmount(),
                request.getCurrency(),
                request.getTransactionType(),
                TransactionStatus.PENDING
        );

        Transaction saved = transactionRepository.save(transaction);
        return new TransactionResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return new TransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransactionStatus(String transactionId, UpdateTransactionStatusRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        TransactionStatus currentStatus = transaction.getStatus();
        TransactionStatus newStatus = request.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        transaction.setStatus(newStatus);
        transaction.setUpdatedAt(java.time.LocalDateTime.now());

        Transaction updated = transactionRepository.save(transaction);
        return new TransactionResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getCustomerTransactions(String customerId) {
        List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return transactions.stream()
                .map(TransactionResponse::new)
                .toList();
    }

    private void validateBusinessRules(CreateTransactionRequest request) {
        if (request.getTransactionType() == TransactionType.WITHDRAWAL
                || request.getTransactionType() == TransactionType.TRANSFER) {
            if (request.getCustomerId().startsWith("TEST") && request.getAmount().intValue() > 1000000) {
                throw new IllegalArgumentException("Test customers cannot make withdrawals/transfers over 1,000,000");
            }
        }
    }

    private void validateStatusTransition(TransactionStatus from, TransactionStatus to) {
        if (TERMINAL_STATUSES.contains(from)) {
            throw new InvalidTransactionStatusTransitionException(from, to);
        }

        if (from == TransactionStatus.PROCESSING && to == TransactionStatus.PENDING) {
            throw new InvalidTransactionStatusTransitionException(from, to);
        }

        if (from == to) {
            throw new InvalidTransactionStatusTransitionException(from, to);
        }
    }
}

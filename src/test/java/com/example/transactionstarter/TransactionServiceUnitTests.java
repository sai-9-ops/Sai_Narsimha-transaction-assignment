package com.example.transactionstarter;

import com.example.transactionstarter.transaction.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceUnitTests {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository);
    }

    @Nested
    @DisplayName("Create Transaction Tests")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should create transaction with PENDING status when valid")
        void createTransaction_ValidRequest_SetsPendingStatus() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-100",
                    "CUST-001",
                    new BigDecimal("250.50"),
                    "USD",
                    TransactionType.DEPOSIT
            );

            when(transactionRepository.existsByTransactionId("TXN-100")).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            TransactionResponse response = transactionService.createTransaction(request);

            ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(transactionCaptor.capture());
            Transaction saved = transactionCaptor.getValue();

            assertEquals("TXN-100", response.getTransactionId());
            assertEquals("CUST-001", response.getCustomerId());
            assertEquals(TransactionStatus.PENDING, response.getStatus());
            assertEquals(TransactionType.DEPOSIT, response.getTransactionType());
            assertEquals(new BigDecimal("250.50"), response.getAmount());
            assertEquals("USD", response.getCurrency());
            assertNotNull(response.getCreatedAt());

            assertEquals(TransactionStatus.PENDING, saved.getStatus());
            assertEquals("TXN-100", saved.getTransactionId());
        }

        @Test
        @DisplayName("Should throw exception when transaction ID already exists")
        void createTransaction_DuplicateId_ThrowsException() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-DUP",
                    "CUST-002",
                    new BigDecimal("100.00"),
                    "EUR",
                    TransactionType.PAYMENT
            );

            when(transactionRepository.existsByTransactionId("TXN-DUP")).thenReturn(true);

            assertThrows(DuplicateTransactionIdException.class,
                    () -> transactionService.createTransaction(request));

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("All transaction types should be supported")
        void createTransaction_AllTypes_Supported() {
            TransactionType[] types = TransactionType.values();
            for (TransactionType type : types) {
                CreateTransactionRequest request = new CreateTransactionRequest(
                        "TXN-" + type,
                        "CUST-ALL",
                        new BigDecimal("50.00"),
                        "USD",
                        type
                );

                when(transactionRepository.existsByTransactionId(anyString())).thenReturn(false);
                when(transactionRepository.save(any(Transaction.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                TransactionResponse response = transactionService.createTransaction(request);
                assertEquals(type, response.getTransactionType());
            }

            verify(transactionRepository, times(types.length)).save(any());
        }
    }

    @Nested
    @DisplayName("Get Transaction Tests")
    class GetTransactionTests {

        @Test
        @DisplayName("Should return transaction when it exists")
        void getTransaction_ExistingId_ReturnsTransaction() {
            Transaction transaction = new Transaction(
                    "TXN-GET",
                    "CUST-GET",
                    new BigDecimal("999.99"),
                    "GBP",
                    TransactionType.TRANSFER,
                    TransactionStatus.COMPLETED
            );

            when(transactionRepository.findById("TXN-GET")).thenReturn(Optional.of(transaction));

            TransactionResponse response = transactionService.getTransaction("TXN-GET");

            assertEquals("TXN-GET", response.getTransactionId());
            assertEquals("CUST-GET", response.getCustomerId());
            assertEquals(TransactionStatus.COMPLETED, response.getStatus());
            assertEquals("GBP", response.getCurrency());
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void getTransaction_NonExistingId_ThrowsException() {
            when(transactionRepository.findById("NON-EXIST")).thenReturn(Optional.empty());

            assertThrows(TransactionNotFoundException.class,
                    () -> transactionService.getTransaction("NON-EXIST"));
        }
    }

    @Nested
    @DisplayName("Update Transaction Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should allow PENDING -> PROCESSING -> COMPLETED transitions")
        void updateStatus_ValidTransitions_Succeeds() {
            Transaction transaction = new Transaction(
                    "TXN-TRANS",
                    "CUST-TRANS",
                    new BigDecimal("500.00"),
                    "JPY",
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.PENDING
            );

            when(transactionRepository.findById("TXN-TRANS")).thenReturn(Optional.of(transaction));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            TransactionResponse r1 = transactionService.updateTransactionStatus(
                    "TXN-TRANS", new UpdateTransactionStatusRequest(TransactionStatus.PROCESSING));
            assertEquals(TransactionStatus.PROCESSING, r1.getStatus());

            TransactionResponse r2 = transactionService.updateTransactionStatus(
                    "TXN-TRANS", new UpdateTransactionStatusRequest(TransactionStatus.COMPLETED));
            assertEquals(TransactionStatus.COMPLETED, r2.getStatus());

            verify(transactionRepository, times(2)).save(transaction);
        }

        @Test
        @DisplayName("Should not allow transition from terminal COMPLETED status")
        void updateStatus_TerminalStatusCompleted_ThrowsException() {
            Transaction transaction = new Transaction(
                    "TXN-DONE",
                    "CUST-DONE",
                    new BigDecimal("100.00"),
                    "CAD",
                    TransactionType.DEPOSIT,
                    TransactionStatus.COMPLETED
            );

            when(transactionRepository.findById("TXN-DONE")).thenReturn(Optional.of(transaction));

            assertThrows(InvalidTransactionStatusTransitionException.class,
                    () -> transactionService.updateTransactionStatus(
                            "TXN-DONE", new UpdateTransactionStatusRequest(TransactionStatus.PENDING)));

            assertThrows(InvalidTransactionStatusTransitionException.class,
                    () -> transactionService.updateTransactionStatus(
                            "TXN-DONE", new UpdateTransactionStatusRequest(TransactionStatus.PROCESSING)));

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not allow transition from terminal FAILED status")
        void updateStatus_TerminalStatusFailed_ThrowsException() {
            Transaction transaction = new Transaction(
                    "TXN-FAIL",
                    "CUST-FAIL",
                    new BigDecimal("200.00"),
                    "AUD",
                    TransactionType.PAYMENT,
                    TransactionStatus.FAILED
            );

            when(transactionRepository.findById("TXN-FAIL")).thenReturn(Optional.of(transaction));

            assertThrows(InvalidTransactionStatusTransitionException.class,
                    () -> transactionService.updateTransactionStatus(
                            "TXN-FAIL", new UpdateTransactionStatusRequest(TransactionStatus.COMPLETED)));
        }

        @Test
        @DisplayName("Should not allow same status transition")
        void updateStatus_SameStatus_ThrowsException() {
            Transaction transaction = new Transaction(
                    "TXN-SAME",
                    "CUST-SAME",
                    new BigDecimal("300.00"),
                    "CHF",
                    TransactionType.REFUND,
                    TransactionStatus.PENDING
            );

            when(transactionRepository.findById("TXN-SAME")).thenReturn(Optional.of(transaction));

            assertThrows(InvalidTransactionStatusTransitionException.class,
                    () -> transactionService.updateTransactionStatus(
                            "TXN-SAME", new UpdateTransactionStatusRequest(TransactionStatus.PENDING)));
        }

        @Test
        @DisplayName("Should not allow PROCESSING -> PENDING (backwards)")
        void updateStatus_ProcessingToPending_ThrowsException() {
            Transaction transaction = new Transaction(
                    "TXN-BACK",
                    "CUST-BACK",
                    new BigDecimal("400.00"),
                    "SGD",
                    TransactionType.TRANSFER,
                    TransactionStatus.PROCESSING
            );

            when(transactionRepository.findById("TXN-BACK")).thenReturn(Optional.of(transaction));

            assertThrows(InvalidTransactionStatusTransitionException.class,
                    () -> transactionService.updateTransactionStatus(
                            "TXN-BACK", new UpdateTransactionStatusRequest(TransactionStatus.PENDING)));
        }

        @Test
        @DisplayName("Should throw exception for non-existing transaction")
        void updateStatus_NonExistingTransaction_ThrowsException() {
            when(transactionRepository.findById("TXN-MISSING")).thenReturn(Optional.empty());

            assertThrows(TransactionNotFoundException.class,
                    () -> transactionService.updateTransactionStatus(
                            "TXN-MISSING", new UpdateTransactionStatusRequest(TransactionStatus.COMPLETED)));
        }
    }

    @Nested
    @DisplayName("Get Customer Transactions Tests")
    class GetCustomerTransactionsTests {

        @Test
        @DisplayName("Should return transactions ordered by created at desc")
        void getCustomerTransactions_ExistingCustomer_ReturnsOrdered() {
            String customerId = "CUST-MULTI";
            Transaction t1 = new Transaction("TXN-A", customerId, new BigDecimal("100"), "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING);
            Transaction t2 = new Transaction("TXN-B", customerId, new BigDecimal("200"), "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING);

            when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                    .thenReturn(List.of(t2, t1));

            List<TransactionResponse> responses = transactionService.getCustomerTransactions(customerId);

            assertEquals(2, responses.size());
            assertEquals("TXN-B", responses.get(0).getTransactionId());
            assertEquals("TXN-A", responses.get(1).getTransactionId());
        }

        @Test
        @DisplayName("Should return empty list for customer with no transactions")
        void getCustomerTransactions_NoTransactions_ReturnsEmpty() {
            when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc("CUST-NONE"))
                    .thenReturn(List.of());

            List<TransactionResponse> responses = transactionService.getCustomerTransactions("CUST-NONE");

            assertTrue(responses.isEmpty());
        }

        @Test
        @DisplayName("Should not return transactions for other customers")
        void getCustomerTransactions_IsolatedByCustomerId() {
            String customerA = "CUST-A";
            String customerB = "CUST-B";

            Transaction tA = new Transaction("TXN-A", customerA, new BigDecimal("50"), "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING);
            Transaction tB = new Transaction("TXN-B", customerB, new BigDecimal("75"), "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING);

            when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerA)).thenReturn(List.of(tA));
            when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerB)).thenReturn(List.of(tB));

            List<TransactionResponse> respA = transactionService.getCustomerTransactions(customerA);
            List<TransactionResponse> respB = transactionService.getCustomerTransactions(customerB);

            assertEquals(1, respA.size());
            assertEquals(customerA, respA.get(0).getCustomerId());

            assertEquals(1, respB.size());
            assertEquals(customerB, respB.get(0).getCustomerId());

            verify(transactionRepository).findByCustomerIdOrderByCreatedAtDesc(customerA);
            verify(transactionRepository).findByCustomerIdOrderByCreatedAtDesc(customerB);
        }
    }

    @Test
    @DisplayName("Exception messages should contain useful context")
    void exceptionMessages_UsefulContext() {
        TransactionNotFoundException notFound = new TransactionNotFoundException("TXN-123");
        assertTrue(notFound.getMessage().contains("TXN-123"));

        DuplicateTransactionIdException dup = new DuplicateTransactionIdException("TXN-456");
        assertTrue(dup.getMessage().contains("TXN-456"));

        InvalidTransactionStatusTransitionException inv =
                new InvalidTransactionStatusTransitionException(TransactionStatus.COMPLETED, TransactionStatus.PENDING);
        assertTrue(inv.getMessage().contains("COMPLETED"));
        assertTrue(inv.getMessage().contains("PENDING"));
    }
}

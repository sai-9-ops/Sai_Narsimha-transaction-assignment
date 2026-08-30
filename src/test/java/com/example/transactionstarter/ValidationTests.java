package com.example.transactionstarter;

import com.example.transactionstarter.transaction.*;
import jakarta.validation.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void close() {
        validatorFactory.close();
    }

    @Nested
    @DisplayName("CreateTransactionRequest Validation")
    class CreateTransactionRequestValidationTests {

        @Test
        @DisplayName("Valid request passes all validations")
        void validRequest_NoViolations() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-VALID-001",
                    "CUST-001",
                    new BigDecimal("100.00"),
                    "USD",
                    TransactionType.DEPOSIT
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Expected no violations but got: " + violations);
        }

        @Test
        @DisplayName("Blank transaction ID fails validation")
        void blankTransactionId_Violation() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "",
                    "CUST-001",
                    new BigDecimal("100.00"),
                    "USD",
                    TransactionType.DEPOSIT
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("transactionId")));
        }

        @Test
        @DisplayName("Null transaction ID fails validation")
        void nullTransactionId_Violation() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    null,
                    "CUST-001",
                    new BigDecimal("100.00"),
                    "USD",
                    TransactionType.DEPOSIT
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Blank customer ID fails validation")
        void blankCustomerId_Violation() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-001",
                    "   ",
                    new BigDecimal("100.00"),
                    "USD",
                    TransactionType.DEPOSIT
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("customerId")));
        }

        @Test
        @DisplayName("Null amount fails validation")
        void nullAmount_Violation() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-001",
                    "CUST-001",
                    null,
                    "USD",
                    TransactionType.DEPOSIT
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
        }

        @Test
        @DisplayName("Zero amount fails validation")
        void zeroAmount_Violation() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-001",
                    "CUST-001",
                    BigDecimal.ZERO,
                    "USD",
                    TransactionType.DEPOSIT
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("greater than zero")));
        }

        @Test
        @DisplayName("Negative amount fails validation")
        void negativeAmount_Violation() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-001",
                    "CUST-001",
                    new BigDecimal("-50.00"),
                    "USD",
                    TransactionType.DEPOSIT
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Blank currency fails validation")
        void blankCurrency_Violation() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-001",
                    "CUST-001",
                    new BigDecimal("100.00"),
                    "",
                    TransactionType.DEPOSIT
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Invalid currency format (lowercase) fails validation")
        void lowercaseCurrency_Violation() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-001",
                    "CUST-001",
                    new BigDecimal("100.00"),
                    "usd",
                    TransactionType.DEPOSIT
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getMessage().contains("ISO 4217")));
        }

        @Test
        @DisplayName("Invalid currency format (too long) fails validation")
        void tooLongCurrency_Violation() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-001",
                    "CUST-001",
                    new BigDecimal("100.00"),
                    "USDA",
                    TransactionType.DEPOSIT
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Null transaction type fails validation")
        void nullTransactionType_Violation() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-001",
                    "CUST-001",
                    new BigDecimal("100.00"),
                    "USD",
                    null
            );

            Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("transactionType")));
        }

        @Test
        @DisplayName("All ISO 4217-like 3-letter uppercase currencies are valid")
        void validCurrencies_NoViolations() {
            String[] currencies = {"USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR"};
            for (String currency : currencies) {
                CreateTransactionRequest request = new CreateTransactionRequest(
                        "TXN-" + currency,
                        "CUST-001",
                        new BigDecimal("100.00"),
                        currency,
                        TransactionType.DEPOSIT
                );
                Set<ConstraintViolation<CreateTransactionRequest>> violations = validator.validate(request);
                assertTrue(violations.isEmpty(), "Currency " + currency + " should be valid");
            }
        }
    }

    @Nested
    @DisplayName("UpdateTransactionStatusRequest Validation")
    class UpdateStatusRequestValidationTests {

        @Test
        @DisplayName("Valid request with status passes")
        void validStatus_NoViolations() {
            for (TransactionStatus status : TransactionStatus.values()) {
                UpdateTransactionStatusRequest request = new UpdateTransactionStatusRequest(status);
                Set<ConstraintViolation<UpdateTransactionStatusRequest>> violations = validator.validate(request);
                assertTrue(violations.isEmpty(), "Status " + status + " should be valid");
            }
        }

        @Test
        @DisplayName("Null status fails validation")
        void nullStatus_Violation() {
            UpdateTransactionStatusRequest request = new UpdateTransactionStatusRequest(null);
            Set<ConstraintViolation<UpdateTransactionStatusRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Transaction Entity Tests")
    class TransactionEntityTests {

        @Test
        @DisplayName("Entity equality based on transaction ID")
        void equality_BasedOnTransactionId() {
            Transaction t1 = new Transaction("ID-1", "CUST-1", new BigDecimal("100"), "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING);
            Transaction t2 = new Transaction("ID-1", "CUST-DIFFERENT", new BigDecimal("999"), "EUR", TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED);
            Transaction t3 = new Transaction("ID-2", "CUST-1", new BigDecimal("100"), "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING);

            assertEquals(t1, t2, "Same ID should be equal");
            assertNotEquals(t1, t3, "Different IDs should not be equal");
            assertEquals(t1.hashCode(), t2.hashCode(), "Same ID should have same hash");
            assertNotEquals(t1.hashCode(), t3.hashCode(), "Different IDs may have different hash");
        }

        @Test
        @DisplayName("Constructor initializes fields correctly")
        void constructor_InitializesFields() {
            Transaction t = new Transaction("TXN-NEW", "CUST-NEW",
                    new BigDecimal("500.55"), "GBP",
                    TransactionType.TRANSFER, TransactionStatus.PENDING);

            assertEquals("TXN-NEW", t.getTransactionId());
            assertEquals("CUST-NEW", t.getCustomerId());
            assertEquals(new BigDecimal("500.55"), t.getAmount());
            assertEquals("GBP", t.getCurrency());
            assertEquals(TransactionType.TRANSFER, t.getTransactionType());
            assertEquals(TransactionStatus.PENDING, t.getStatus());
            assertNotNull(t.getCreatedAt());
        }
    }
}

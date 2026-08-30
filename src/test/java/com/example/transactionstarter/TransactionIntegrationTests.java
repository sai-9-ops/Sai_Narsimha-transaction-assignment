package com.example.transactionstarter;

import com.example.transactionstarter.transaction.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @Order(1)
    void createTransaction_ValidData_Returns201AndPersists() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-001",
                "CUST-100",
                new BigDecimal("150.75"),
                "USD",
                TransactionType.DEPOSIT
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-001"))
                .andExpect(jsonPath("$.customerId").value("CUST-100"))
                .andExpect(jsonPath("$.amount").value(150.75))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists());

        assertTrue(transactionRepository.existsByTransactionId("TXN-001"));
    }

    @Test
    @Order(2)
    void createTransaction_DuplicateId_Returns409() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-DUP",
                "CUST-200",
                new BigDecimal("50.00"),
                "EUR",
                TransactionType.PAYMENT
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @Order(3)
    void createTransaction_InvalidValidation_Returns400() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "",
                "",
                new BigDecimal("-10"),
                "us",
                null
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @Order(4)
    void getTransaction_ExistingId_ReturnsTransaction() throws Exception {
        CreateTransactionRequest createRequest = new CreateTransactionRequest(
                "TXN-GET-001",
                "CUST-300",
                new BigDecimal("500.00"),
                "GBP",
                TransactionType.TRANSFER
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions/TXN-GET-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN-GET-001"))
                .andExpect(jsonPath("$.customerId").value("CUST-300"))
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"));
    }

    @Test
    @Order(5)
    void getTransaction_NonExistingId_Returns404() throws Exception {
        mockMvc.perform(get("/api/transactions/TXN-NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    @Order(6)
    void updateTransactionStatus_ValidTransition_ReturnsUpdated() throws Exception {
        CreateTransactionRequest createRequest = new CreateTransactionRequest(
                "TXN-UPD-001",
                "CUST-400",
                new BigDecimal("1000.00"),
                "JPY",
                TransactionType.WITHDRAWAL
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        UpdateTransactionStatusRequest updateRequest =
                new UpdateTransactionStatusRequest(TransactionStatus.PROCESSING);

        mockMvc.perform(patch("/api/transactions/TXN-UPD-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.updatedAt").exists());

        UpdateTransactionStatusRequest completeRequest =
                new UpdateTransactionStatusRequest(TransactionStatus.COMPLETED);

        mockMvc.perform(patch("/api/transactions/TXN-UPD-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(7)
    void updateTransactionStatus_FromTerminalStatus_Returns400() throws Exception {
        CreateTransactionRequest createRequest = new CreateTransactionRequest(
                "TXN-TERM-001",
                "CUST-500",
                new BigDecimal("250.00"),
                "CAD",
                TransactionType.REFUND
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        UpdateTransactionStatusRequest completeRequest =
                new UpdateTransactionStatusRequest(TransactionStatus.COMPLETED);
        mockMvc.perform(patch("/api/transactions/TXN-TERM-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk());

        UpdateTransactionStatusRequest invalidRequest =
                new UpdateTransactionStatusRequest(TransactionStatus.PENDING);
        mockMvc.perform(patch("/api/transactions/TXN-TERM-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid status transition")));
    }

    @Test
    @Order(8)
    void getCustomerTransactions_MultipleTransactions_ReturnsOrderedList() throws Exception {
        String customerId = "CUST-MULTI";

        for (int i = 1; i <= 3; i++) {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "TXN-LIST-" + i,
                    customerId,
                    new BigDecimal("100.00").add(new BigDecimal(i * 10)),
                    "USD",
                    TransactionType.DEPOSIT
            );
            mockMvc.perform(post("/api/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
            Thread.sleep(10);
        }

        MvcResult result = mockMvc.perform(get("/api/transactions")
                        .param("customerId", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].customerId").value(customerId))
                .andExpect(jsonPath("$[0].transactionId").value("TXN-LIST-3"))
                .andExpect(jsonPath("$[2].transactionId").value("TXN-LIST-1"))
                .andReturn();

        List<?> transactions = objectMapper.readValue(
                result.getResponse().getContentAsString(), List.class);
        assertEquals(3, transactions.size());
    }

    @Test
    @Order(9)
    void getCustomerTransactions_NoTransactions_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("customerId", "CUST-EMPTY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Order(10)
    void createTransaction_InvalidCurrencyFormat_Returns400() throws Exception {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "TXN-CURR",
                "CUST-CURR",
                new BigDecimal("100.00"),
                "usd123",
                TransactionType.PAYMENT
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ISO 4217")));
    }
}

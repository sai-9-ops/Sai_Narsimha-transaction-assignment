# Customer Transactions API

## 1. Overview

This project implements a RESTful Customer Transactions API using Java 17 and Spring Boot.

The application supports the following four transaction operations:

1. Create a transaction
2. Retrieve an individual transaction
3. Update the status of a transaction
4. Retrieve all transactions belonging to a customer

The application uses Spring Web for REST endpoints, Spring Data JPA for persistence, H2 as the embedded database, Jakarta Bean Validation for request validation, and JUnit/Spring Boot Test with MockMvc for integration testing.

## 2. Assignment Objectives

The main objectives of this project are:

- Build a maintainable Spring Boot REST API for customer transactions.
- Implement the four required transaction operations.
- Apply input validation and business-rule validation.
- Persist transaction data using JPA and an H2 database.
- Prevent duplicate transaction IDs.
- Enforce valid transaction status transitions.
- Return appropriate HTTP status codes and structured error responses.
- Provide meaningful integration tests beyond application-startup testing.

## 3. Technology Stack

| Technology | Usage |
|---|---|
| Java 17 | Application development language/runtime |
| Spring Boot 3.5.5 | Application framework |
| Spring Web | REST controllers and HTTP API |
| Spring Data JPA | Repository and persistence layer |
| Hibernate/JPA | ORM and entity mapping |
| H2 Database | Embedded in-memory database |
| Jakarta Validation | Request/input validation |
| JUnit 5 | Automated integration testing |
| Spring Boot Test | Integration testing |
| MockMvc | HTTP endpoint testing |
| Maven | Build and dependency management |

## 4. Project Structure

```text
transaction-starter/
├── pom.xml
├── README.md
├── STUDENT_CHECKLIST.md
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.properties
│       └── maven-wrapper.jar
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── transactionstarter/
│   │   │               ├── TransactionStarterApplication.java
│   │   │               ├── sample/
│   │   │               │   └── SampleController.java
│   │   │               └── transaction/
│   │   │                   ├── Transaction.java
│   │   │                   ├── TransactionController.java
│   │   │                   ├── TransactionService.java
│   │   │                   ├── TransactionRepository.java
│   │   │                   ├── TransactionStatus.java
│   │   │                   ├── TransactionType.java
│   │   │                   ├── CreateTransactionRequest.java
│   │   │                   ├── UpdateTransactionStatusRequest.java
│   │   │                   ├── TransactionResponse.java
│   │   │                   └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── transactionstarter/
│                       ├── TransactionStarterApplicationTests.java
│                       └── transaction/
│                           └── TransactionControllerTest.java
└── target/
    ├── classes/
    ├── test-classes/
    └── surefire-reports/

```
5. API Endpoints
5.1 Create Transaction
POST /api/transactions

Creates a new transaction with an initial PENDING status.

Request Body:
{
  "transactionId": "TXN-1001",
  "customerId": "CUS-1001",
  "amount": 1500.00,
  "currency": "INR",
  "transactionType": "PAYMENT"
}

Success Response:

201 Created

The response contains the persisted transaction details, including its status and timestamps.

5.2 Get Transaction
GET /api/transactions/{transactionId}

Retrieves a transaction using its unique transaction ID.

Example:

GET /api/transactions/TXN-1001

Success Response:

200 OK

Error Response:

404 Not Found (If the transaction does not exist)

5.3 Update Transaction Status
PATCH /api/transactions/{transactionId}/status

Updates the status of an existing transaction after validating the requested status transition.

Example:

PATCH /api/transactions/TXN-1001/status

Request Body:
{
  "status": "PROCESSING"
}

The transaction can subsequently be updated to:
{
  "status": "COMPLETED"
}
Success Response:

200 OK

Only the transaction status is modified and the updatedAt timestamp is updated.

5.4 Get Customer Transactions
GET /api/transactions?customerId={id}

Returns all transactions belonging to the specified customer.

Example:

GET /api/transactions?customerId=CUS-1001

Transactions are returned in descending creation order, with the newest transaction first. If the customer has no transactions, the API returns an empty array [].

Success Response:

200 OK
```text
6. Quick API Reference
Operation          Method       Endpoint                              Success Response
Create transaction	POST	   /api/transactions	                    201 Created
Get transaction	    GET	       /api/transactions/{transactionId}	      200 OK
Update status	    PATCH	   /api/transactions/{transactionId}/status	  200 OK
Get customer 	    GET	       /api/transactions?customerId={id}	      200 OK
transactions
```
```text
7. Transaction Data Model
Each transaction contains the following fields:
Field                            Description
transactionId                 Unique transaction identifier
customerId	                  Identifier of the customer
amount	                      Monetary transaction value
currency	                  Three-letter uppercase currency code
transactionType	              Type of transaction
status	                      Current transaction processing status
createdAt	                  Timestamp assigned when the transaction is created
updatedAt	                  Timestamp updated when the transaction changes

Note: transactionId and customerId are immutable after creation.
```
8. Transaction Types
The application supports the following transaction types:
````
  DEPOSIT
  WITHDRAWAL
  TRANSFER
  PAYMENT
  REFUND
````
9. Transaction Statuses
The application supports the following transaction statuses:
```
PENDING

PROCESSING

COMPLETED

FAILED

CANCELLED
```

10. Transaction Statuses
The application supports the following transaction statuses:
```
PENDING

PROCESSING

COMPLETED

FAILED

CANCELLED

```
10. Validation Rules
Transaction ID: Required. Cannot be blank. Maximum length is 100 characters. Must be unique. Cannot be changed after creation.

Customer ID: Required. Cannot be blank. Maximum length is 100 characters. Cannot be changed after creation.

Amount: Required. Must be greater than zero. Uses BigDecimal for monetary values. Supports up to 15 integer digits and 4 fractional digits.

Currency: Required. Must contain exactly three uppercase letters. Validated against expected ISO 4217-style format.

Transaction Type: Required. Must be one of the supported transaction types.

Initial Status: Every new transaction starts with PENDING. The status is not accepted as a field in the create request.

11. Business Validation
In addition to annotation-based request validation, the application applies the following business rules:

Duplicate Transaction IDs: Transaction IDs must be unique. If a transaction with the same ID already exists, creation is rejected with 409 Conflict.

TEST Customer Restriction: For TEST customers, withdrawals or transfers above 1,000,000 are rejected.

Terminal Statuses: COMPLETED, FAILED, and CANCELLED are terminal. Once reached, the status cannot be changed.

Processing Status: A transaction in PROCESSING cannot move back to PENDING.

Same Status: A transaction cannot be updated to the same status it currently has.

12. Status Transition Rules
A newly created transaction starts in PENDING.
```text
PENDING ───► PROCESSING ───► COMPLETED
```

Other terminal outcomes include:

PENDING ───► FAILED

PENDING ───► CANCELLED

Terminal statuses cannot transition to another status.

A PROCESSING transaction cannot return to PENDING.

A transaction cannot be updated to its current status.

13. Error Handling
The application uses a centralized GlobalExceptionHandler to provide consistent error responses.
```text
HTTP                   StatusMeaning
400 Bad Request     	Validation or business-rule error
404 Not Found	        Transaction does not exist
409 Conflict	        Duplicate transaction ID
```
Error responses contain:

Timestamp

HTTP status

Error description

Error message

14. Persistence and Configuration
Uses an H2 in-memory database for transaction persistence.

Spring Data JPA and Hibernate for database access and entity mapping.

Configuration highlights:

H2 in-memory database

JPA create-drop schema handling

Open Session in View disabled

15. Testing
The project uses JUnit 5, Spring Boot Test (@SpringBootTest), and MockMvc. The test suite covers both successful operations and failure scenarios:

Successful transaction creation and persistence.

Duplicate transaction ID returns 409 Conflict.

Invalid request data returns 400 Bad Request.

Existing transaction retrieval returns 200 OK.

Missing transaction retrieval returns 404 Not Found.

Valid status transition from PENDING to PROCESSING to COMPLETED.

Invalid update from a terminal status is rejected.

Customer transaction list is returned in descending creation order.

Customer with no transactions receives an empty list.

Invalid currency format is rejected.

16. Test Results
The complete automated test suite was executed successfully using Maven:

[INFO] Results:
[INFO] Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 01:12 min
[INFO] Finished at: 2026-08-31T12:31:24+05:30
[INFO] ------------------------------------------------------------------------
```text
Test Summary:-
Metric           Result
Tests Run	     42
Failures	     0
Errors	         0
Skipped	         0
Build Status	BUILD SUCCESS
```
17. Running the Application
Windows:
mvnw.cmd spring-boot:run

18. Running the Tests
Windows:
mvnw.cmd clean test

19. Example API Flow
Step 1: Create a Transaction
POST /api/transactions
```
{
  "transactionId": "TXN-1001",
  "customerId": "CUS-1001",
  "amount": 1500.00,
  "currency": "INR",
  "transactionType": "PAYMENT"
}
```
Created with status: PENDING

Step 2: Move Transaction to Processing
PATCH /api/transactions/TXN-1001/status
```
{
  "status": "PROCESSING"
}
```
Step 3: Complete the Transaction
PATCH /api/transactions/TXN-1001/status
```
{
  "status": "COMPLETED"
}
```
Step 4: Retrieve the Transaction
GET /api/transactions/TXN-1001

Step 5: Retrieve Customer Transactions
GET /api/transactions?customerId=CUS-1001

20. Expected API Behaviour
A valid POST request creates and persists a transaction with PENDING status and a creation timestamp.

GET requests retrieve persisted transaction data.

PATCH requests modify only the transaction status and update the modification timestamp.

Customer-level retrieval returns transactions belonging to the requested customer in descending creation order.

If no transactions exist for a customer, an empty array [] is returned.

21. Activity Outcome
The completed project provides all four requested customer transaction operations together with input validation, business-rule enforcement, transaction persistence, duplicate ID protection, status-transition validation, centralized exception handling, appropriate HTTP status codes, and automated integration test coverage. The design separates controller, service, repository, entity, and DTO responsibilities, making the implementation maintainable and straightforward to extend.

22. Declaration of AI Tool Usage
I hereby declare that GitHub Copilot was utilized as an assistive tool during the development of this project. While Copilot was used for code suggestions, boilerplate generation, and syntax assistance, I actively architected the solution, wrote the core business logic, implemented the domain rules, and performed all testing. I fully understand the underlying mechanics, codebase architecture, and functionality of the application.

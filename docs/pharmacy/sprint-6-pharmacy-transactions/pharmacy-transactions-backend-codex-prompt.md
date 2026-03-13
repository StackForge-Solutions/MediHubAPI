# Pharmacy Transactions Backend Codex Prompt

## Purpose

Use this prompt when asking Codex to implement the backend for `Sprint 6: Pharmacy Transactions` in the pharmacy inventory module.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-transactions-backend
```

Current checked-out branch:

```text
feature/auth-jwt-identity-role-claims
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-transactions-backend
```

Command to create it explicitly from the current branch name:

```bash
git checkout -b feature/pharmacy-transactions-backend feature/auth-jwt-identity-role-claims
```

## Backend Scope

This prompt is only for the backend required by `Sprint 6: Pharmacy Transactions`.

Main goals:

- expose paginated transaction ledger API
- expose transaction detail API
- support server-side filtering by date, medicine, batch, vendor, transaction type, and reference
- reuse the existing immutable stock transaction history as the source of truth
- return data suitable for audit, review, and reporting screens

## Data Model Design

Use the existing transaction and inventory model established by stock receiving and stock adjustment.

### 1. `pharmacy_stock_transaction`

Purpose:
- Immutable audit log of stock movement

Use existing entity:

- `PharmacyStockTransaction`

Expected columns:

- `id` bigint primary key
- `medicine_id` bigint not null
- `batch_id` bigint null
- `transaction_type` varchar(50) not null
- `qty_in` int not null default 0
- `qty_out` int not null default 0
- `balance_after` int not null
- `unit_cost` decimal(14,2) null
- `unit_price` decimal(14,2) null
- `reference_type` varchar(50) null
- `reference_id` bigint null
- `reference_no` varchar(100) null
- `note` varchar(500) null
- `created_by` varchar(100) null
- `transaction_time` datetime not null

Indexes:

- index on `medicine_id`
- index on `batch_id`
- index on `transaction_type`
- index on `transaction_time`
- index on `reference_type + reference_id`

Notes:

- this table is the source of truth for the transaction ledger
- do not mutate historical rows in this sprint

### 2. `mdm_medicines`

Purpose:
- Medicine master data for display in the transaction ledger

Use existing entity:

- `MdmMedicine`

Used fields:

- `id`
- `name` or mapped medicine display name field
- `code` if needed for search or export
- `form`

### 3. `pharmacy_stock_batch`

Purpose:
- Batch metadata for ledger display

Use existing entity:

- `PharmacyStockBatch`

Used fields:

- `id`
- `batch_no`
- `vendor_id`

### 4. `pharmacy_vendor`

Purpose:
- Vendor display in the ledger

Use existing entity:

- `PharmacyVendor`

Used fields:

- `id`
- `vendor_name`

## Suggested DTOs

### Transaction Ledger Row DTO

```json
{
  "transactionId": 7001,
  "transactionTime": "2026-03-12T11:00:00Z",
  "medicineId": 101,
  "medicineName": "Paracetamol 500",
  "batchId": 9001,
  "batchNo": "BATCH-APR-01",
  "vendorId": 4,
  "vendorName": "ABC Pharma",
  "transactionType": "PURCHASE_RECEIPT",
  "qtyIn": 100,
  "qtyOut": 0,
  "balanceAfter": 120,
  "unitCost": 1.75,
  "unitPrice": 2.5,
  "referenceType": "PURCHASE_ORDER",
  "referenceId": 501,
  "referenceNo": "PO-20260312-001",
  "createdBy": "admin",
  "note": "Initial receipt"
}
```

### Transaction Detail DTO

```json
{
  "transactionId": 7001,
  "transactionTime": "2026-03-12T11:00:00Z",
  "transactionType": "PURCHASE_RECEIPT",
  "medicineId": 101,
  "medicineName": "Paracetamol 500",
  "batchId": 9001,
  "batchNo": "BATCH-APR-01",
  "qtyIn": 100,
  "qtyOut": 0,
  "balanceBefore": 20,
  "balanceAfter": 120,
  "unitCost": 1.75,
  "unitPrice": 2.5,
  "referenceType": "PURCHASE_ORDER",
  "referenceId": 501,
  "referenceNo": "PO-20260312-001",
  "createdBy": "admin",
  "note": "Initial receipt"
}
```

## API Contract

Base path:

```text
/api/pharmacy
```

### 1. GET `/api/pharmacy/transactions`

Purpose:
- Paginated transaction ledger for the main transactions page

Query params:

- `q`: string
- `medicineId`: long
- `vendorId`: long
- `transactionType`: string
- `batchNo`: string
- `referenceType`: string
- `referenceId`: long
- `fromDate`: date
- `toDate`: date
- `page`: int
- `size`: int

Response:
- `PageResponse<PharmacyTransactionRowDto>`

### 2. GET `/api/pharmacy/transactions/{id}`

Purpose:
- Transaction detail for the frontend drawer

Response:
- `DataResponse<PharmacyTransactionDetailDto>`

## Suggested Backend Package Structure

```text
src/main/java/com/MediHubAPI/
  controller/pharmacy/
    PharmacyTransactionController.java
  dto/pharmacy/
    PharmacyTransactionRowDto.java
    PharmacyTransactionDetailDto.java
  repository/pharmacy/
    PharmacyStockTransactionRepository.java
  repository/projection/
    PharmacyTransactionRowProjection.java
    PharmacyTransactionDetailProjection.java
  service/pharmacy/
    PharmacyTransactionQueryService.java
  service/pharmacy/impl/
    PharmacyTransactionQueryServiceImpl.java
```

## Suggested Controller Design

Create:

- `PharmacyTransactionController`

Endpoints:

- `GET /api/pharmacy/transactions`
- `GET /api/pharmacy/transactions/{id}`

Use:

- `PageResponse<T>` for list endpoints
- `DataResponse<T>` for detail payloads

## Suggested Repository Query Design

### Ledger list query should return

- transaction id and timestamp
- medicine id and display name
- batch id and batch number
- vendor id and vendor name
- transaction type
- qty in and qty out
- balance after
- unit cost and unit price
- reference type, id, and number
- created by
- note

### Ledger list query behavior

- order by `transaction_time desc`
- support filters by:
  - medicine id
  - vendor id
  - transaction type
  - batch number
  - reference type
  - reference id
  - from and to date
  - free-text `q`
- free-text search can match reference number, note, batch number, and medicine name if practical

### Detail query should return

- all list fields needed by the drawer
- `balanceBefore`

Recommended balance detail rule:

- if `balanceBefore` is not stored, derive it as:
  - `balance_after - qty_in + qty_out`

## Validation and Exception Handling

Use the existing shared exception handling infrastructure in:

- `src/main/java/com/MediHubAPI/exception/GlobalExceptionHandler.java`
- `src/main/java/com/MediHubAPI/exception/ValidationException.java`
- `src/main/java/com/MediHubAPI/exception/ResourceNotFoundException.java`
- `src/main/java/com/MediHubAPI/exception/HospitalAPIException.java`

Do not create a separate pharmacy exception handler for this scope unless absolutely necessary.
Prefer integrating with the existing `GlobalExceptionHandler`.

### Required Validation Rules

#### GET `/api/pharmacy/transactions`

Validate:

- `page >= 0`
- `size > 0`
- `size <= 100`
- `q` minimum length 2 if provided and non-blank
- `medicineId > 0` if provided
- `vendorId > 0` if provided
- `referenceId > 0` if provided
- `fromDate <= toDate` when both are provided
- `transactionType` must be a valid enum value if enum-based
- `referenceType` must be a valid allowed value if enum-based

#### GET `/api/pharmacy/transactions/{id}`

Validate:

- `id > 0`
- throw `ResourceNotFoundException` or a specific transaction not found exception if the row does not exist

## Business Rules

- transaction ledger is read/query only
- do not create, update, or delete transaction rows from this sprint
- purchase receipt and stock adjustment flows remain the producers of ledger rows
- support transaction types:
  - PURCHASE_RECEIPT
  - SALE
  - ADJUSTMENT_IN
  - ADJUSTMENT_OUT
  - RETURN_TO_VENDOR
  - RETURN_FROM_PATIENT
  - EXPIRED
  - DAMAGED
- return latest movement first
- keep response shape stable for audit/export use cases

## Suggested Tests

- list endpoint returns paginated rows in descending transaction time order
- list endpoint filters by medicine, vendor, type, batch, reference, and date range
- list endpoint rejects invalid pagination and invalid date ranges
- detail endpoint returns a single transaction with derived `balanceBefore`
- detail endpoint returns not found for missing id

## Acceptance Criteria

- transaction list endpoint returns paginated ledger rows for the UI
- detail endpoint returns complete transaction data for the drawer
- response shapes match the UI contract
- server-side filters work as documented
- implementation follows existing package structure and response wrapper style

## Codex Prompt

```text
Implement the backend for `Sprint 6: Pharmacy Transactions` in the pharmacy inventory module.

Project context:
- Spring Boot backend
- existing pharmacy stock foundation already includes medicines, stock batches, stock summary, and immutable stock transaction rows
- purchase receipt and stock adjustment flows already create transaction rows
- this sprint exposes a transaction ledger and transaction detail query layer for audit and reporting

Build the following:
- paginated ledger API: `GET /api/pharmacy/transactions`
- transaction detail API: `GET /api/pharmacy/transactions/{id}`

Reuse or extend these existing concepts where available:
- `PharmacyStockTransaction`
- `PharmacyStockTransactionRepository`
- `PageResponse<T>`
- `DataResponse<T>`
- shared validation and exception handling infrastructure

Required request and response models:
- PharmacyTransactionRowDto
- PharmacyTransactionDetailDto

Required behavior:
- support server-side filtering by date, medicine, batch, vendor, transaction type, and reference
- order rows by latest transaction first
- include medicine, batch, vendor, reference, and audit metadata needed by the UI
- derive `balanceBefore` for the detail endpoint if it is not already stored
- keep this sprint read-only; do not add mutation APIs

Required API contract:
- GET `/api/pharmacy/transactions`
  - query params: q, medicineId, vendorId, transactionType, batchNo, referenceType, referenceId, fromDate, toDate, page, size
  - response: PageResponse<PharmacyTransactionRowDto>
- GET `/api/pharmacy/transactions/{id}`
  - response: DataResponse<PharmacyTransactionDetailDto>

Suggested package structure:
- controller/pharmacy/PharmacyTransactionController.java
- dto/pharmacy/PharmacyTransactionRowDto.java
- dto/pharmacy/PharmacyTransactionDetailDto.java
- repository/projection/PharmacyTransactionRowProjection.java
- repository/projection/PharmacyTransactionDetailProjection.java
- service/pharmacy/PharmacyTransactionQueryService.java
- service/pharmacy/impl/PharmacyTransactionQueryServiceImpl.java

Validation and exception handling:
- reuse GlobalExceptionHandler
- reuse ValidationException, ResourceNotFoundException, and HospitalAPIException where appropriate
- do not create a separate pharmacy exception handler unless required

Implementation notes:
- keep list endpoint optimized for server-side pagination and filtering
- do not mutate historical transaction rows
- note any assumptions if vendor or reference metadata must be joined indirectly
- no dedicated export API is required in this scope unless you intentionally choose to add one and document it

Acceptance criteria:
- list API returns paginated transaction data for the UI
- detail API returns drawer-ready transaction data
- filters work correctly
- implementation matches existing backend patterns and response wrappers

Also provide:
- any assumptions if existing entities or enums differ
- brief explanation of query design
- note which validations are handled in controller vs service
```

## Notes

- This document is backend-only for `Sprint 6: Pharmacy Transactions`.
- Keep it aligned with `docs/pharmacy-development-sequence.md` and `docs/pharmacy-stock-purchase-ui-design.md`.
- This scope depends on upstream receipt and adjustment flows already creating `PharmacyStockTransaction` records.

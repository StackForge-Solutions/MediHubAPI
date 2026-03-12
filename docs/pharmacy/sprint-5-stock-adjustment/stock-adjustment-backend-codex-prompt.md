# Stock Adjustment Backend Codex Prompt

## Purpose

Use this prompt when asking Codex to implement the backend for `Sprint 5: Stock Adjustment` in the pharmacy inventory module.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-stock-adjustment-backend
```

Current checked-out branch:

```text
feature/auth-jwt-identity-role-claims
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-stock-adjustment-backend
```

Command to create it explicitly from the current branch name:

```bash
git checkout -b feature/pharmacy-stock-adjustment-backend feature/auth-jwt-identity-role-claims
```

## Backend Scope

This prompt is only for the backend required by `Sprint 5: Stock Adjustment`.

Main goals:

- create stock adjustment header and item persistence
- expose paginated stock adjustment list API
- expose create-and-post stock adjustment API
- expose stock adjustment detail API
- validate stock availability and batch rules before posting
- update batch stock and stock summary atomically
- write stock transaction entries for every posted adjustment item

## DB Mapping Design

Use the existing batch-based inventory model from stock receiving and manage stocks.

### 1. `stock_adjustment`

Purpose:
- Header table for posted stock adjustment documents

Columns:

- `id` bigint primary key
- `adjustment_no` varchar(100) not null unique
- `adjustment_date` date not null
- `adjustment_type` varchar(30) not null
- `reason` varchar(50) not null
- `note` varchar(500) null
- `status` varchar(30) not null
- `created_by` varchar(100) null
- `created_at` datetime not null
- `updated_at` datetime null

Indexes:

- index on `adjustment_no`
- index on `adjustment_date`
- index on `adjustment_type`
- index on `reason`
- index on `status`

Java entity:

- `StockAdjustment`

Notes:

- keep this as the document header
- this sprint only needs posted adjustments; drafts are optional and out of scope unless already supported elsewhere

### 2. `stock_adjustment_item`

Purpose:
- Line items belonging to a stock adjustment

Columns:

- `id` bigint primary key
- `stock_adjustment_id` bigint not null
- `medicine_id` bigint not null
- `batch_id` bigint null
- `qty` int not null
- `unit_cost` decimal(14,2) null
- `line_value` decimal(14,2) null
- `note` varchar(500) null
- `created_at` datetime not null

Indexes:

- index on `stock_adjustment_id`
- index on `medicine_id`
- index on `batch_id`

Java entity:

- `StockAdjustmentItem`

Notes:

- `line_value` can be stored or computed as `qty * unit_cost`
- allow `batch_id` only if the business rules permit non-batch increase adjustments; otherwise validate it as required for stock-moving rows

### 3. `pharmacy_stock`

Purpose:
- Summary stock table per medicine

Use existing entity:

- `PharmacyStock`

Adjustment effect:

- increase or decrease `available_qty` based on posted items
- never let summary stock become negative

### 4. `pharmacy_stock_batch`

Purpose:
- Source of truth for physical stock by batch

Use existing entity:

- `PharmacyStockBatch`

Adjustment effect:

- for `DECREASE`, deduct from the selected batch
- for `INCREASE`, add to the selected batch
- never let batch `available_qty` become negative

### 5. `pharmacy_stock_transaction`

Purpose:
- Immutable stock movement audit log

Use existing entity:

- `PharmacyStockTransaction`

Adjustment effect:

- write one transaction row per posted adjustment item
- use `ADJUSTMENT_IN` for increases
- use `ADJUSTMENT_OUT` for decreases
- set reference type and reference id to the stock adjustment document

### Entity Relationships

- `StockAdjustment 1 -> many StockAdjustmentItem`
- `StockAdjustmentItem many -> 1 MdmMedicine`
- `StockAdjustmentItem many -> 1 PharmacyStockBatch`
- `StockAdjustmentItem` posting updates `PharmacyStock`
- `StockAdjustmentItem` posting writes `PharmacyStockTransaction`

## Suggested DTOs

### Stock Adjustment List Row DTO

```json
{
  "adjustmentId": 301,
  "adjustmentNo": "ADJ-20260312-001",
  "adjustmentDate": "2026-03-12",
  "adjustmentType": "DECREASE",
  "reason": "DAMAGED",
  "medicineCount": 2,
  "totalQtyImpact": 14,
  "createdBy": "admin",
  "status": "POSTED"
}
```

### Create Stock Adjustment Request DTO

```json
{
  "adjustmentDate": "2026-03-12",
  "adjustmentType": "DECREASE",
  "reason": "DAMAGED",
  "note": "Broken strips found during count",
  "items": [
    {
      "medicineId": 101,
      "batchId": 9001,
      "qty": 10,
      "unitCost": 1.75,
      "note": "Damaged strips"
    }
  ]
}
```

### Create Stock Adjustment Response DTO

```json
{
  "adjustmentId": 301,
  "adjustmentNo": "ADJ-20260312-001",
  "status": "POSTED"
}
```

### Stock Adjustment Detail DTO

```json
{
  "adjustmentId": 301,
  "adjustmentNo": "ADJ-20260312-001",
  "adjustmentDate": "2026-03-12",
  "adjustmentType": "DECREASE",
  "reason": "DAMAGED",
  "note": "Broken strips found during count",
  "createdBy": "admin",
  "status": "POSTED",
  "items": [
    {
      "medicineId": 101,
      "medicineName": "Paracetamol 500",
      "batchId": 9001,
      "batchNo": "BATCH-APR-01",
      "qty": 10,
      "unitCost": 1.75,
      "lineValue": 17.5,
      "note": "Damaged strips"
    }
  ]
}
```

## API Contract

Base path:

```text
/api/pharmacy
```

### 1. GET `/api/pharmacy/stock-adjustments`

Purpose:
- Paginated stock adjustment list for the list page

Query params:

- `q`: string
- `reason`: string
- `type`: string
- `fromDate`: date
- `toDate`: date
- `page`: int
- `size`: int

Response:
- `PageResponse<StockAdjustmentListRowDto>`

### 2. POST `/api/pharmacy/stock-adjustments`

Purpose:
- Create and immediately post a stock adjustment

Request:
- `CreateStockAdjustmentRequestDto`

Response:
- `DataResponse<CreateStockAdjustmentResponseDto>`

### 3. GET `/api/pharmacy/stock-adjustments/{id}`

Purpose:
- Load a posted stock adjustment for the detail page

Response:
- `DataResponse<StockAdjustmentDetailDto>`

## Suggested Backend Package Structure

```text
src/main/java/com/MediHubAPI/
  controller/pharmacy/
    PharmacyStockAdjustmentController.java
  dto/pharmacy/
    StockAdjustmentListRowDto.java
    StockAdjustmentItemDto.java
    StockAdjustmentDetailDto.java
    CreateStockAdjustmentRequestDto.java
    CreateStockAdjustmentItemRequestDto.java
    CreateStockAdjustmentResponseDto.java
  model/pharmacy/
    StockAdjustment.java
    StockAdjustmentItem.java
  repository/pharmacy/
    StockAdjustmentRepository.java
    StockAdjustmentItemRepository.java
    PharmacyStockRepository.java
    PharmacyStockBatchRepository.java
    PharmacyStockTransactionRepository.java
  repository/projection/
    StockAdjustmentListRowProjection.java
  service/pharmacy/
    PharmacyStockAdjustmentService.java
  service/pharmacy/impl/
    PharmacyStockAdjustmentServiceImpl.java
```

## Suggested Controller Design

Create:

- `PharmacyStockAdjustmentController`

Endpoints:

- `GET /api/pharmacy/stock-adjustments`
- `POST /api/pharmacy/stock-adjustments`
- `GET /api/pharmacy/stock-adjustments/{id}`

Use:

- `PageResponse<T>` for list endpoints
- `DataResponse<T>` for create response and detail payload

## Suggested Repository Query Design

### List query should return

- adjustment id and number
- adjustment date
- adjustment type
- reason
- distinct medicine count
- sum of item quantities as total quantity impact
- created by
- status

### Detail query should return

- header fields from `stock_adjustment`
- line item details joined to `mdm_medicines`
- batch number from `pharmacy_stock_batch`

### Posting workflow should load

- all referenced medicines
- all referenced batches
- current `PharmacyStock` summary rows
- existing batch `available_qty`

## Validation and Exception Handling

Use the existing shared exception handling infrastructure in:

- `src/main/java/com/MediHubAPI/exception/GlobalExceptionHandler.java`
- `src/main/java/com/MediHubAPI/exception/ValidationException.java`
- `src/main/java/com/MediHubAPI/exception/ResourceNotFoundException.java`
- `src/main/java/com/MediHubAPI/exception/HospitalAPIException.java`
- `src/main/java/com/MediHubAPI/exception/pharmacy/MedicineNotFoundException.java`

Do not create a separate pharmacy exception handler for this scope unless absolutely necessary.
Prefer integrating with the existing `GlobalExceptionHandler`.

### Required Validation Rules

#### GET `/api/pharmacy/stock-adjustments`

Validate:

- `page >= 0`
- `size > 0`
- `size <= 100`
- `q` minimum length 2 if provided and non-blank
- `fromDate <= toDate` when both are provided
- `type` must be a valid adjustment type if enum-based
- `reason` must be a valid reason if enum-based

#### POST `/api/pharmacy/stock-adjustments`

Validate:

- `adjustmentDate` is required
- `adjustmentType` is required
- `reason` is required
- at least one item is required
- each item must have `medicineId`
- each item `qty > 0`
- each item `unitCost >= 0` if provided
- if `adjustmentType = DECREASE`, header `note` is required
- if `adjustmentType = DECREASE`, item quantity must not exceed available batch stock
- do not allow negative stock at batch or summary level
- do not allow adjustment out from expired batches unless a dedicated expired flow already exists
- if the business rule requires batch-controlled adjustments, require `batchId` for all stock-moving items
- reject duplicate invalid rows instead of silently merging them unless a clear merge rule is intentionally implemented

#### GET `/api/pharmacy/stock-adjustments/{id}`

Validate:

- `id > 0`
- throw `ResourceNotFoundException` or a specific stock adjustment not found exception if the document does not exist

## Business Rules

- posting must run inside a transaction
- only posted adjustments affect stock quantities
- `DECREASE` writes stock transactions with `qtyOut`
- `INCREASE` writes stock transactions with `qtyIn`
- set transaction type to `ADJUSTMENT_OUT` or `ADJUSTMENT_IN`
- set transaction reference type to `STOCK_ADJUSTMENT`
- set transaction reference id and reference no from the posted adjustment
- update both `pharmacy_stock_batch.available_qty` and `pharmacy_stock.available_qty`
- compute `lineValue` as `qty * unitCost` if not persisted directly
- generate `adjustment_no` in a stable business format such as `ADJ-yyyyMMdd-###`
- the detail endpoint is read-only after posting

## Transaction and Consistency Rules

- save header and items only if the entire posting operation succeeds
- write stock transactions in the same database transaction as stock quantity updates
- lock affected stock summary and batch rows if needed to avoid race conditions during concurrent posting
- never partially post an adjustment document
- if a referenced stock summary row does not exist for a medicine, create or initialize it only if that matches the current inventory design; otherwise fail fast with a clear error

## Suggested Tests

- create adjustment increase with valid batch and update stock successfully
- create adjustment decrease with valid available stock successfully
- reject decrease when requested qty exceeds batch available qty
- reject request with missing note on `DECREASE`
- reject request with invalid medicine or batch id
- list endpoint filters by date, type, reason, and search text
- detail endpoint returns header and line items correctly
- posting writes `PharmacyStockTransaction` rows with correct reference metadata

## Acceptance Criteria

- stock adjustment list endpoint returns paginated rows for the UI
- create endpoint posts valid adjustment documents and updates inventory correctly
- detail endpoint returns a complete read-only adjustment document
- response shapes match the UI contract
- stock summary and batch quantities stay consistent after posting
- transaction rows are created for all posted items
- code follows existing package structure and response wrapper style

## Codex Prompt

```text
Implement the backend for `Sprint 5: Stock Adjustment` in the pharmacy inventory module.

Project context:
- Spring Boot backend
- existing pharmacy stock foundation already includes medicines, stock summary, stock batches, and stock transactions
- this sprint adds controlled manual stock corrections
- stock adjustment must work on top of existing stock and batches and must preserve inventory auditability

Build the following:
- `StockAdjustment` header entity
- `StockAdjustmentItem` entity
- paginated list API: `GET /api/pharmacy/stock-adjustments`
- create-and-post API: `POST /api/pharmacy/stock-adjustments`
- detail API: `GET /api/pharmacy/stock-adjustments/{id}`

Use or reuse these existing concepts where available:
- `PharmacyStock`
- `PharmacyStockBatch`
- `PharmacyStockTransaction`
- `PageResponse<T>`
- `DataResponse<T>`
- shared validation and exception handling infrastructure

Required request and response models:
- StockAdjustmentListRowDto
- CreateStockAdjustmentRequestDto
- CreateStockAdjustmentItemRequestDto
- CreateStockAdjustmentResponseDto
- StockAdjustmentDetailDto
- StockAdjustmentItemDto

Required behavior:
- support `INCREASE` and `DECREASE`
- validate stock and batch availability before posting
- require note for decreases
- prevent negative stock after posting
- update batch stock and stock summary in one transaction
- write one stock transaction row per item with adjustment reference metadata
- generate business adjustment number

Required API contract:
- GET `/api/pharmacy/stock-adjustments`
  - query params: q, reason, type, fromDate, toDate, page, size
  - response: PageResponse<StockAdjustmentListRowDto>
- POST `/api/pharmacy/stock-adjustments`
  - request:
    {
      "adjustmentDate": "2026-03-12",
      "adjustmentType": "DECREASE",
      "reason": "DAMAGED",
      "note": "Broken strips found during count",
      "items": [
        {
          "medicineId": 101,
          "batchId": 9001,
          "qty": 10,
          "unitCost": 1.75,
          "note": "Damaged strips"
        }
      ]
    }
  - response: DataResponse<CreateStockAdjustmentResponseDto>
- GET `/api/pharmacy/stock-adjustments/{id}`
  - response: DataResponse<StockAdjustmentDetailDto>

Suggested package structure:
- controller/pharmacy/PharmacyStockAdjustmentController.java
- dto/pharmacy/StockAdjustmentListRowDto.java
- dto/pharmacy/StockAdjustmentItemDto.java
- dto/pharmacy/StockAdjustmentDetailDto.java
- dto/pharmacy/CreateStockAdjustmentRequestDto.java
- dto/pharmacy/CreateStockAdjustmentItemRequestDto.java
- dto/pharmacy/CreateStockAdjustmentResponseDto.java
- model/pharmacy/StockAdjustment.java
- model/pharmacy/StockAdjustmentItem.java
- repository/pharmacy/StockAdjustmentRepository.java
- repository/pharmacy/StockAdjustmentItemRepository.java
- repository/projection/StockAdjustmentListRowProjection.java
- service/pharmacy/PharmacyStockAdjustmentService.java
- service/pharmacy/impl/PharmacyStockAdjustmentServiceImpl.java

Validation and exception handling:
- reuse GlobalExceptionHandler
- reuse ValidationException, ResourceNotFoundException, HospitalAPIException, and MedicineNotFoundException where appropriate
- do not create a separate pharmacy exception handler unless required

Implementation notes:
- keep posting logic transactional
- do not silently over-deduct or auto-correct requested quantities
- do not mutate historical transaction rows
- keep list endpoint optimized for server-side pagination
- note any schema migrations required if these tables do not already exist

Acceptance criteria:
- list API returns paginated adjustment data for the UI
- create API persists header and items, updates inventory, and writes transactions
- detail API returns posted adjustment data with item lines
- validation blocks invalid stock deductions
- implementation matches existing backend patterns and response wrappers

Also provide:
- any assumptions if existing entities or enums differ
- brief explanation of transaction/posting flow
- note which validations are handled in controller vs service
```

## Notes

- This document is backend-only for `Sprint 5: Stock Adjustment`.
- Keep it aligned with `docs/pharmacy-development-sequence.md` and `docs/pharmacy-stock-purchase-ui-design.md`.
- This scope depends on prior stock receipt, batch creation, stock summary, and transaction foundation being available first.

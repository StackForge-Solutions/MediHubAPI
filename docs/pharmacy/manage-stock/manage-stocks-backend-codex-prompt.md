# Manage Stocks Backend Codex Prompt

## Purpose

Use this prompt when asking Codex to implement the backend for the `Manage Stocks` page in the pharmacy inventory module.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-manage-stocks-backend
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-manage-stocks-backend
```

## Backend Scope

This prompt is only for the backend required by the `Manage Stocks` UI.

Main goals:

- Expose stock summary cards API
- Expose paginated manage stocks list API
- Expose medicine stock detail API
- Expose batch list API for a selected medicine
- Expose recent stock transactions API for a selected medicine
- Support server-side filtering, pagination, and sorting

## DB Mapping Design

Use a batch-based inventory design.

### 1. `mdm_medicines`

Purpose:
- Medicine master data

Columns:

- `id` bigint primary key
- `form` varchar(20) not null
- `brand` varchar(255) not null
- `composition` varchar(255) not null
- `is_active` bit not null
- `code` varchar(64) null

Java entity:

- `MdmMedicine`

Notes:

- Do not store selling price, purchase price, stock quantity, or batch details here.

### 2. `pharmacy_stock`

Purpose:
- Summary stock table per medicine for fast dashboard reads

Columns:

- `id` bigint primary key
- `medicine_id` bigint not null unique
- `available_qty` int not null default 0
- `reserved_qty` int not null default 0
- `reorder_level` int not null default 0
- `updated_at` datetime null

Java entity:

- `PharmacyStock`

Mapping:

- `@OneToOne` to `MdmMedicine`

Notes:

- This is a summary table.
- It should be updated from stock batches and transactions.
- This is not the source of truth for inventory history.

### 3. `pharmacy_stock_batch`

Purpose:
- Source of truth for physical stock by batch

Columns:

- `id` bigint primary key
- `medicine_id` bigint not null
- `vendor_id` bigint null
- `batch_no` varchar(100) not null
- `expiry_date` date not null
- `purchase_price` decimal(14,2) not null
- `mrp` decimal(14,2) not null
- `selling_price` decimal(14,2) not null
- `received_qty` int not null
- `available_qty` int not null
- `received_at` datetime not null
- `purchase_order_item_id` bigint null
- `is_active` bit not null default 1

Indexes:

- index on `medicine_id`
- index on `expiry_date`
- index on `vendor_id`
- unique recommendation: `medicine_id + batch_no + expiry_date`

Java entity:

- `PharmacyStockBatch`

Notes:

- `Manage Stocks` uses this table for nearest expiry, batch drill-down, and value calculation.
- FEFO ordering should use `expiry_date asc`.

### 4. `pharmacy_stock_transaction`

Purpose:
- Immutable audit log of stock movement

Columns:

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

Java entity:

- `PharmacyStockTransaction`

Notes:

- `Manage Stocks` uses this table for recent transaction history per medicine.
- Never update old rows except under strict admin repair tools.

### 5. `pharmacy_vendor`

Purpose:
- Optional join for vendor display in batch detail

Columns:

- `id` bigint primary key
- `vendor_name` varchar(255) not null
- `vendor_code` varchar(50) not null
- `active` bit not null

Java entity:

- `PharmacyVendor`

### Entity Relationships

- `MdmMedicine 1 -> 1 PharmacyStock`
- `MdmMedicine 1 -> many PharmacyStockBatch`
- `MdmMedicine 1 -> many PharmacyStockTransaction`
- `PharmacyVendor 1 -> many PharmacyStockBatch`
- `PharmacyStockBatch 1 -> many PharmacyStockTransaction`

## Suggested DTOs

### Stock Summary DTO

```json
{
  "totalMedicines": 450,
  "lowStockCount": 32,
  "outOfStockCount": 8,
  "expiringSoonCount": 19,
  "stockValue": 275430.75
}
```

### Manage Stocks Row DTO

```json
{
  "medicineId": 101,
  "medicineCode": "MED-001",
  "medicineName": "Paracetamol 500",
  "brand": "Dolo",
  "form": "TAB",
  "availableQty": 120,
  "reservedQty": 0,
  "reorderLevel": 50,
  "sellingPrice": 2.5,
  "mrp": 3.0,
  "nearestExpiryDate": "2026-08-31",
  "stockValue": 210.0,
  "stockStatus": "HEALTHY",
  "lowStock": false,
  "inStock": true
}
```

### Stock Detail DTO

```json
{
  "medicineId": 101,
  "medicineCode": "MED-001",
  "medicineName": "Paracetamol 500",
  "brand": "Dolo",
  "form": "TAB",
  "composition": "Paracetamol 500mg",
  "availableQty": 120,
  "reservedQty": 0,
  "reorderLevel": 50,
  "nearestExpiryDate": "2026-08-31",
  "stockValue": 210.0,
  "lowStock": false
}
```

### Stock Batch DTO

```json
{
  "batchId": 9001,
  "batchNo": "BATCH-APR-01",
  "vendorId": 4,
  "vendorName": "ABC Pharma",
  "expiryDate": "2026-08-31",
  "purchasePrice": 1.75,
  "mrp": 3.0,
  "sellingPrice": 2.5,
  "receivedQty": 100,
  "availableQty": 70,
  "expired": false
}
```

### Stock Transaction DTO

```json
{
  "transactionId": 7001,
  "transactionTime": "2026-03-12T11:00:00Z",
  "transactionType": "PURCHASE_RECEIPT",
  "batchNo": "BATCH-APR-01",
  "qtyIn": 100,
  "qtyOut": 0,
  "balanceAfter": 120,
  "referenceType": "PURCHASE_ORDER",
  "referenceId": 501,
  "referenceNo": "PO-20260312-001",
  "note": "Initial receipt"
}
```

## API Contract

Base path:

```text
/api/pharmacy
```

### 1. GET `/api/pharmacy/stocks`

Purpose:
- Paginated stock list for `Manage Stocks`

Query params:

- `q`: string
- `page`: int
- `size`: int
- `sort`: string
- `inStockOnly`: boolean
- `lowStockOnly`: boolean
- `expiringInDays`: int
- `vendorId`: long
- `form`: string

Response:
- `PageResponse<ManageStockRowDto>`

### 2. GET `/api/pharmacy/stocks/summary`

Purpose:
- Summary cards for `Manage Stocks`

Query params:

- `q`: string optional
- `vendorId`: long optional
- `form`: string optional

Response:
- `DataResponse<StockSummaryDto>`

### 3. GET `/api/pharmacy/stocks/{medicineId}`

Purpose:
- Header section for stock detail page

Response:
- `DataResponse<MedicineStockDetailDto>`

### 4. GET `/api/pharmacy/stocks/{medicineId}/batches`

Purpose:
- Batch detail list

Query params:

- `includeExpired`: boolean
- `page`: int
- `size`: int

Response:
- `PageResponse<MedicineStockBatchDto>`

### 5. GET `/api/pharmacy/stocks/{medicineId}/transactions`

Purpose:
- Recent stock movement history

Query params:

- `page`: int
- `size`: int

Response:
- `PageResponse<MedicineStockTransactionDto>`

## Suggested Backend Package Structure

```text
src/main/java/com/MediHubAPI/
  controller/pharmacy/
    PharmacyStockController.java
  dto/pharmacy/
    StockSummaryDto.java
    ManageStockRowDto.java
    MedicineStockDetailDto.java
    MedicineStockBatchDto.java
    MedicineStockTransactionDto.java
  model/pharmacy/
    PharmacyStock.java
    PharmacyStockBatch.java
    PharmacyStockTransaction.java
    PharmacyVendor.java
  repository/pharmacy/
    PharmacyStockRepository.java
    PharmacyStockBatchRepository.java
    PharmacyStockTransactionRepository.java
  repository/projection/
    ManageStockRowProjection.java
    StockSummaryProjection.java
    MedicineBatchProjection.java
    MedicineTransactionProjection.java
  service/pharmacy/
    PharmacyStockQueryService.java
  service/pharmacy/impl/
    PharmacyStockQueryServiceImpl.java
```

## Suggested Controller Design

Create:

- `PharmacyStockController`

Endpoints:

- `GET /api/pharmacy/stocks`
- `GET /api/pharmacy/stocks/summary`
- `GET /api/pharmacy/stocks/{medicineId}`
- `GET /api/pharmacy/stocks/{medicineId}/batches`
- `GET /api/pharmacy/stocks/{medicineId}/transactions`

Use:

- `PageResponse<T>` for paginated list endpoints
- `DataResponse<T>` for single payload endpoints

## Suggested Repository Query Design

### Stock list query should return

- medicine identity fields from `mdm_medicines`
- stock summary fields from `pharmacy_stock`
- nearest expiry from `pharmacy_stock_batch`
- representative current `selling_price` and `mrp`
- computed `stock_value`

### Summary query should return

- total medicines
- low stock count
- out of stock count
- expiring soon count
- total stock value

### Batches query should return

- only rows with `available_qty > 0` by default
- sorted by `expiry_date asc`

### Transactions query should return

- latest movement first
- filter by `medicine_id`

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

#### GET `/api/pharmacy/stocks`

Validate:

- `page >= 0`
- `size > 0`
- `size <= 100`
- `q` minimum length 2 if provided and non-blank
- `expiringInDays >= 0` if provided
- `vendorId > 0` if provided
- `form` must be a valid enum value if mapped to enum
- `sort` should allow only approved sort fields

Recommended allowed sort fields:

- `medicineName`
- `brand`
- `form`
- `availableQty`
- `nearestExpiryDate`
- `stockValue`

#### GET `/api/pharmacy/stocks/summary`

Validate:

- `q` minimum length 2 if provided and non-blank
- `vendorId > 0` if provided
- `form` must be valid if provided

#### GET `/api/pharmacy/stocks/{medicineId}`

Validate:

- `medicineId > 0`
- medicine must exist, otherwise throw `MedicineNotFoundException`

#### GET `/api/pharmacy/stocks/{medicineId}/batches`

Validate:

- `medicineId > 0`
- `page >= 0`
- `size > 0`
- `size <= 100`
- medicine must exist before querying batches

#### GET `/api/pharmacy/stocks/{medicineId}/transactions`

Validate:

- `medicineId > 0`
- `page >= 0`
- `size > 0`
- `size <= 100`
- medicine must exist before querying transactions

### Recommended Custom Exceptions

For this page, use these exception types:

- `MedicineNotFoundException`
  - use when `medicineId` does not exist
  - already present in the project
- `ValidationException`
  - use for service-layer validation with field-level details
- `HospitalAPIException`
  - use for structured pharmacy/business errors with explicit HTTP status and error code

Only add new custom exceptions if the existing types are not enough.

If new exceptions are needed, keep them under:

```text
src/main/java/com/MediHubAPI/exception/pharmacy/
```

Recommended optional exceptions:

- `InvalidStockQueryException extends ValidationException`
- `StockBatchNotFoundException extends HospitalAPIException`
- `StockTransactionQueryException extends ValidationException`

In most cases, `ValidationException` plus `MedicineNotFoundException` is sufficient for `Manage Stocks`.

### Required ValidationException Usage

When query parameters are invalid, throw `ValidationException` with field-level details.

Example fields:

- `page`
- `size`
- `q`
- `sort`
- `expiringInDays`
- `vendorId`
- `medicineId`

Example construction pattern:

```java
throw new ValidationException(
    "Validation failed",
    List.of(
        new ValidationException.ValidationErrorDetail("size", "size must be between 1 and 100"),
        new ValidationException.ValidationErrorDetail("sort", "unsupported sort field")
    )
);
```

### GlobalExceptionHandler Integration

The implementation should rely on the existing handlers already present in `GlobalExceptionHandler`:

- `MethodArgumentNotValidException`
- `BindException`
- `ConstraintViolationException`
- `ValidationException`
- `HospitalAPIException`
- `DataIntegrityViolationException`
- generic fallback `Exception`

For `Manage Stocks`, the expected mapping is:

- invalid request params -> `400 Bad Request`
- invalid enum/form/sort -> `400 Bad Request`
- medicine not found -> `404 Not Found`
- unexpected database/data issues -> `409 Conflict` or `500 Internal Server Error` depending on the source

### Suggested Validation Approach

Controller layer:

- use bean validation annotations where simple
- examples: `@Min(0)`, `@Positive`, `@PositiveOrZero`

Service layer:

- validate business/query rules that are cross-field or repository-aware
- throw `ValidationException` for invalid query combinations or unsupported sort values
- throw `MedicineNotFoundException` before loading batches or transactions

### Suggested Error Response Shape

For validation failures, the existing `GlobalExceptionHandler` already returns structured payloads.
The backend should preserve this style.

Typical validation response:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/pharmacy/stocks",
  "code": "VALIDATION_ERROR",
  "errorCode": "VALIDATION_ERROR",
  "validationErrors": {
    "size": "size must be between 1 and 100",
    "sort": "unsupported sort field"
  },
  "errors": [
    "size must be between 1 and 100",
    "unsupported sort field"
  ]
}
```

Typical not found response:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Medicine not found: 999",
  "path": "/api/pharmacy/stocks/999",
  "code": "MEDICINE_NOT_FOUND",
  "errorCode": "MEDICINE_NOT_FOUND"
}
```

## Business Rules

- `lowStock = availableQty <= reorderLevel`
- `inStock = availableQty > 0`
- `stockStatus` rules:
  - `OUT_OF_STOCK` if available qty is 0
  - `LOW_STOCK` if available qty is greater than 0 and less than or equal to reorder level
  - `EXPIRING_SOON` if nearest expiry is within selected threshold
  - otherwise `HEALTHY`
- Exclude inactive medicines unless explicitly needed
- Exclude inactive batches from active stock calculations
- Expired batches should not contribute to usable stock if business rules require that

## Codex Prompt

```text
Implement the backend for the `Manage Stocks` page in the pharmacy inventory module.

Project context:
- Java Spring Boot backend
- Existing project package base: com.MediHubAPI
- Existing response wrappers:
  - DataResponse
  - PageResponse
- Existing medicine master entity:
  - MdmMedicine
- Existing simple stock summary entity:
  - PharmacyStock

DB Mapping Design:
- Use `mdm_medicines` for medicine master data only:
  - id
  - form
  - brand
  - composition
  - is_active
  - code
- Use `pharmacy_stock` as one-row-per-medicine stock summary:
  - id
  - medicine_id
  - available_qty
  - reserved_qty
  - reorder_level
  - updated_at
- Use `pharmacy_stock_batch` as the source of truth for physical stock:
  - id
  - medicine_id
  - vendor_id
  - batch_no
  - expiry_date
  - purchase_price
  - mrp
  - selling_price
  - received_qty
  - available_qty
  - received_at
  - purchase_order_item_id
  - is_active
- Use `pharmacy_stock_transaction` as immutable stock ledger:
  - id
  - medicine_id
  - batch_id
  - transaction_type
  - qty_in
  - qty_out
  - balance_after
  - unit_cost
  - unit_price
  - reference_type
  - reference_id
  - reference_no
  - note
  - created_by
  - transaction_time
- Use `pharmacy_vendor` for vendor display in batch detail:
  - id
  - vendor_name
  - vendor_code
  - active

Entity relationship expectations:
- MdmMedicine 1 -> 1 PharmacyStock
- MdmMedicine 1 -> many PharmacyStockBatch
- MdmMedicine 1 -> many PharmacyStockTransaction
- PharmacyVendor 1 -> many PharmacyStockBatch
- PharmacyStockBatch 1 -> many PharmacyStockTransaction

Implement these backend capabilities:
- paginated stock list
- stock summary cards
- stock detail by medicine
- stock batches by medicine
- recent stock transactions by medicine

Required endpoints:
- GET /api/pharmacy/stocks
- GET /api/pharmacy/stocks/summary
- GET /api/pharmacy/stocks/{medicineId}
- GET /api/pharmacy/stocks/{medicineId}/batches
- GET /api/pharmacy/stocks/{medicineId}/transactions

Required response wrappers:
- list endpoints should return PageResponse<T>
- detail endpoints should return DataResponse<T>

Required filters for GET /api/pharmacy/stocks:
- q
- page
- size
- sort
- inStockOnly
- lowStockOnly
- expiringInDays
- vendorId
- form

Suggested DTOs:
- StockSummaryDto
  - totalMedicines
  - lowStockCount
  - outOfStockCount
  - expiringSoonCount
  - stockValue
- ManageStockRowDto
  - medicineId
  - medicineCode
  - medicineName
  - brand
  - form
  - availableQty
  - reservedQty
  - reorderLevel
  - sellingPrice
  - mrp
  - nearestExpiryDate
  - stockValue
  - stockStatus
  - lowStock
  - inStock
- MedicineStockDetailDto
  - medicineId
  - medicineCode
  - medicineName
  - brand
  - form
  - composition
  - availableQty
  - reservedQty
  - reorderLevel
  - nearestExpiryDate
  - stockValue
  - lowStock
- MedicineStockBatchDto
  - batchId
  - batchNo
  - vendorId
  - vendorName
  - expiryDate
  - purchasePrice
  - mrp
  - sellingPrice
  - receivedQty
  - availableQty
  - expired
- MedicineStockTransactionDto
  - transactionId
  - transactionTime
  - transactionType
  - batchNo
  - qtyIn
  - qtyOut
  - balanceAfter
  - referenceType
  - referenceId
  - referenceNo
  - note

API Contract:
- GET /api/pharmacy/stocks
  - response: PageResponse<ManageStockRowDto>
- GET /api/pharmacy/stocks/summary
  - response: DataResponse<StockSummaryDto>
- GET /api/pharmacy/stocks/{medicineId}
  - response: DataResponse<MedicineStockDetailDto>
- GET /api/pharmacy/stocks/{medicineId}/batches
  - response: PageResponse<MedicineStockBatchDto>
- GET /api/pharmacy/stocks/{medicineId}/transactions
  - response: PageResponse<MedicineStockTransactionDto>

Important rules:
- Do not store selling price or purchase price in MdmMedicine
- Keep PharmacyStock as summary only
- Batch table is the source for expiry, price, and batch drill-down
- Transactions table is the source for stock history
- Reuse `GlobalExceptionHandler` for validation and error responses
- Prefer `ValidationException` for bad query params and `MedicineNotFoundException` for missing medicine IDs

Suggested Backend Package Structure:
- controller/pharmacy/PharmacyStockController.java
- dto/pharmacy/StockSummaryDto.java
- dto/pharmacy/ManageStockRowDto.java
- dto/pharmacy/MedicineStockDetailDto.java
- dto/pharmacy/MedicineStockBatchDto.java
- dto/pharmacy/MedicineStockTransactionDto.java
- model/pharmacy/PharmacyStock.java
- model/pharmacy/PharmacyStockBatch.java
- model/pharmacy/PharmacyStockTransaction.java
- model/pharmacy/PharmacyVendor.java
- repository/pharmacy/PharmacyStockRepository.java
- repository/pharmacy/PharmacyStockBatchRepository.java
- repository/pharmacy/PharmacyStockTransactionRepository.java
- repository/projection/ManageStockRowProjection.java
- repository/projection/StockSummaryProjection.java
- repository/projection/MedicineBatchProjection.java
- repository/projection/MedicineTransactionProjection.java
- service/pharmacy/PharmacyStockQueryService.java
- service/pharmacy/impl/PharmacyStockQueryServiceImpl.java

Suggested Controller Design:
- Create `PharmacyStockController`
- Keep controller thin
- Expose:
  - GET /api/pharmacy/stocks
  - GET /api/pharmacy/stocks/summary
  - GET /api/pharmacy/stocks/{medicineId}
  - GET /api/pharmacy/stocks/{medicineId}/batches
  - GET /api/pharmacy/stocks/{medicineId}/transactions
- Use `PageResponse<T>` for paginated endpoints
- Use `DataResponse<T>` for detail endpoints

Suggested Repository Query Design:
- use projections for list and summary queries where useful
- keep stock list query optimized for dashboard reads
- keep batch query sorted by FEFO
- keep transactions query ordered by latest first

Stock list query should return:
- medicine identity fields from `mdm_medicines`
- stock summary fields from `pharmacy_stock`
- nearest expiry from `pharmacy_stock_batch`
- representative current `selling_price` and `mrp`
- computed `stock_value`

Summary query should return:
- total medicines
- low stock count
- out of stock count
- expiring soon count
- total stock value

Batches query should return:
- active batches for the medicine
- only rows with `available_qty > 0` by default
- vendor display fields
- sorted by `expiry_date asc`

Transactions query should return:
- latest movement first
- filter by `medicine_id`
- batch and reference metadata for audit display

Implement:
- entity classes if missing
- repositories
- projection interfaces if helpful
- DTOs
- query service
- controller

Suggested DTOs:
- StockSummaryDto
- ManageStockRowDto
- MedicineStockDetailDto
- MedicineStockBatchDto
- MedicineStockTransactionDto

Implementation requirements:
- support server-side pagination
- support server-side filtering
- use typed DTOs
- prefer repository query projections for list endpoints
- keep controller thin
- put business logic in service layer
- use readable native SQL or JPQL where appropriate
- include indexes in entity/table mapping where useful
- use bean validation on controller params where practical
- use `ValidationException` for service-layer validation with field-level details
- do not add ad hoc error response formats outside `GlobalExceptionHandler`

Validation and Exception Handling:
- reuse existing:
  - GlobalExceptionHandler
  - ValidationException
  - HospitalAPIException
  - ResourceNotFoundException
  - MedicineNotFoundException
- do not create a separate pharmacy exception handler unless required

Required Validation Rules:
- for GET /api/pharmacy/stocks:
  - page >= 0
  - size between 1 and 100
  - q minimum length 2 if provided
  - expiringInDays >= 0 if provided
  - vendorId > 0 if provided
  - form must be valid if enum-based
  - sort must be one of approved fields
- for GET /api/pharmacy/stocks/summary:
  - q minimum length 2 if provided
  - vendorId > 0 if provided
  - form must be valid if provided
- for GET /api/pharmacy/stocks/{medicineId}:
  - medicineId > 0
  - throw MedicineNotFoundException if medicine does not exist
- for GET /api/pharmacy/stocks/{medicineId}/batches:
  - medicineId > 0
  - page >= 0
  - size between 1 and 100
  - medicine must exist first
- for GET /api/pharmacy/stocks/{medicineId}/transactions:
  - medicineId > 0
  - page >= 0
  - size between 1 and 100
  - medicine must exist first

Required ValidationException Usage:
- use `ValidationException` for service-layer validation with field-level details
- include invalid field names such as:
  - page
  - size
  - q
  - sort
  - expiringInDays
  - vendorId
  - medicineId

GlobalExceptionHandler Integration:
- rely on existing handlers for:
  - MethodArgumentNotValidException
  - BindException
  - ConstraintViolationException
  - ValidationException
  - HospitalAPIException
  - DataIntegrityViolationException
  - generic Exception
- expected mapping:
  - invalid request params -> 400
  - invalid enum/form/sort -> 400
  - medicine not found -> 404

Suggested Validation Approach:
- controller layer:
  - use bean validation annotations for simple numeric/path validations
- service layer:
  - validate unsupported sort values
  - validate cross-field query rules
  - validate medicine existence before loading batches or transactions
  - throw ValidationException for business/query validation failures

Business Rules:
- lowStock = availableQty <= reorderLevel
- inStock = availableQty > 0
- stockStatus should be one of:
  - HEALTHY
  - LOW_STOCK
  - OUT_OF_STOCK
  - EXPIRING_SOON
- exclude inactive medicines unless explicitly requested
- exclude inactive batches from stock calculations
- expired batches should not contribute to usable stock if business rule requires that
- batch query should default to FEFO order
- manage stocks is read/query only; stock quantity changes must happen through receipt or adjustment flows

Expected files:
- PharmacyStockController.java
- PharmacyStockQueryService.java
- PharmacyStockQueryServiceImpl.java
- PharmacyStockBatch.java
- PharmacyStockTransaction.java
- PharmacyVendor.java
- PharmacyStockBatchRepository.java
- PharmacyStockTransactionRepository.java
- DTO classes for summary, row, detail, batch, and transaction
- projection interfaces if used

Acceptance criteria:
- stock list endpoint returns paginated rows for the UI
- summary endpoint returns top card totals
- stock detail endpoint returns medicine inventory snapshot
- batches endpoint returns batch drill-down data
- transactions endpoint returns recent movement history
- response shapes match the UI contract
- code follows existing package structure and response wrapper style

Also provide:
- any assumptions if existing entities differ
- brief explanation of query design
- note any migration or schema changes needed
- brief note on which validations are handled in controller vs service
- any new pharmacy exceptions only if existing shared exceptions are insufficient
```

## Notes

- This document is backend-only for the `Manage Stocks` page.
- Keep it aligned with `docs/pharmacy-stock-purchase-ui-design.md`.
- If needed later, create equivalent backend prompt docs for:
  - `Stock Adjustment`
  - `Pharmacy Vendors`
  - `Purchase Order`
  - `Pharmacy Transactions`

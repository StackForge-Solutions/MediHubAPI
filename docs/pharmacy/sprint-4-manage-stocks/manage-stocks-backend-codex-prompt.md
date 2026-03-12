# Manage Stocks Backend Codex Prompt

## Purpose

Use this prompt when asking Codex to implement the backend for `Sprint 4: Manage Stocks` in the pharmacy inventory module.

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

This prompt is only for the backend required by `Sprint 4: Manage Stocks`.

Main goals:

- expose stock summary cards API
- expose paginated manage stocks list API
- expose medicine stock detail API
- expose batch list API for a selected medicine
- expose recent stock transactions API for a selected medicine
- support server-side filtering, pagination, and sorting

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

## Business Rules

- `lowStock = availableQty <= reorderLevel`
- `inStock = availableQty > 0`
- `stockStatus` rules:
  - `OUT_OF_STOCK` if available qty is 0
  - `LOW_STOCK` if available qty is greater than 0 and less than or equal to reorder level
  - `EXPIRING_SOON` if nearest expiry is within selected threshold
  - otherwise `HEALTHY`
- exclude inactive medicines unless explicitly needed
- exclude inactive batches from active stock calculations
- expired batches should not contribute to usable stock if business rules require that

## Codex Prompt

```text
Implement the backend for `Sprint 4: Manage Stocks` in the pharmacy inventory module.

Project context:
- Java Spring Boot backend
- Existing project package base: com.MediHubAPI
- Existing response wrappers:
  - DataResponse
  - PageResponse
- Existing medicine master entity:
  - MdmMedicine
- Existing stock entities:
  - PharmacyStock
  - PharmacyStockBatch
  - PharmacyStockTransaction

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

Important rules:
- do not store selling price or purchase price in MdmMedicine
- keep PharmacyStock as summary only
- batch table is the source for expiry, price, and batch drill-down
- transactions table is the source for stock history
- reuse GlobalExceptionHandler for validation and error responses
- prefer ValidationException for bad query params and MedicineNotFoundException for missing medicine IDs

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
```

## Notes

- This document is backend-only for `Sprint 4: Manage Stocks`.
- Keep it aligned with `docs/pharmacy-development-sequence.md` and `docs/pharmacy-stock-purchase-ui-design.md`.
- This scope depends on Sprint 3 stock receipt and batch creation being available first.

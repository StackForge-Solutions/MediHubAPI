# Stock Receiving and Inventory Foundation Backend Codex Prompt

## Purpose

Use this prompt when asking Codex to implement the backend for `Sprint 3: Stock Receiving and Inventory Foundation` in the pharmacy inventory module.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-stock-receiving-backend
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-stock-receiving-backend
```

## Backend Scope

This prompt is only for the backend required by `Sprint 3: Stock Receiving and Inventory Foundation`.

Main goals:

- implement purchase-order stock receipt API
- create stock batches from receipt lines
- update summary stock per medicine
- write immutable stock transaction rows for receipts
- expose purchase-order receipt history for the PO detail page

## DB Mapping Design

Use these tables and relationships.

### 1. `pharmacy_purchase_order`

Purpose:
- Purchase-order header and receipt lifecycle state

Relevant fields:

- `id`
- `vendor_id`
- `po_number`
- `order_date`
- `invoice_number`
- `invoice_date`
- `status`
- `note`

Status flow:

- `DRAFT`
- `APPROVED`
- `PARTIALLY_RECEIVED`
- `RECEIVED`
- `CANCELLED`

### 2. `pharmacy_purchase_order_item`

Purpose:
- Purchase-order line items used to calculate pending receipt quantity

Relevant fields:

- `id`
- `purchase_order_id`
- `medicine_id`
- `ordered_qty`
- `received_qty`
- `purchase_price`
- `mrp`
- `selling_price`

### 3. `pharmacy_stock`

Purpose:
- Fast summary table with current available inventory per medicine

Relevant fields:

- `id`
- `medicine_id`
- `available_qty`
- `reserved_qty`
- `reorder_level`
- `updated_at`

Notes:

- This is a summary table only.
- Receipt posting must increase `available_qty`.
- Keep this table aligned with batch and transaction writes.

### 4. `pharmacy_stock_batch`

Purpose:
- Source of truth for received physical inventory by batch

Relevant fields:

- `id`
- `medicine_id`
- `vendor_id`
- `batch_no`
- `expiry_date`
- `purchase_price`
- `mrp`
- `selling_price`
- `received_qty`
- `available_qty`
- `received_at`
- `purchase_order_item_id`
- `is_active`

Indexes:

- index on `medicine_id`
- index on `vendor_id`
- index on `expiry_date`
- unique recommendation: `medicine_id + batch_no + expiry_date`

### 5. `pharmacy_stock_transaction`

Purpose:
- Immutable stock movement ledger

Relevant fields:

- `id`
- `medicine_id`
- `batch_id`
- `transaction_type`
- `qty_in`
- `qty_out`
- `balance_after`
- `unit_cost`
- `unit_price`
- `reference_type`
- `reference_id`
- `reference_no`
- `note`
- `created_by`
- `transaction_time`

Notes:

- Purchase receipt must write `PURCHASE_RECEIPT` rows.
- Reference metadata should point back to the purchase order.

### 6. Supporting tables

- `pharmacy_vendor`
- `mdm_medicines`

### Entity Relationships

- `PharmacyPurchaseOrder 1 -> many PharmacyPurchaseOrderItem`
- `PharmacyPurchaseOrderItem many -> 1 MdmMedicine`
- `PharmacyPurchaseOrder many -> 1 PharmacyVendor`
- `MdmMedicine 1 -> 1 PharmacyStock`
- `MdmMedicine 1 -> many PharmacyStockBatch`
- `MdmMedicine 1 -> many PharmacyStockTransaction`
- `PharmacyStockBatch many -> 1 PharmacyVendor`
- `PharmacyStockBatch 1 -> many PharmacyStockTransaction`

## Suggested DTOs

### Receive Request

```json
{
  "receiptDate": "2026-03-12",
  "invoiceNumber": "INV-889",
  "invoiceDate": "2026-03-12",
  "note": "Weekly vendor delivery",
  "items": [
    {
      "purchaseOrderItemId": 801,
      "batchNo": "BATCH-APR-01",
      "expiryDate": "2026-08-31",
      "receivedQty": 100,
      "purchasePrice": 1.75,
      "mrp": 3.0,
      "sellingPrice": 2.5,
      "note": "Shelf A"
    }
  ]
}
```

### Receive Response

```json
{
  "data": {
    "purchaseOrderId": 501,
    "status": "RECEIVED",
    "receivedItemCount": 1,
    "receivedQty": 100
  }
}
```

### Receipt History Row

```json
{
  "batchId": 9001,
  "purchaseOrderItemId": 801,
  "medicineId": 101,
  "medicineName": "Paracetamol 500",
  "batchNo": "BATCH-APR-01",
  "expiryDate": "2026-08-31",
  "receivedQty": 100,
  "purchasePrice": 1.75,
  "mrp": 3.0,
  "sellingPrice": 2.5,
  "receivedAt": "2026-03-12T11:00:00Z"
}
```

## API Contract

Base path:

```text
/api/pharmacy/purchase-orders
```

### 1. POST `/api/pharmacy/purchase-orders/{id}/receive`

Purpose:
- Receive stock for an approved or partially received PO

Request body:
- `PurchaseOrderReceiveRequest`

Response:
- `DataResponse<PurchaseOrderReceiveResponseDto>`

### 2. GET `/api/pharmacy/purchase-orders/{id}/receipts`

Purpose:
- Receipt history for PO detail page

Query params:

- `page`: int
- `size`: int

Response:
- `PageResponse<PurchaseOrderReceiptHistoryRowDto>`

## Suggested Backend Package Structure

```text
src/main/java/com/MediHubAPI/
  controller/pharmacy/
    PharmacyPurchaseOrderReceiptController.java
  dto/pharmacy/
    PurchaseOrderReceiveRequest.java
    PurchaseOrderReceiveRequestItem.java
    PurchaseOrderReceiveResponseDto.java
    PurchaseOrderReceiptHistoryRowDto.java
  model/pharmacy/
    PharmacyStock.java
    PharmacyStockBatch.java
    PharmacyStockTransaction.java
    PharmacyPurchaseOrder.java
    PharmacyPurchaseOrderItem.java
  repository/pharmacy/
    PharmacyPurchaseOrderRepository.java
    PharmacyPurchaseOrderItemRepository.java
    PharmacyStockRepository.java
    PharmacyStockBatchRepository.java
    PharmacyStockTransactionRepository.java
  repository/projection/
    PurchaseOrderReceiptHistoryProjection.java
  service/pharmacy/
    PharmacyPurchaseOrderReceiptService.java
  service/pharmacy/impl/
    PharmacyPurchaseOrderReceiptServiceImpl.java
```

## Suggested Controller Design

Create:

- `PharmacyPurchaseOrderReceiptController`

Endpoints:

- `POST /api/pharmacy/purchase-orders/{id}/receive`
- `GET /api/pharmacy/purchase-orders/{id}/receipts`

Use:

- `DataResponse<T>` for receive action
- `PageResponse<T>` for receipt history

## Suggested Repository and Service Design

Receipt service should:

- load purchase order with locking before posting receipt
- validate PO status before any write
- load only requested PO items for receipt
- validate pending quantity against `ordered_qty - received_qty`
- create one stock batch per received line
- increment `pharmacy_stock.available_qty`
- write one `PharmacyStockTransaction` row per received line
- update PO item `received_qty`
- set PO status to `PARTIALLY_RECEIVED` or `RECEIVED`

Receipt history query should return:

- medicine name
- batch number
- expiry date
- received quantity
- cost and price fields
- received timestamp

## Validation and Exception Handling

Use the existing shared exception handling infrastructure in:

- `src/main/java/com/MediHubAPI/exception/GlobalExceptionHandler.java`
- `src/main/java/com/MediHubAPI/exception/ValidationException.java`
- `src/main/java/com/MediHubAPI/exception/HospitalAPIException.java`
- `src/main/java/com/MediHubAPI/exception/pharmacy/PurchaseOrderNotFoundException.java`
- `src/main/java/com/MediHubAPI/exception/pharmacy/PurchaseOrderReceiptNotAllowedException.java`
- `src/main/java/com/MediHubAPI/exception/pharmacy/PurchaseOrderItemNotFoundException.java`
- `src/main/java/com/MediHubAPI/exception/pharmacy/OverReceiptException.java`

Do not create a separate pharmacy exception handler for this scope unless absolutely necessary.

### Required Validation Rules

#### POST `/api/pharmacy/purchase-orders/{id}/receive`

Validate:

- `id > 0`
- purchase order must exist
- PO status must be `APPROVED` or `PARTIALLY_RECEIVED`
- request `items` must not be empty
- every item must have:
  - `purchaseOrderItemId > 0`
  - `batchNo` required
  - `expiryDate` required
  - `receivedQty > 0`
  - `purchasePrice >= 0`
  - `mrp >= 0`
  - `sellingPrice >= 0`
- `invoiceDate <= receiptDate` if both are provided
- `expiryDate >= receiptDate`
- `purchaseOrderItemId` values must not repeat in one request
- received quantity must not exceed pending quantity
- batch number should be unique for the same medicine and expiry date

#### GET `/api/pharmacy/purchase-orders/{id}/receipts`

Validate:

- `id > 0`
- purchase order must exist
- `page >= 0`
- `size between 1 and 100`

### Expected Exception Mapping

- invalid request or validation failure -> `400 Bad Request`
- purchase order or line item not found -> `404 Not Found`
- invalid receipt state or over-receipt -> `409 Conflict`

## Business Rules

- inventory is created only when receipt is posted
- do not allow stock receipt on `DRAFT` or `CANCELLED` POs
- allow partial receipt and keep remaining quantity pending
- update PO header invoice fields from receipt request when supplied
- receipt lines create new active batches with `available_qty = received_qty`
- each receipt line creates a `PURCHASE_RECEIPT` ledger row
- stock summary must stay consistent with posted batches and transactions
- receipt history is read-only audit data

## Codex Prompt

```text
Implement the backend for `Sprint 3: Stock Receiving and Inventory Foundation` in the pharmacy inventory module.

Project context:
- Java Spring Boot backend
- Existing project package base: com.MediHubAPI
- Existing response wrappers:
  - DataResponse
  - PageResponse
- Purchase orders already exist and move through:
  - DRAFT
  - APPROVED
  - PARTIALLY_RECEIVED
  - RECEIVED
  - CANCELLED

Implement these backend capabilities:
- receive stock against a purchase order
- create stock batch rows from receipt lines
- update summary stock quantities
- write immutable stock transaction rows for receipt posting
- expose receipt history for a purchase order

Required endpoints:
- POST /api/pharmacy/purchase-orders/{id}/receive
- GET /api/pharmacy/purchase-orders/{id}/receipts

Required response wrappers:
- receive action should return DataResponse<PurchaseOrderReceiveResponseDto>
- receipt history should return PageResponse<PurchaseOrderReceiptHistoryRowDto>

DB expectations:
- use pharmacy_purchase_order and pharmacy_purchase_order_item for PO state and pending quantity
- use pharmacy_stock as one-row-per-medicine summary
- use pharmacy_stock_batch as source of truth for received inventory batches
- use pharmacy_stock_transaction as immutable ledger
- use pharmacy_vendor and mdm_medicines as supporting references

Suggested DTOs:
- PurchaseOrderReceiveRequest
  - receiptDate
  - invoiceNumber
  - invoiceDate
  - note
  - items
- PurchaseOrderReceiveRequestItem
  - purchaseOrderItemId
  - batchNo
  - expiryDate
  - receivedQty
  - purchasePrice
  - mrp
  - sellingPrice
  - note
- PurchaseOrderReceiveResponseDto
  - purchaseOrderId
  - status
  - receivedItemCount
  - receivedQty
- PurchaseOrderReceiptHistoryRowDto
  - batchId
  - purchaseOrderItemId
  - medicineId
  - medicineName
  - batchNo
  - expiryDate
  - receivedQty
  - purchasePrice
  - mrp
  - sellingPrice
  - receivedAt

Suggested Backend Package Structure:
- controller/pharmacy/PharmacyPurchaseOrderReceiptController.java
- dto/pharmacy/PurchaseOrderReceiveRequest.java
- dto/pharmacy/PurchaseOrderReceiveRequestItem.java
- dto/pharmacy/PurchaseOrderReceiveResponseDto.java
- dto/pharmacy/PurchaseOrderReceiptHistoryRowDto.java
- model/pharmacy/PharmacyStock.java
- model/pharmacy/PharmacyStockBatch.java
- model/pharmacy/PharmacyStockTransaction.java
- repository/pharmacy/PharmacyPurchaseOrderRepository.java
- repository/pharmacy/PharmacyPurchaseOrderItemRepository.java
- repository/pharmacy/PharmacyStockRepository.java
- repository/pharmacy/PharmacyStockBatchRepository.java
- repository/pharmacy/PharmacyStockTransactionRepository.java
- repository/projection/PurchaseOrderReceiptHistoryProjection.java
- service/pharmacy/PharmacyPurchaseOrderReceiptService.java
- service/pharmacy/impl/PharmacyPurchaseOrderReceiptServiceImpl.java

Implementation requirements:
- keep controller thin
- perform receipt posting in a transactional service
- lock the purchase order before posting receipt
- validate PO state before any inventory write
- reject over-receipt
- reject duplicate line items inside one receive request
- reject duplicate batch number for the same medicine and expiry date
- increment stock summary from received quantity
- create one batch per receipt line
- create one PURCHASE_RECEIPT transaction row per receipt line
- update PO status to PARTIALLY_RECEIVED or RECEIVED after posting

Validation and Exception Handling:
- reuse GlobalExceptionHandler
- reuse ValidationException for field-level validation errors
- reuse PurchaseOrderNotFoundException for missing PO
- reuse PurchaseOrderReceiptNotAllowedException for invalid PO status
- reuse PurchaseOrderItemNotFoundException when a line item does not belong to the PO
- reuse OverReceiptException when request quantity exceeds pending quantity
- do not add a separate pharmacy exception handler unless required

Required Validation Rules:
- POST /receive:
  - purchaseOrderId > 0
  - PO exists
  - PO status must be APPROVED or PARTIALLY_RECEIVED
  - items not empty
  - purchaseOrderItemId > 0
  - batchNo required
  - expiryDate required
  - receivedQty > 0
  - purchasePrice, mrp, sellingPrice >= 0
  - invoiceDate cannot be after receiptDate
  - expiryDate cannot be before receiptDate
  - duplicate purchaseOrderItemId not allowed in a single request
  - receivedQty cannot exceed pendingQty
- GET /receipts:
  - purchaseOrderId > 0
  - page >= 0
  - size between 1 and 100

Business Rules:
- inventory exists only after purchase receipt is posted
- do not allow receipt on DRAFT or CANCELLED purchase orders
- keep receipt history queryable from PO detail
- stock summary is derived operational data, not audit history
- transaction table is immutable audit history

Acceptance criteria:
- receive endpoint creates stock batches and stock ledger rows
- stock summary quantity increases after receipt
- PO item received quantities are updated
- PO status becomes PARTIALLY_RECEIVED or RECEIVED correctly
- receipt history endpoint returns posted receipt rows
- validation and error handling follow existing shared infrastructure

Also provide:
- any assumptions if existing entities differ
- brief explanation of receipt posting flow
- note any schema/index changes required
- brief note on controller-layer vs service-layer validation
```

## Notes

- This document is backend-only for `Sprint 3: Stock Receiving and Inventory Foundation`.
- Keep it aligned with `docs/pharmacy-development-sequence.md` and `docs/pharmacy-stock-purchase-ui-design.md`.
- This scope establishes the real inventory foundation used later by `Manage Stocks`, `Stock Adjustment`, and `Pharmacy Transactions`.

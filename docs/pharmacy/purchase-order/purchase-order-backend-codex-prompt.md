# Purchase Order Backend Codex Prompt

## Purpose

Use this prompt when asking Codex to implement the backend for the `Purchase Order` core module in the pharmacy inventory system.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-purchase-order-backend
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-purchase-order-backend
```

## Backend Scope

This prompt is only for the backend required by `Sprint Purchase Order`.

Main goals:

- create purchase orders
- update draft purchase orders
- list purchase orders
- fetch purchase-order detail
- approve purchase orders
- cancel purchase orders

## DB Mapping Design

Use these tables:

### 1. `pharmacy_purchase_order`

Purpose:
- Purchase order header

Fields:

- `id`
- `vendor_id`
- `po_number`
- `order_date`
- `invoice_number`
- `invoice_date`
- `status`
- `subtotal`
- `tax_amount`
- `discount_amount`
- `net_amount`
- `note`
- `created_at`
- `updated_at`

### 2. `pharmacy_purchase_order_item`

Purpose:
- Purchase order line items

Fields:

- `id`
- `purchase_order_id`
- `medicine_id`
- `ordered_qty`
- `received_qty`
- `purchase_price`
- `mrp`
- `selling_price`

### 3. Supporting tables

- `pharmacy_vendor`
- `mdm_medicines`

Entity relationship expectations:

- PurchaseOrder many -> 1 PharmacyVendor
- PurchaseOrder 1 -> many PurchaseOrderItem
- PurchaseOrderItem many -> 1 MdmMedicine

## Suggested DTOs

- `PurchaseOrderCreateRequest`
- `PurchaseOrderUpdateRequest`
- `PurchaseOrderItemRequest`
- `PurchaseOrderRowDto`
- `PurchaseOrderDetailDto`
- `PurchaseOrderItemDto`
- `PurchaseOrderActionResponseDto`

## API Contract

Base path:

```text
/api/pharmacy/purchase-orders
```

Required endpoints:

- `GET /api/pharmacy/purchase-orders`
- `POST /api/pharmacy/purchase-orders`
- `PUT /api/pharmacy/purchase-orders/{id}`
- `GET /api/pharmacy/purchase-orders/{id}`
- `POST /api/pharmacy/purchase-orders/{id}/approve`
- `POST /api/pharmacy/purchase-orders/{id}/cancel`

Required response wrappers:

- list endpoints -> `PageResponse<T>`
- detail/action endpoints -> `DataResponse<T>`

## Suggested Backend Package Structure

```text
src/main/java/com/MediHubAPI/
  controller/pharmacy/
    PharmacyPurchaseOrderController.java
  dto/pharmacy/
    PurchaseOrderCreateRequest.java
    PurchaseOrderUpdateRequest.java
    PurchaseOrderItemRequest.java
    PurchaseOrderRowDto.java
    PurchaseOrderDetailDto.java
    PurchaseOrderItemDto.java
    PurchaseOrderActionResponseDto.java
  model/pharmacy/
    PharmacyPurchaseOrder.java
    PharmacyPurchaseOrderItem.java
  repository/pharmacy/
    PharmacyPurchaseOrderRepository.java
    PharmacyPurchaseOrderItemRepository.java
    PharmacyVendorRepository.java
    MdmMedicineRepository.java
  repository/projection/
    PurchaseOrderRowProjection.java
  service/pharmacy/
    PharmacyPurchaseOrderService.java
  service/pharmacy/impl/
    PharmacyPurchaseOrderServiceImpl.java
  exception/pharmacy/
    PurchaseOrderNotFoundException.java
    PurchaseOrderStateException.java
```

## Suggested Controller Design

Create:

- `PharmacyPurchaseOrderController`

Keep controller thin.

Endpoints:

- list
- create
- update draft
- detail
- approve
- cancel

Use:

- `PageResponse<T>` for list
- `DataResponse<T>` for create/update/detail/actions

## Suggested Repository Query Design

### PO list query should return

- po number
- vendor name
- order date
- status
- item count
- ordered qty
- received qty
- net amount

### PO detail query should return

- header fields
- vendor fields
- item list
- calculated pending quantities

### Status action queries should support

- validating current status
- updating status safely

## Validation and Exception Handling

Use the existing shared exception handling infrastructure:

- `GlobalExceptionHandler`
- `ValidationException`
- `HospitalAPIException`

Do not create a separate pharmacy exception handler unless required.

### Required Validation Rules

#### GET `/api/pharmacy/purchase-orders`

Validate:

- `page >= 0`
- `size between 1 and 100`
- `q` minimum length 2 if provided
- `status` valid enum if provided
- `vendorId > 0` if provided
- `fromDate <= toDate` if both provided
- `sort` must be one of approved fields

#### POST and PUT

Validate:

- `vendorId` required and valid
- `orderDate` required
- items list must not be empty
- every item must have:
  - `medicineId`
  - `orderedQty > 0`
  - `purchasePrice >= 0`
  - `mrp >= 0`
  - `sellingPrice >= 0`
- PO can be updated only in `DRAFT`

#### Approve

Validate:

- PO exists
- PO status is `DRAFT`
- PO has at least one valid item

#### Cancel

Validate:

- PO exists
- PO is not already `RECEIVED`

### GlobalExceptionHandler Integration

Expected mapping:

- invalid request/body -> `400`
- PO not found -> `404`
- invalid state transition -> `409`

## Business Rules

- new PO starts as `DRAFT`
- only `DRAFT` PO can be edited
- only `DRAFT` PO can be approved
- `RECEIVED` PO cannot be cancelled
- totals should be recalculated server-side
- vendor and medicine references must be valid

## Codex Prompt

```text
Implement the backend for the `Purchase Order` core module in the pharmacy inventory system.

Project context:
- Java Spring Boot backend
- Existing project package base: com.MediHubAPI
- Existing response wrappers:
  - DataResponse
  - PageResponse

DB Mapping Design:
- Use `pharmacy_purchase_order` as purchase-order header
- Use `pharmacy_purchase_order_item` as purchase-order line items
- Use `pharmacy_vendor` for vendor reference
- Use `mdm_medicines` for medicine reference

Implement these backend capabilities:
- list purchase orders
- create draft purchase orders
- update draft purchase orders
- fetch purchase-order detail
- approve purchase orders
- cancel purchase orders

Required endpoints:
- GET /api/pharmacy/purchase-orders
- POST /api/pharmacy/purchase-orders
- PUT /api/pharmacy/purchase-orders/{id}
- GET /api/pharmacy/purchase-orders/{id}
- POST /api/pharmacy/purchase-orders/{id}/approve
- POST /api/pharmacy/purchase-orders/{id}/cancel

Required response wrappers:
- list endpoints should return PageResponse<T>
- detail/action endpoints should return DataResponse<T>

Suggested DTOs:
- PurchaseOrderCreateRequest
- PurchaseOrderUpdateRequest
- PurchaseOrderItemRequest
- PurchaseOrderRowDto
- PurchaseOrderDetailDto
- PurchaseOrderItemDto
- PurchaseOrderActionResponseDto

Important rules:
- new PO starts as DRAFT
- only DRAFT PO can be edited
- only DRAFT PO can be approved
- RECEIVED PO cannot be cancelled
- totals must be recalculated server-side
- reuse GlobalExceptionHandler for validation and errors

Suggested Backend Package Structure:
- controller/pharmacy/PharmacyPurchaseOrderController.java
- dto/pharmacy/PurchaseOrderCreateRequest.java
- dto/pharmacy/PurchaseOrderUpdateRequest.java
- dto/pharmacy/PurchaseOrderItemRequest.java
- dto/pharmacy/PurchaseOrderRowDto.java
- dto/pharmacy/PurchaseOrderDetailDto.java
- dto/pharmacy/PurchaseOrderItemDto.java
- dto/pharmacy/PurchaseOrderActionResponseDto.java
- model/pharmacy/PharmacyPurchaseOrder.java
- model/pharmacy/PharmacyPurchaseOrderItem.java
- repository/pharmacy/PharmacyPurchaseOrderRepository.java
- repository/pharmacy/PharmacyPurchaseOrderItemRepository.java
- service/pharmacy/PharmacyPurchaseOrderService.java
- service/pharmacy/impl/PharmacyPurchaseOrderServiceImpl.java
- exception/pharmacy/PurchaseOrderNotFoundException.java
- exception/pharmacy/PurchaseOrderStateException.java

Suggested Repository Query Design:
- list query should return PO header summary and counts
- detail query should return PO header and items
- state transition logic should validate current status before changing it

Validation and Exception Handling:
- reuse existing:
  - GlobalExceptionHandler
  - ValidationException
  - HospitalAPIException
- do not create a separate pharmacy exception handler unless required

Required Validation Rules:
- GET list:
  - page >= 0
  - size between 1 and 100
  - q minimum length 2 if provided
  - status valid if provided
  - vendorId > 0 if provided
  - fromDate <= toDate
  - sort valid
- POST/PUT:
  - vendorId required
  - orderDate required
  - items not empty
  - medicineId required
  - orderedQty > 0
  - price fields >= 0
  - only DRAFT can be updated
- approve:
  - PO exists
  - status is DRAFT
- cancel:
  - PO exists
  - RECEIVED cannot be cancelled

Business Rules:
- PO totals are computed server-side
- vendor and medicine references must be valid
- PO item receivedQty starts at 0
- approved PO should be ready for stock receipt flow

Implement:
- DTOs
- service
- controller
- repository methods needed for list/detail/actions
- exception classes if missing

Acceptance criteria:
- PO list endpoint returns paginated rows
- create draft works
- update draft works
- detail endpoint returns PO with items
- approve and cancel actions enforce valid state transitions
- response shapes match UI needs

Also provide:
- any assumptions if receipt flow already exists
- brief explanation of totals calculation
- note any schema changes needed
```

## Notes

- This prompt is intentionally scoped to `Sprint Purchase Order`.
- Use `docs/pharmacy-stock-purchase-ui-design.md` for broader consistency.


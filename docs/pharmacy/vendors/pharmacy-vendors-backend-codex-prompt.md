# Pharmacy Vendors Backend Codex Prompt

## Purpose

Use this prompt when asking Codex to implement the backend for the `Pharmacy Vendors` module in the pharmacy inventory system.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-vendors-backend
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-vendors-backend
```

## Backend Scope

This prompt is only for the backend required by the `Pharmacy Vendors` UI.

Main goals:

- Expose vendor list API with pagination and filters
- Expose vendor detail API
- Expose vendor create API
- Expose vendor update API
- Expose vendor-related recent purchase orders API
- Support validation and conflict handling through the shared exception flow

## DB Mapping Design

Use a dedicated vendor master table.

### 1. `pharmacy_vendor`

Purpose:
- Supplier master data for pharmacy purchasing

Columns:

- `id` bigint primary key
- `vendor_code` varchar(50) not null unique
- `vendor_name` varchar(255) not null
- `contact_person` varchar(255) null
- `phone` varchar(20) null
- `email` varchar(255) null
- `gst_no` varchar(30) null
- `drug_license_no` varchar(50) null
- `address_line1` varchar(255) null
- `address_line2` varchar(255) null
- `city` varchar(100) null
- `state` varchar(100) null
- `pincode` varchar(20) null
- `payment_terms_days` int not null default 0
- `active` bit not null default 1
- `created_at` datetime null
- `updated_at` datetime null

Indexes:

- index on `vendor_name`
- index on `vendor_code`
- index on `phone`
- index on `gst_no`
- index on `city`

Java entity:

- `PharmacyVendor`

### 2. Optional future relation: `pharmacy_purchase_order`

Purpose:
- Used only for vendor detail stats and recent PO listing

Expected fields for vendor-linked summary:

- `id`
- `vendor_id`
- `po_number`
- `order_date`
- `status`
- `net_amount`

If purchase order is not yet implemented:

- return empty recent-PO list
- return zero stats in vendor detail response

## Suggested DTOs

### Vendor List Row DTO

```json
{
  "vendorId": 4,
  "vendorCode": "VND-004",
  "vendorName": "ABC Pharma",
  "contactPerson": "Raj Kumar",
  "phone": "9876543210",
  "email": "raj@abcpharma.in",
  "gstNo": "29ABCDE1234F1Z7",
  "city": "Bengaluru",
  "paymentTermsDays": 30,
  "active": true,
  "outstandingPurchaseOrders": 3,
  "lastPurchaseDate": "2026-03-10"
}
```

### Vendor Detail DTO

```json
{
  "vendorId": 4,
  "vendorCode": "VND-004",
  "vendorName": "ABC Pharma",
  "contactPerson": "Raj Kumar",
  "phone": "9876543210",
  "email": "raj@abcpharma.in",
  "gstNo": "29ABCDE1234F1Z7",
  "drugLicenseNo": "DL-10012",
  "addressLine1": "MG Road",
  "addressLine2": "Near Metro",
  "city": "Bengaluru",
  "state": "Karnataka",
  "pincode": "560001",
  "paymentTermsDays": 30,
  "active": true,
  "stats": {
    "totalPurchaseOrders": 48,
    "pendingPurchaseOrders": 3,
    "totalPurchaseValue": 523400.0,
    "lastPurchaseDate": "2026-03-10"
  }
}
```

### Vendor Create/Update Request DTO

```json
{
  "vendorCode": "VND-004",
  "vendorName": "ABC Pharma",
  "contactPerson": "Raj Kumar",
  "phone": "9876543210",
  "email": "raj@abcpharma.in",
  "gstNo": "29ABCDE1234F1Z7",
  "drugLicenseNo": "DL-10012",
  "addressLine1": "MG Road",
  "addressLine2": "Near Metro",
  "city": "Bengaluru",
  "state": "Karnataka",
  "pincode": "560001",
  "paymentTermsDays": 30,
  "active": true
}
```

### Vendor Purchase Order Row DTO

```json
{
  "purchaseOrderId": 501,
  "poNumber": "PO-20260312-001",
  "orderDate": "2026-03-12",
  "status": "APPROVED",
  "itemCount": 5,
  "netAmount": 10450.0
}
```

## API Contract

Base path:

```text
/api/pharmacy/vendors
```

### 1. GET `/api/pharmacy/vendors`

Purpose:
- Paginated vendor list for the main vendor page

Query params:

- `q`: string
- `active`: boolean
- `page`: int
- `size`: int
- `sort`: string

Response:
- `PageResponse<PharmacyVendorRowDto>`

### 2. POST `/api/pharmacy/vendors`

Purpose:
- Create a new vendor

Response:
- `DataResponse<PharmacyVendorDetailDto>` or a compact created DTO

### 3. PUT `/api/pharmacy/vendors/{vendorId}`

Purpose:
- Update an existing vendor

Response:
- `DataResponse<PharmacyVendorDetailDto>`

### 4. GET `/api/pharmacy/vendors/{vendorId}`

Purpose:
- Vendor detail page

Response:
- `DataResponse<PharmacyVendorDetailDto>`

### 5. GET `/api/pharmacy/vendors/{vendorId}/purchase-orders`

Purpose:
- Vendor detail related purchase orders table

Query params:

- `page`: int
- `size`: int

Response:
- `PageResponse<PharmacyVendorPurchaseOrderRowDto>`

## Suggested Backend Package Structure

```text
src/main/java/com/MediHubAPI/
  controller/pharmacy/
    PharmacyVendorController.java
  dto/pharmacy/
    PharmacyVendorRowDto.java
    PharmacyVendorDetailDto.java
    PharmacyVendorUpsertRequest.java
    PharmacyVendorPurchaseOrderRowDto.java
    PharmacyVendorStatsDto.java
  model/pharmacy/
    PharmacyVendor.java
  repository/pharmacy/
    PharmacyVendorRepository.java
  repository/projection/
    PharmacyVendorRowProjection.java
    PharmacyVendorPurchaseOrderProjection.java
    PharmacyVendorStatsProjection.java
  service/pharmacy/
    PharmacyVendorService.java
  service/pharmacy/impl/
    PharmacyVendorServiceImpl.java
  exception/pharmacy/
    PharmacyVendorNotFoundException.java
    DuplicateVendorCodeException.java
```

## Suggested Controller Design

Create:

- `PharmacyVendorController`

Endpoints:

- `GET /api/pharmacy/vendors`
- `POST /api/pharmacy/vendors`
- `PUT /api/pharmacy/vendors/{vendorId}`
- `GET /api/pharmacy/vendors/{vendorId}`
- `GET /api/pharmacy/vendors/{vendorId}/purchase-orders`

Use:

- `PageResponse<T>` for paginated list endpoints
- `DataResponse<T>` for single payload endpoints

## Suggested Repository Query Design

### Vendor list query should return

- vendor identity fields from `pharmacy_vendor`
- contact fields
- city
- active flag
- payment terms
- derived outstanding PO count if PO table exists
- derived last purchase date if PO table exists

### Vendor detail query should return

- full vendor profile
- derived stats if PO table exists

### Vendor purchase orders query should return

- recent purchase orders for selected vendor
- latest first or order date desc

## Validation and Exception Handling

Use the existing shared exception handling infrastructure in:

- `src/main/java/com/MediHubAPI/exception/GlobalExceptionHandler.java`
- `src/main/java/com/MediHubAPI/exception/ValidationException.java`
- `src/main/java/com/MediHubAPI/exception/ConflictException.java`
- `src/main/java/com/MediHubAPI/exception/HospitalAPIException.java`

Prefer integrating with the existing `GlobalExceptionHandler`.

### Required Validation Rules

#### GET `/api/pharmacy/vendors`

Validate:

- `page >= 0`
- `size > 0`
- `size <= 100`
- `q` minimum length 2 if provided and non-blank
- `sort` should allow only approved sort fields

Recommended allowed sort fields:

- `vendorName`
- `vendorCode`
- `city`
- `paymentTermsDays`
- `lastPurchaseDate`

#### POST and PUT `/api/pharmacy/vendors`

Validate:

- `vendorCode` required and max length 50
- `vendorName` required and max length 255
- `phone` max length 20 if provided
- `email` valid format if provided
- `gstNo` max length 30 if provided
- `drugLicenseNo` max length 50 if provided
- `paymentTermsDays >= 0`
- `city`, `state`, `pincode` length-safe if provided
- `vendorCode` unique
- `gstNo` unique if business requires it

#### GET `/api/pharmacy/vendors/{vendorId}`

Validate:

- `vendorId > 0`
- vendor must exist, otherwise throw not-found exception

#### GET `/api/pharmacy/vendors/{vendorId}/purchase-orders`

Validate:

- `vendorId > 0`
- `page >= 0`
- `size > 0`
- `size <= 100`
- vendor must exist first

### Recommended Custom Exceptions

- `PharmacyVendorNotFoundException extends HospitalAPIException`
- `DuplicateVendorCodeException extends ConflictException` or `HospitalAPIException`
- `ValidationException` for service-layer field validation

### Required ValidationException Usage

When request or query parameters are invalid, throw `ValidationException` with field-level details.

Example fields:

- `vendorCode`
- `vendorName`
- `phone`
- `email`
- `gstNo`
- `paymentTermsDays`
- `sort`

### GlobalExceptionHandler Integration

Expected mapping:

- invalid request params -> `400 Bad Request`
- invalid body fields -> `400 Bad Request`
- duplicate vendor code or GST conflict -> `409 Conflict`
- vendor not found -> `404 Not Found`

### Suggested Validation Approach

Controller layer:

- use bean validation annotations for request DTOs and path/query params

Service layer:

- validate duplicate vendor code and GST rules
- validate unsupported sort values
- validate vendor existence before loading detail or PO history
- throw `ValidationException` for business/query validation failures

## Business Rules

- `vendorCode` should be unique
- inactive vendors remain visible in history but may be blocked from new PO creation
- do not hard delete vendors if they are referenced by purchase data
- if purchase order module is not ready, vendor detail stats may safely return zero values

## Codex Prompt

```text
Implement the backend for the `Pharmacy Vendors` module in the pharmacy inventory system.

Project context:
- Java Spring Boot backend
- Existing project package base: com.MediHubAPI
- Existing response wrappers:
  - DataResponse
  - PageResponse

DB Mapping Design:
- Use `pharmacy_vendor` for supplier master data with fields:
  - id
  - vendor_code
  - vendor_name
  - contact_person
  - phone
  - email
  - gst_no
  - drug_license_no
  - address_line1
  - address_line2
  - city
  - state
  - pincode
  - payment_terms_days
  - active
  - created_at
  - updated_at

Implement these backend capabilities:
- paginated vendor list
- vendor detail
- create vendor
- update vendor
- vendor purchase-order history endpoint

Required endpoints:
- GET /api/pharmacy/vendors
- POST /api/pharmacy/vendors
- PUT /api/pharmacy/vendors/{vendorId}
- GET /api/pharmacy/vendors/{vendorId}
- GET /api/pharmacy/vendors/{vendorId}/purchase-orders

Required response wrappers:
- list endpoints should return PageResponse<T>
- detail endpoints should return DataResponse<T>

Required filters for GET /api/pharmacy/vendors:
- q
- active
- page
- size
- sort

Suggested DTOs:
- PharmacyVendorRowDto
- PharmacyVendorDetailDto
- PharmacyVendorUpsertRequest
- PharmacyVendorPurchaseOrderRowDto
- PharmacyVendorStatsDto

API Contract:
- GET /api/pharmacy/vendors
  - response: PageResponse<PharmacyVendorRowDto>
- POST /api/pharmacy/vendors
  - response: DataResponse<PharmacyVendorDetailDto>
- PUT /api/pharmacy/vendors/{vendorId}
  - response: DataResponse<PharmacyVendorDetailDto>
- GET /api/pharmacy/vendors/{vendorId}
  - response: DataResponse<PharmacyVendorDetailDto>
- GET /api/pharmacy/vendors/{vendorId}/purchase-orders
  - response: PageResponse<PharmacyVendorPurchaseOrderRowDto>

Important rules:
- vendorCode should be unique
- inactive vendors should still be queryable
- do not hard delete vendors if they may be referenced later
- reuse GlobalExceptionHandler for validation and error responses

Suggested Backend Package Structure:
- controller/pharmacy/PharmacyVendorController.java
- dto/pharmacy/PharmacyVendorRowDto.java
- dto/pharmacy/PharmacyVendorDetailDto.java
- dto/pharmacy/PharmacyVendorUpsertRequest.java
- dto/pharmacy/PharmacyVendorPurchaseOrderRowDto.java
- dto/pharmacy/PharmacyVendorStatsDto.java
- model/pharmacy/PharmacyVendor.java
- repository/pharmacy/PharmacyVendorRepository.java
- repository/projection/PharmacyVendorRowProjection.java
- repository/projection/PharmacyVendorPurchaseOrderProjection.java
- repository/projection/PharmacyVendorStatsProjection.java
- service/pharmacy/PharmacyVendorService.java
- service/pharmacy/impl/PharmacyVendorServiceImpl.java
- exception/pharmacy/PharmacyVendorNotFoundException.java
- exception/pharmacy/DuplicateVendorCodeException.java

Suggested Controller Design:
- Create `PharmacyVendorController`
- Keep controller thin
- Use PageResponse<T> for paginated endpoints
- Use DataResponse<T> for detail endpoints

Suggested Repository Query Design:
- use projections for list and stats queries where useful
- vendor list query should return vendor profile and optional PO summary fields
- vendor detail query should return full profile and optional stats
- vendor purchase orders query should return recent rows by vendor

Validation and Exception Handling:
- reuse existing:
  - GlobalExceptionHandler
  - ValidationException
  - ConflictException
  - HospitalAPIException
- do not create a separate pharmacy exception handler unless required

Required Validation Rules:
- for GET /api/pharmacy/vendors:
  - page >= 0
  - size between 1 and 100
  - q minimum length 2 if provided
  - sort must be one of approved fields
- for POST and PUT:
  - vendorCode required
  - vendorName required
  - email valid if provided
  - paymentTermsDays >= 0
  - vendorCode unique
- for GET /api/pharmacy/vendors/{vendorId}:
  - vendorId > 0
  - throw PharmacyVendorNotFoundException if vendor does not exist
- for GET /api/pharmacy/vendors/{vendorId}/purchase-orders:
  - vendorId > 0
  - page >= 0
  - size between 1 and 100
  - vendor must exist first

Required ValidationException Usage:
- use ValidationException for service-layer validation with field-level details

GlobalExceptionHandler Integration:
- expected mapping:
  - invalid request params -> 400
  - invalid body fields -> 400
  - duplicate vendor code -> 409
  - vendor not found -> 404

Suggested Validation Approach:
- controller layer:
  - use bean validation annotations for request DTOs and params
- service layer:
  - validate duplicate code/GST rules
  - validate unsupported sort values
  - validate vendor existence before loading detail or PO history

Business Rules:
- vendorCode must be unique
- inactive vendors may still appear in list and detail pages
- if purchase orders are not implemented yet, vendor stats and PO history may return zero/empty results

Implement:
- entity classes if missing
- repositories
- projection interfaces if helpful
- DTOs
- service
- controller
- not-found and conflict exceptions if missing

Acceptance criteria:
- vendor list endpoint returns paginated rows for the UI
- create and update endpoints enforce validation
- detail endpoint returns vendor profile and stats
- purchase-order history endpoint works or returns safe empty data until PO module is ready
- code follows existing package structure and response wrapper style

Also provide:
- any assumptions if purchase order entities do not yet exist
- brief explanation of query design
- note any schema changes needed
```

## Notes

- This document is backend-only for the `Pharmacy Vendors` module.
- Keep it aligned with `docs/pharmacy-stock-purchase-ui-design.md`.
- If needed later, create equivalent backend prompt docs for:
  - `Purchase Order`
  - `Stock Adjustment`
  - `Pharmacy Transactions`


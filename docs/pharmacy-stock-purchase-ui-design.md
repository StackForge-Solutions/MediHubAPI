# Pharmacy Stock & Purchase UI Design

## Scope

This document defines the UI design and backend API contract for the pharmacy inventory module using these pages:

- Manage Stocks
- Stock Adjustment
- Pharmacy Vendors
- Purchase Order
- Pharmacy Transactions

The design assumes:

- Angular frontend
- Spring Boot backend
- Single pharmacy/store for v1
- `MdmMedicine` remains medicine master data only
- Batch-based inventory is the source of truth

## UI Navigation

Place these pages under the `Pharmacy` menu:

- `Manage Stocks`
- `Purchase Orders`
- `Stock Adjustments`
- `Vendors`
- `Transactions`

Recommended route structure:

```text
/pharmacy/stocks
/pharmacy/stocks/:medicineId
/pharmacy/purchase-orders
/pharmacy/purchase-orders/new
/pharmacy/purchase-orders/:id
/pharmacy/stock-adjustments
/pharmacy/stock-adjustments/new
/pharmacy/stock-adjustments/:id
/pharmacy/vendors
/pharmacy/vendors/:id
/pharmacy/transactions
```

## Angular Page and Component Structure

Recommended feature module structure:

```text
src/app/features/pharmacy-inventory/
  pharmacy-inventory.routes.ts
  services/
    pharmacy-vendor.api.ts
    pharmacy-stock.api.ts
    pharmacy-purchase-order.api.ts
    pharmacy-adjustment.api.ts
    pharmacy-transaction.api.ts
  models/
    pharmacy-vendor.model.ts
    pharmacy-stock.model.ts
    pharmacy-purchase-order.model.ts
    pharmacy-adjustment.model.ts
    pharmacy-transaction.model.ts
  pages/
    manage-stocks/
      manage-stocks.page.ts
      manage-stocks.page.html
      manage-stocks.page.scss
    stock-detail/
      stock-detail.page.ts
      stock-detail.page.html
      stock-detail.page.scss
    stock-adjustment-list/
      stock-adjustment-list.page.ts
      stock-adjustment-list.page.html
      stock-adjustment-list.page.scss
    stock-adjustment-form/
      stock-adjustment-form.page.ts
      stock-adjustment-form.page.html
      stock-adjustment-form.page.scss
    stock-adjustment-detail/
      stock-adjustment-detail.page.ts
      stock-adjustment-detail.page.html
      stock-adjustment-detail.page.scss
    vendor-list/
      vendor-list.page.ts
      vendor-list.page.html
      vendor-list.page.scss
    vendor-detail/
      vendor-detail.page.ts
      vendor-detail.page.html
      vendor-detail.page.scss
    purchase-order-list/
      purchase-order-list.page.ts
      purchase-order-list.page.html
      purchase-order-list.page.scss
    purchase-order-form/
      purchase-order-form.page.ts
      purchase-order-form.page.html
      purchase-order-form.page.scss
    purchase-order-detail/
      purchase-order-detail.page.ts
      purchase-order-detail.page.html
      purchase-order-detail.page.scss
    transaction-list/
      transaction-list.page.ts
      transaction-list.page.html
      transaction-list.page.scss
  components/
    stock-summary-cards/
    stock-filter-bar/
    stock-table/
    stock-batch-table/
    stock-transaction-table/
    vendor-form-drawer/
    vendor-summary-card/
    purchase-order-filter-bar/
    purchase-order-item-grid/
    purchase-order-summary/
    receive-stock-dialog/
    adjustment-form-grid/
    adjustment-preview-card/
    medicine-search-autocomplete/
    vendor-select/
    status-chip/
  state/
    pharmacy-vendor.store.ts
    pharmacy-stock.store.ts
    pharmacy-purchase-order.store.ts
    pharmacy-adjustment.store.ts
    pharmacy-transaction.store.ts
```

Recommended routing:

```ts
export const PHARMACY_INVENTORY_ROUTES: Routes = [
  { path: 'pharmacy/stocks', component: ManageStocksPage },
  { path: 'pharmacy/stocks/:medicineId', component: StockDetailPage },
  { path: 'pharmacy/purchase-orders', component: PurchaseOrderListPage },
  { path: 'pharmacy/purchase-orders/new', component: PurchaseOrderFormPage },
  { path: 'pharmacy/purchase-orders/:id', component: PurchaseOrderDetailPage },
  { path: 'pharmacy/stock-adjustments', component: StockAdjustmentListPage },
  { path: 'pharmacy/stock-adjustments/new', component: StockAdjustmentFormPage },
  { path: 'pharmacy/stock-adjustments/:id', component: StockAdjustmentDetailPage },
  { path: 'pharmacy/vendors', component: VendorListPage },
  { path: 'pharmacy/vendors/:id', component: VendorDetailPage },
  { path: 'pharmacy/transactions', component: TransactionListPage }
];
```

## Shared Frontend Design Rules

- Use server-side pagination for all list pages.
- Use a common filter bar pattern on every list page.
- Use status chips for PO status, stock health, and transaction types.
- Use right-side drawers for quick create/edit on vendor and stock quick views.
- Use full-page forms for purchase orders and stock adjustments.
- Never allow direct stock quantity editing from a table row.
- Any stock change must go through purchase receipt or stock adjustment.

## Shared Backend API Conventions

Use existing response wrappers where possible:

- List pages: `PageResponse<T>`
- Single record pages: `DataResponse<T>`
- Actions: `ApiResponse<T>` or `DataResponse<T>`

Existing wrappers already exist in:

- `src/main/java/com/MediHubAPI/dto/DataResponse.java`
- `src/main/java/com/MediHubAPI/dto/PageResponse.java`
- `src/main/java/com/MediHubAPI/dto/ApiResponse.java`

Base path:

```text
/api/pharmacy
```

## 1. Manage Stocks

### UI Purpose

Show current stock health per medicine and provide drill-down into batch-level inventory and transaction history.

### Angular Structure

- Page: `manage-stocks.page`
- Components:
    - `stock-summary-cards`
    - `stock-filter-bar`
    - `stock-table`
    - `status-chip`

Detail page:

- Page: `stock-detail.page`
- Components:
    - `vendor-summary-card`
    - `stock-batch-table`
    - `stock-transaction-table`

### Main Screen Layout

- Summary cards:
    - Total Medicines
    - Low Stock
    - Out of Stock
    - Expiring Soon
    - Stock Value
- Filter bar
- Main stock table
- Row action menu:
    - View Details
    - Adjust Stock
    - View Transactions

### UI Flow

Entry flow:

1. User opens `Manage Stocks` from the Pharmacy menu.
2. Page loads summary cards first.
3. Page loads stock table with default filters.
4. User scans stock health using low stock and expiry signals.

Filter flow:

1. User searches by medicine name, brand, or code.
2. User applies quick filters such as `Low Stock`, `Out of Stock`, or `Expiring Soon`.
3. Table refreshes without leaving the page.
4. Summary cards refresh using the same filter context.

Row action flow:

1. User opens row action menu.
2. User selects `View Details` to open the medicine stock detail page.
3. User selects `Adjust Stock` to navigate to stock adjustment form with medicine prefilled.
4. User selects `View Transactions` to open the transaction page with medicine filter pre-applied.

Stock detail flow:

1. User enters the stock detail page for a medicine.
2. Header shows medicine profile, current quantity, reorder level, and nearest expiry.
3. Batch table shows available stock by batch sorted by earliest expiry.
4. Recent transactions panel shows latest receipts, sales, and adjustments.
5. User can navigate to `Create Adjustment` or `Transactions` from this page.

### Backend API Contract

#### GET `/api/pharmacy/stocks`

Purpose:

- Stock list page

Query params:

- `q`: string
- `page`: number
- `size`: number
- `sort`: string, example `medicineName,asc`
- `inStockOnly`: boolean
- `lowStockOnly`: boolean
- `expiringInDays`: number
- `vendorId`: number
- `form`: string

Response:

```json
{
  "content": [
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
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### GET `/api/pharmacy/stocks/summary`

Purpose:

- Summary cards

Query params:

- `q`: string optional
- `vendorId`: number optional
- `form`: string optional

Response:

```json
{
  "data": {
    "totalMedicines": 450,
    "lowStockCount": 32,
    "outOfStockCount": 8,
    "expiringSoonCount": 19,
    "stockValue": 275430.75
  }
}
```

#### GET `/api/pharmacy/stocks/{medicineId}`

Purpose:

- Stock detail header

Response:

```json
{
  "data": {
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
}
```

#### GET `/api/pharmacy/stocks/{medicineId}/batches`

Purpose:

- Batch table for stock detail page

Query params:

- `includeExpired`: boolean
- `page`: number
- `size`: number

Response:

```json
{
  "content": [
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
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### GET `/api/pharmacy/stocks/{medicineId}/transactions`

Purpose:

- Recent movement history in stock detail page

Query params:

- `page`: number
- `size`: number

Response:

```json
{
  "content": [
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
      "note": "Initial receipt"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

## 2. Stock Adjustment

### UI Purpose

Allow controlled stock increase or decrease with a full audit trail.

### Angular Structure

- Page: `stock-adjustment-list.page`
- Page: `stock-adjustment-form.page`
- Page: `stock-adjustment-detail.page`
- Components:
    - `adjustment-form-grid`
    - `adjustment-preview-card`
    - `medicine-search-autocomplete`

### Main Screen Layout

List page:

- Filter bar
- Adjustment table
- Create Adjustment button

Form page:

- Adjustment header form
- Multi-line medicine grid
- Preview card
- Submit action

Detail page:

- Header summary
- Line item table
- Related transactions table

### UI Flow

List flow:

1. User opens `Stock Adjustments`.
2. Page shows recent adjustments with date and reason filters.
3. User can search by adjustment number, medicine, or note.
4. User clicks a row to open the adjustment detail page.

Create flow:

1. User clicks `Create Adjustment`.
2. User selects adjustment type: `Increase` or `Decrease`.
3. User selects reason and enters a note.
4. User adds one or more line items using medicine search.
5. User optionally selects a specific batch.
6. System shows current stock, entered quantity, and expected impact.
7. User reviews preview card and submits.
8. On success, user is redirected to adjustment detail page.

Validation flow:

1. If adjustment type is `Decrease`, note is mandatory.
2. If quantity exceeds available batch stock, show inline error.
3. If no batch is selected for a batch-controlled medicine, show batch selection warning.
4. Submit remains disabled until all line items are valid.

Detail flow:

1. User views posted adjustment summary.
2. Page shows before and after effect through linked transactions.
3. Adjustment remains read-only after posting.

### Backend API Contract

#### GET `/api/pharmacy/stock-adjustments`

Purpose:

- Adjustment list page

Query params:

- `q`: string
- `reason`: string
- `type`: string
- `fromDate`: date
- `toDate`: date
- `page`: number
- `size`: number

Response:

```json
{
  "content": [
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
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### POST `/api/pharmacy/stock-adjustments`

Purpose:

- Create and post stock adjustment

Request:

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
    },
    {
      "medicineId": 108,
      "batchId": 9012,
      "qty": 4,
      "unitCost": 12.0,
      "note": "Leaking bottle"
    }
  ]
}
```

Response:

```json
{
  "data": {
    "adjustmentId": 301,
    "adjustmentNo": "ADJ-20260312-001",
    "status": "POSTED"
  }
}
```

#### GET `/api/pharmacy/stock-adjustments/{id}`

Purpose:

- Adjustment detail page

Response:

```json
{
  "data": {
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
}
```

## 3. Pharmacy Vendors

### UI Purpose

Maintain supplier records and view vendor-related purchase activity.

### Angular Structure

- Page: `vendor-list.page`
- Page: `vendor-detail.page`
- Components:
    - `vendor-form-drawer`
    - `vendor-summary-card`

### Main Screen Layout

List page:

- Search and filter bar
- Vendor table
- Create Vendor drawer

Detail page:

- Vendor profile card
- Recent purchase orders
- Recent supplied medicines

### UI Flow

List flow:

1. User opens `Vendors`.
2. Page loads vendor table with active vendors by default.
3. User searches by vendor name, phone, GST, or city.
4. User can create a new vendor from the page without navigation.

Create and edit flow:

1. User clicks `Create Vendor`.
2. Right-side drawer opens with vendor form.
3. User fills contact, tax, license, and address details.
4. User saves vendor and drawer closes on success.
5. Table refreshes and new vendor is highlighted.

Vendor detail flow:

1. User clicks a vendor row.
2. Vendor detail page opens with profile summary.
3. Page shows recent purchase orders and vendor statistics.
4. User can jump directly to `Create Purchase Order` for that vendor.

Operational flow:

1. If vendor is inactive, UI should hide `Create PO` action.
2. If vendor has pending POs, show a badge in the list and detail page.

### Backend API Contract

#### GET `/api/pharmacy/vendors`

Purpose:

- Vendor list page

Query params:

- `q`: string
- `active`: boolean
- `page`: number
- `size`: number

Response:

```json
{
  "content": [
    {
      "vendorId": 4,
      "vendorCode": "VND-004",
      "vendorName": "ABC Pharma",
      "contactPerson": "Raj Kumar",
      "phone": "9876543210",
      "email": "raj@abcpharma.in",
      "gstNo": "29ABCDE1234F1Z7",
      "city": "Bengaluru",
      "active": true,
      "outstandingPurchaseOrders": 3,
      "lastPurchaseDate": "2026-03-10"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### POST `/api/pharmacy/vendors`

Purpose:

- Create vendor

Request:

```json
{
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

Response:

```json
{
  "data": {
    "vendorId": 4,
    "vendorCode": "VND-004"
  }
}
```

#### PUT `/api/pharmacy/vendors/{id}`

Purpose:

- Update vendor

Request:

```json
{
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

Response:

```json
{
  "data": {
    "vendorId": 4,
    "updated": true
  }
}
```

#### GET `/api/pharmacy/vendors/{id}`

Purpose:

- Vendor detail page

Response:

```json
{
  "data": {
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
}
```

#### GET `/api/pharmacy/vendors/{id}/purchase-orders`

Purpose:

- Vendor detail related purchase order tab

Query params:

- `page`: number
- `size`: number

Response:

```json
{
  "content": [
    {
      "purchaseOrderId": 501,
      "poNumber": "PO-20260312-001",
      "orderDate": "2026-03-12",
      "status": "APPROVED",
      "itemCount": 5,
      "netAmount": 10450.0
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

## 4. Purchase Order

### UI Purpose

Manage purchase lifecycle from draft to approval to stock receipt.

### Angular Structure

- Page: `purchase-order-list.page`
- Page: `purchase-order-form.page`
- Page: `purchase-order-detail.page`
- Components:
    - `purchase-order-filter-bar`
    - `purchase-order-item-grid`
    - `purchase-order-summary`
    - `receive-stock-dialog`
    - `vendor-select`
    - `medicine-search-autocomplete`

### Main Screen Layout

List page:

- Status tabs
- Filter bar
- PO table

Form page:

- Header card
- Item grid
- Summary footer
- Save Draft and Approve actions

Detail page:

- Header status section
- Items table
- Receipt history
- Receive stock action

### UI Flow

List flow:

1. User opens `Purchase Orders`.
2. Default tab shows `Draft` and `Approved` records.
3. User filters by vendor, status, and date range.
4. User clicks `New Purchase Order` to start a new document.

Create flow:

1. User selects vendor in the header section.
2. User fills order date, expected delivery date, and optional notes.
3. User adds medicines using autocomplete in the item grid.
4. For each line, user enters ordered quantity, purchase price, MRP, and selling price.
5. Summary section calculates subtotal, tax, discount, and net amount in real time.
6. User saves as draft or approves immediately.
7. On success, user is redirected to the purchase order detail page.

Detail flow:

1. User opens a purchase order detail page.
2. Page shows header information, item lines, and receipt history.
3. If status is `DRAFT`, user can edit and approve.
4. If status is `APPROVED` or `PARTIALLY_RECEIVED`, user can receive stock.
5. If any stock receipt exists, line editing is locked.

Receive stock flow:

1. User clicks `Receive Stock`.
2. Receive dialog opens with pending PO items only.
3. User enters batch number, expiry date, received quantity, purchase price, MRP, and selling price.
4. System validates pending quantity and required batch fields.
5. User submits receipt.
6. On success:
   - PO status updates to `PARTIALLY_RECEIVED` or `RECEIVED`
   - stock summary updates
   - transaction ledger gets new receipt rows
7. User can navigate from PO detail to the corresponding stock detail.

### Backend API Contract

#### GET `/api/pharmacy/purchase-orders`

Purpose:

- Purchase order list page

Query params:

- `q`: string
- `status`: string
- `vendorId`: number
- `fromDate`: date
- `toDate`: date
- `page`: number
- `size`: number

Response:

```json
{
  "content": [
    {
      "purchaseOrderId": 501,
      "poNumber": "PO-20260312-001",
      "vendorId": 4,
      "vendorName": "ABC Pharma",
      "orderDate": "2026-03-12",
      "status": "APPROVED",
      "itemCount": 5,
      "orderedQty": 250,
      "receivedQty": 100,
      "netAmount": 10450.0
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### POST `/api/pharmacy/purchase-orders`

Purpose:

- Create draft purchase order

Request:

```json
{
  "vendorId": 4,
  "orderDate": "2026-03-12",
  "invoiceNumber": null,
  "invoiceDate": null,
  "expectedDeliveryDate": "2026-03-14",
  "note": "Weekly stock order",
  "items": [
    {
      "medicineId": 101,
      "orderedQty": 100,
      "purchasePrice": 1.75,
      "mrp": 3.0,
      "sellingPrice": 2.5,
      "taxPercent": 12.0,
      "discountPercent": 0.0
    }
  ]
}
```

Response:

```json
{
  "data": {
    "purchaseOrderId": 501,
    "poNumber": "PO-20260312-001",
    "status": "DRAFT"
  }
}
```

#### PUT `/api/pharmacy/purchase-orders/{id}`

Purpose:

- Update draft purchase order

Request:

```json
{
  "vendorId": 4,
  "orderDate": "2026-03-12",
  "invoiceNumber": null,
  "invoiceDate": null,
  "expectedDeliveryDate": "2026-03-14",
  "note": "Weekly stock order updated",
  "items": [
    {
      "itemId": 801,
      "medicineId": 101,
      "orderedQty": 120,
      "purchasePrice": 1.75,
      "mrp": 3.0,
      "sellingPrice": 2.5,
      "taxPercent": 12.0,
      "discountPercent": 0.0
    }
  ]
}
```

Response:

```json
{
  "data": {
    "purchaseOrderId": 501,
    "updated": true
  }
}
```

#### POST `/api/pharmacy/purchase-orders/{id}/approve`

Purpose:

- Approve draft purchase order

Request:

```json
{}
```

Response:

```json
{
  "data": {
    "purchaseOrderId": 501,
    "status": "APPROVED"
  }
}
```

#### GET `/api/pharmacy/purchase-orders/{id}`

Purpose:

- Purchase order detail page

Response:

```json
{
  "data": {
    "purchaseOrderId": 501,
    "poNumber": "PO-20260312-001",
    "vendorId": 4,
    "vendorName": "ABC Pharma",
    "orderDate": "2026-03-12",
    "invoiceNumber": null,
    "invoiceDate": null,
    "expectedDeliveryDate": "2026-03-14",
    "status": "APPROVED",
    "subtotal": 175.0,
    "taxAmount": 21.0,
    "discountAmount": 0.0,
    "netAmount": 196.0,
    "note": "Weekly stock order",
    "items": [
      {
        "itemId": 801,
        "medicineId": 101,
        "medicineName": "Paracetamol 500",
        "orderedQty": 100,
        "receivedQty": 0,
        "pendingQty": 100,
        "purchasePrice": 1.75,
        "mrp": 3.0,
        "sellingPrice": 2.5,
        "taxPercent": 12.0,
        "discountPercent": 0.0,
        "lineTotal": 196.0
      }
    ]
  }
}
```

#### POST `/api/pharmacy/purchase-orders/{id}/receive`

Purpose:

- Receive stock and create batches

Request:

```json
{
  "receiptDate": "2026-03-12",
  "invoiceNumber": "INV-889",
  "invoiceDate": "2026-03-12",
  "items": [
    {
      "purchaseOrderItemId": 801,
      "batchNo": "BATCH-APR-01",
      "expiryDate": "2026-08-31",
      "receivedQty": 100,
      "purchasePrice": 1.75,
      "mrp": 3.0,
      "sellingPrice": 2.5
    }
  ]
}
```

Response:

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

## 5. Pharmacy Transactions

### UI Purpose

Provide a full stock movement ledger for audit, review, and reporting.

### Angular Structure

- Page: `transaction-list.page`
- Components:
    - `stock-transaction-table`
    - `stock-filter-bar`
    - `status-chip`

### Main Screen Layout

- Filter bar
- Transaction table
- Export action
- Detail drawer on row click

### UI Flow

List flow:

1. User opens `Transactions`.
2. Page loads latest stock movements in descending time order.
3. User applies filters by medicine, type, vendor, batch, or reference number.
4. Table refreshes and preserves filter state in query params if possible.

Audit flow:

1. User clicks a transaction row.
2. Detail drawer opens with transaction metadata.
3. Drawer shows source document details such as purchase order or adjustment reference.
4. User can navigate to linked PO, stock detail, or adjustment page.

Reporting flow:

1. User applies date range and transaction type filters.
2. User exports the filtered ledger.
3. Export should match currently visible filters exactly.

### Backend API Contract

#### GET `/api/pharmacy/transactions`

Purpose:

- Transaction ledger page

Query params:

- `q`: string
- `medicineId`: number
- `vendorId`: number
- `transactionType`: string
- `batchNo`: string
- `referenceType`: string
- `referenceId`: number
- `fromDate`: date
- `toDate`: date
- `page`: number
- `size`: number

Response:

```json
{
  "content": [
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
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### GET `/api/pharmacy/transactions/{id}`

Purpose:

- Transaction detail drawer

Response:

```json
{
  "data": {
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
}
```

## Shared Supporting APIs

These APIs support multiple pages and reusable components.

### GET `/api/pharmacy/medicines/search`

Purpose:

- Medicine autocomplete in PO and adjustment forms

Query params:

- `mode`
- `form`
- `q`
- `limit`
- `inStockOnly`

This aligns with the existing medicine search controller.

### GET `/api/pharmacy/vendors/options`

Purpose:

- Vendor dropdown component

Query params:

- `q`
- `active`

Response:

```json
{
  "data": [
    {
      "id": 4,
      "label": "ABC Pharma",
      "code": "VND-004"
    }
  ]
}
```

### GET `/api/pharmacy/enums`

Purpose:

- Load dropdown metadata in one request

Response:

```json
{
  "data": {
    "purchaseOrderStatuses": [
      "DRAFT",
      "APPROVED",
      "PARTIALLY_RECEIVED",
      "RECEIVED",
      "CANCELLED"
    ],
    "transactionTypes": [
      "PURCHASE_RECEIPT",
      "SALE",
      "ADJUSTMENT_IN",
      "ADJUSTMENT_OUT",
      "RETURN_TO_VENDOR",
      "RETURN_FROM_PATIENT",
      "EXPIRED",
      "DAMAGED"
    ],
    "adjustmentReasons": [
      "COUNT_CORRECTION",
      "DAMAGED",
      "EXPIRED",
      "LOST",
      "MANUAL_OPENING"
    ]
  }
}
```

## Validation Rules

- Do not allow stock receipt on a cancelled PO.
- Do not allow PO edits after any receipt has been posted.
- Do not allow negative `receivedQty`, `availableQty`, or adjustment quantity.
- Require `batchNo` and `expiryDate` on stock receipt.
- Require `reason` and `note` for stock decreases.
- Do not allow dispensing or adjustment out from expired batches unless using a dedicated expired flow.

## Suggested Backend Build Order

1. Vendor CRUD APIs
2. Purchase order CRUD and approve APIs
3. Purchase order receive API with batch creation
4. Stock list and stock detail APIs
5. Stock adjustment APIs
6. Transactions list and detail APIs
7. Shared dropdown and enum APIs

## Suggested Frontend Build Order

1. Vendor list and vendor create drawer
2. Purchase order list and create page
3. Purchase order detail and receive stock dialog
4. Manage stocks list and stock detail page
5. Stock adjustment list and create page
6. Transactions page

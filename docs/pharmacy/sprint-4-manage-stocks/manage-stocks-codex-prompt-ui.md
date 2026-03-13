# Manage Stocks Codex Prompt

## Purpose

Use this prompt when asking Codex to design or implement the frontend for `Sprint 4: Manage Stocks` in Angular.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-manage-stocks-ui
```

Current checked-out branch:

```text
feature/auth-jwt-identity-role-claims
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-manage-stocks-ui
```

Command to create it explicitly from the current branch name:

```bash
git checkout -b feature/pharmacy-manage-stocks-ui feature/auth-jwt-identity-role-claims
```

## Codex Prompt

```text
Implement the `Manage Stocks` frontend for `Sprint 4` in the pharmacy inventory module using Angular.

Project context:
- This is a pharmacy stock and purchase workflow.
- `Manage Stocks` is Sprint 4 core inventory visibility.
- The backend is Spring Boot.
- The UI should fit a hospital/pharmacy admin workflow, not an e-commerce style layout.
- Keep the design clean, dense, and operationally useful.
- Prioritize table-driven workflow, fast filtering, clear stock health signals, and drill-down actions.

Routes:
- /pharmacy/stocks
- /pharmacy/stocks/:medicineId

Main goals:
- show current medicine stock summary in one page
- allow quick filtering for low stock, out of stock, and expiring stock
- provide row actions to open stock detail, stock adjustment, and transactions
- show top summary cards for operational visibility

Required page sections:

1. Header area
- Page title: Manage Stocks
- Subtitle: Track medicine availability, expiry, and stock health
- Primary action button: Export

2. Summary cards row
- Total Medicines
- Low Stock
- Out of Stock
- Expiring Soon
- Stock Value

3. Filter bar
- Search by medicine name, brand, or code
- Form filter
- In stock only toggle
- Low stock only toggle
- Expiring in days filter
- Vendor filter
- Reset filters button

4. Main stock table
Columns:
- Medicine
- Brand
- Form
- Available Qty
- Reserved Qty
- Reorder Level
- Selling Price
- MRP
- Nearest Expiry
- Stock Value
- Status
- Actions

5. Row actions
- View Details
- Adjust Stock
- View Transactions

6. Empty states
- No medicines found
- No stock matches selected filters

7. Loading and error states
- show skeleton or loading placeholders for cards and table
- show retry option on API error

8. Shared components and app conventions
- use BannerComponent
- use LoaderBackdropComponent
- use authorization logic(superadmin have all access)
- use HeaderComponent

Design requirements:
- use a compact admin dashboard style
- make low stock, out of stock, and expiring rows visually distinct
- use status chips:
  - HEALTHY
  - LOW_STOCK
  - OUT_OF_STOCK
  - EXPIRING_SOON
- keep filters sticky if possible
- table should support server-side pagination and sorting
- design should work well on desktop first, and remain usable on tablet widths

Navigation behavior:
- clicking a row or `View Details` goes to /pharmacy/stocks/:medicineId
- clicking `Adjust Stock` navigates to /pharmacy/stock-adjustments/new with medicineId prefilled
- clicking `View Transactions` navigates to /pharmacy/transactions with medicineId filter pre-applied

Use these backend APIs:

GET /api/pharmacy/stocks
Query params:
- q
- page
- size
- sort
- inStockOnly
- lowStockOnly
- expiringInDays
- vendorId
- form

Response shape:
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

GET /api/pharmacy/stocks/summary
Response shape:
{
  "data": {
    "totalMedicines": 450,
    "lowStockCount": 32,
    "outOfStockCount": 8,
    "expiringSoonCount": 19,
    "stockValue": 275430.75
  }
}

GET /api/pharmacy/stocks/:medicineId
Response shape:
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

GET /api/pharmacy/stocks/:medicineId/batches
Response shape:
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
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}

GET /api/pharmacy/stocks/:medicineId/transactions
Response shape:
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
      "referenceNo": "PO-20260312-001",
      "note": "Initial receipt"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}

Angular implementation requirements:
- create page component: manage-stocks.page
- create stock detail page for /pharmacy/stocks/:medicineId
- create reusable components if needed:
  - stock-summary-cards
  - stock-filter-bar
  - stock-table
  - stock-detail-header
  - stock-batch-table
  - stock-transaction-table
  - status-chip
- create API service for stock listing, summary, detail, batches, and transaction calls
- keep models typed
- use route query params to preserve filter state
- keep filter form reactive
- support debounced search input

Expected files:
- manage-stocks.page.ts
- manage-stocks.page.html
- manage-stocks.page.scss
- stock-detail.page.ts
- stock-detail.page.html
- stock-detail.page.scss
- pharmacy-stock.api.ts
- pharmacy-stock.model.ts
- any small reusable components needed for this page

Implementation notes:
- do not hardcode data
- do not use client-side filtering for server-driven data
- format currency consistently
- format expiry dates clearly
- highlight dangerous states without making the page noisy
- keep the page scalable for large medicine lists
- keep stock detail read-only for this sprint; quantity changes should happen through receipt or adjustment flows

Acceptance criteria:
- summary cards load from API
- stock table loads from API with server-side filters
- search and filters update query params and refresh data
- row actions navigate correctly
- stock detail page loads summary, batches, and recent transactions
- loading, empty, and error states are handled
- layout is production-ready and consistent with an admin panel

Also provide:
- Angular interfaces for request and response models
- a short explanation of component responsibilities
- any assumptions if backend fields differ
```

## Notes

- This document is frontend-only for `Sprint 4: Manage Stocks`.
- Keep it aligned with `docs/pharmacy-development-sequence.md` and `docs/pharmacy-stock-purchase-ui-design.md`.
- This scope depends on Sprint 3 stock receipt and batch creation being available first.

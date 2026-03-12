# Manage Stocks Codex Prompt

## Purpose

Use this prompt when asking Codex to design or implement the `Manage Stocks` page for the pharmacy inventory module.

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
Implement the `Manage Stocks` page for the pharmacy inventory module in Angular.

Project context:
- This is a pharmacy stock and purchase workflow.
- `Manage Stocks` is the main operational inventory page.
- The backend is Spring Boot.
- The UI should fit a hospital/pharmacy admin workflow, not an e-commerce style layout.
- Keep the design clean, dense, and operationally useful.
- Prioritize table-driven workflow, fast filtering, clear stock health signals, and drill-down actions.

Route:
- /pharmacy/stocks

Secondary route:
- /pharmacy/stocks/:medicineId

Main goals:
- Show current medicine stock summary in one page.
- Allow quick filtering for low stock, out of stock, and expiring stock.
- Provide row actions to open stock detail, stock adjustment, and transactions.
- Show top summary cards for operational visibility.

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
- Show skeleton or loading placeholders for cards and table
- Show retry option on API error

8. Use BannerComponent,LoaderBackdropComponent,authorization logic(superadmin have all access),HeaderComponent

Design requirements:
- Use a compact admin dashboard style.
- Make low stock, out of stock, and expiring rows visually distinct.
- Use status chips:
  - HEALTHY
  - LOW_STOCK
  - OUT_OF_STOCK
  - EXPIRING_SOON
- Keep filters sticky if possible.
- Table should support server-side pagination and sorting.
- Design should work well on desktop first, and remain usable on tablet widths.

Navigation behavior:
- Clicking a row or `View Details` goes to /pharmacy/stocks/:medicineId
- Clicking `Adjust Stock` navigates to /pharmacy/stock-adjustments/new with medicineId prefilled
- Clicking `View Transactions` navigates to /pharmacy/transactions with medicineId filter pre-applied

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

Angular implementation requirements:
- Create page component: manage-stocks.page
- Create reusable components if needed:
  - stock-summary-cards
  - stock-filter-bar
  - stock-table
  - status-chip
- Create API service for stock listing and summary calls
- Keep models typed
- Use route query params to preserve filter state
- Keep filter form reactive
- Support debounced search input

Expected files:
- manage-stocks.page.ts
- manage-stocks.page.html
- manage-stocks.page.scss
- pharmacy-stock.api.ts
- pharmacy-stock.model.ts
- any small reusable components needed for this page()

Implementation notes:
- Do not hardcode data.
- Do not use client-side filtering for server-driven data.
- Format currency consistently.
- Format expiry dates clearly.
- Highlight dangerous states without making the page noisy.
- Keep the page scalable for large medicine lists.

Acceptance criteria:
- Summary cards load from API
- Stock table loads from API with server-side filters
- Search and filters update query params and refresh data
- Row actions navigate correctly
- Loading, empty, and error states are handled
- Layout is production-ready and consistent with an admin panel

Also provide:
- Angular interfaces for request/response models
- a short explanation of component responsibilities
- any assumptions if backend fields differ
```

## Notes

- This prompt is intentionally scoped only to the `Manage Stocks` page.
- Use the main design reference in `docs/pharmacy-stock-purchase-ui-design.md` for consistency with the rest of the
  module.
- If needed, create similar prompt files later for:
    - `Stock Adjustment`
    - `Pharmacy Vendors`
    - `Purchase Order`
    - `Pharmacy Transactions`

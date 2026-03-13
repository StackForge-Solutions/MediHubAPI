# Stock Adjustment Codex Prompt

## Purpose

Use this prompt when asking Codex to design or implement the frontend for `Sprint 5: Stock Adjustment` in Angular.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-stock-adjustment-ui
```

Current checked-out branch:

```text
feature/auth-jwt-identity-role-claims
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-stock-adjustment-ui
```

Command to create it explicitly from the current branch name:

```bash
git checkout -b feature/pharmacy-stock-adjustment-ui feature/auth-jwt-identity-role-claims
```

## Codex Prompt

```text
Implement the `Stock Adjustment` frontend for `Sprint 5` in the pharmacy inventory module using Angular.

Project context:
- This is a pharmacy stock and purchase workflow.
- `Stock Adjustment` is Sprint 5 and must work on top of the existing stock, batches, and stock summary flows.
- The backend is Spring Boot.
- The UI should fit a hospital/pharmacy admin workflow, not an e-commerce style layout.
- Keep the design clean, dense, operationally useful, and audit-friendly.
- Prioritize controlled stock correction, strong validation, and clear before/after visibility.

Routes:
- /pharmacy/stock-adjustments
- /pharmacy/stock-adjustments/new
- /pharmacy/stock-adjustments/:id

Main goals:
- show recent stock adjustments in a searchable list
- allow users to create posted stock adjustments with one or more line items
- support both `INCREASE` and `DECREASE` adjustment flows
- show current stock, selected batch, quantity impact, and preview before submit
- provide a read-only detail page with full adjustment context after posting

Required page sections:

1. List page header
- Page title: Stock Adjustments
- Subtitle: Record controlled inventory corrections with full audit trail
- Primary action button: Create Adjustment

2. List page filters
- Search by adjustment number, medicine, or note
- Adjustment type filter
- Reason filter
- From date filter
- To date filter
- Reset filters button

3. List page table
Columns:
- Adjustment No
- Date
- Type
- Reason
- Medicine Count
- Total Qty Impact
- Created By
- Status
- Actions

4. Create adjustment form
- Adjustment date
- Adjustment type: `INCREASE` or `DECREASE`
- Reason
- Note
- Multi-line item grid
- Add row action
- Remove row action

5. Line item grid behavior
Columns:
- Medicine search
- Batch selection
- Current stock
- Current batch stock
- Qty
- Unit Cost
- Note
- Expected impact

6. Preview and submission area
- Summary card with adjustment type, line count, total quantity impact, and estimated value impact
- Validation summary for blocking errors
- Submit button
- Cancel/back action

7. Detail page
- Header summary card
- Adjustment metadata
- Read-only item table
- Linked transaction or audit section if transaction rows are available

8. Empty, loading, and error states
- loading skeleton or placeholders for list and detail views
- clear empty state when no adjustments match filters
- retry option on API errors

9. Shared components and app conventions
- use BannerComponent
- use LoaderBackdropComponent
- use authorization logic(superadmin have all access)
- use HeaderComponent

Design requirements:
- use a compact admin workflow style
- make `INCREASE` and `DECREASE` visually distinct but not noisy
- use status chips for:
  - POSTED
  - INCREASE
  - DECREASE
  - adjustment reasons where helpful
- keep list filters sticky if possible
- keep create form desktop-first, but usable on tablet widths
- show inline row-level validation instead of only global errors
- prevent accidental posting by keeping the review area clear and explicit

Navigation behavior:
- clicking a list row or `View Details` goes to /pharmacy/stock-adjustments/:id
- clicking `Create Adjustment` goes to /pharmacy/stock-adjustments/new
- when opened from manage stocks, support a prefilled `medicineId` query param on /pharmacy/stock-adjustments/new
- after successful submit, navigate to /pharmacy/stock-adjustments/:id

Use these backend APIs:

GET /api/pharmacy/stock-adjustments
Query params:
- q
- reason
- type
- fromDate
- toDate
- page
- size

Response shape:
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

POST /api/pharmacy/stock-adjustments
Request shape:
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

Response shape:
{
  "data": {
    "adjustmentId": 301,
    "adjustmentNo": "ADJ-20260312-001",
    "status": "POSTED"
  }
}

GET /api/pharmacy/stock-adjustments/:id
Response shape:
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

GET /api/pharmacy/medicines/search
Query params:
- mode
- form
- q
- limit
- inStockOnly

Use for:
- medicine autocomplete in the adjustment form

GET /api/pharmacy/stocks/:medicineId/batches
Query params:
- includeExpired
- page
- size

Use for:
- batch dropdown and current batch stock visibility

GET /api/pharmacy/enums
Response shape:
{
  "data": {
    "adjustmentReasons": [
      "COUNT_CORRECTION",
      "DAMAGED",
      "EXPIRED",
      "LOST",
      "MANUAL_OPENING"
    ]
  }
}

Angular implementation requirements:
- create page component: stock-adjustment-list.page
- create page component: stock-adjustment-form.page
- create page component: stock-adjustment-detail.page
- create reusable components if needed:
  - adjustment-form-grid
  - adjustment-preview-card
  - medicine-search-autocomplete
  - status-chip
- create API service for adjustment list, create, and detail calls
- keep models typed
- use route query params to preserve list filter state
- keep filter form and create form reactive
- support debounced search input on list and medicine autocomplete
- support query-param prefill for medicineId when navigated from manage stocks

Expected files:
- stock-adjustment-list.page.ts
- stock-adjustment-list.page.html
- stock-adjustment-list.page.scss
- stock-adjustment-form.page.ts
- stock-adjustment-form.page.html
- stock-adjustment-form.page.scss
- stock-adjustment-detail.page.ts
- stock-adjustment-detail.page.html
- stock-adjustment-detail.page.scss
- pharmacy-adjustment.api.ts
- pharmacy-adjustment.model.ts
- any small reusable components needed for this flow

Implementation notes:
- do not hardcode data
- do not use client-side filtering for server-driven list data
- disable submit until all rows are valid
- if adjustment type is `DECREASE`, require a header note
- if quantity exceeds available batch stock, show inline error
- if a batch-controlled medicine has no batch selected, show a blocking warning
- do not allow negative or zero quantities
- show currency formatting consistently for unit cost and line value
- keep the detail page fully read-only after posting

Acceptance criteria:
- adjustment list loads from API with server-side filters and pagination
- create page can add multiple line items and validate each row
- medicine search and batch selection work from backend data
- submit posts a valid adjustment and redirects to detail page
- detail page loads posted adjustment data correctly
- loading, empty, and error states are handled
- layout is production-ready and consistent with the pharmacy admin module

Also provide:
- Angular interfaces for request and response models
- a short explanation of component responsibilities
- any assumptions if backend fields differ
```

## Notes

- This document is frontend-only for `Sprint 5: Stock Adjustment`.
- Keep it aligned with `docs/pharmacy-development-sequence.md` and `docs/pharmacy-stock-purchase-ui-design.md`.
- This scope depends on prior stock receipt, batch creation, and manage stocks inventory visibility being available first.

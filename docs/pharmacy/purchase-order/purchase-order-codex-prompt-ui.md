# Purchase Order Codex Prompt

## Purpose

Use this prompt when asking Codex to design or implement the `Purchase Order` module for the pharmacy inventory system in Angular.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-purchase-order-ui
```

Current checked-out branch:

```text
feature/auth-jwt-identity-role-claims
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-purchase-order-ui
```

Command to create it explicitly from the current branch name:

```bash
git checkout -b feature/pharmacy-purchase-order-ui feature/auth-jwt-identity-role-claims
```

## Codex Prompt

```text
Implement the `Purchase Order` module for the pharmacy inventory system in Angular.

Project context:
- This is a pharmacy stock and purchase workflow.
- `Purchase Order` is Sprint 2 core functionality.
- The backend is Spring Boot.
- The UI should fit a hospital/pharmacy admin workflow, not an e-commerce style layout.
- Keep the design clean, dense, and operationally useful.
- Prioritize fast PO creation, editable item grid workflow, clear status transitions, and strong draft/approval UX.

Routes:
- /pharmacy/purchase-orders
- /pharmacy/purchase-orders/new
- /pharmacy/purchase-orders/:id

Main goals:
- Show a purchase-order list with status filters.
- Allow draft creation and editing.
- Allow PO detail viewing.
- Allow PO approval and cancellation.
- Prepare the PO for stock receipt flow.

Required page sections:

1. Purchase Order List Header
- Title: Purchase Orders
- Subtitle: Create and manage pharmacy procurement orders
- Primary action: New Purchase Order

2. Purchase Order List Filter Bar
- Search by PO number, vendor, invoice number
- Status filter
- Vendor filter
- Date range filter
- Reset filters button

3. Purchase Order List Table
Columns:
- PO Number
- Vendor
- Order Date
- Status
- Items
- Ordered Qty
- Received Qty
- Net Amount
- Actions

4. Purchase Order Form Page
Header section:
- Vendor select
- Order date
- Expected delivery date
- Invoice number optional
- Invoice date optional
- Note

5. Purchase Order Item Grid
Columns:
- Medicine
- Ordered Qty
- Purchase Price
- MRP
- Selling Price
- Tax Percent
- Discount Percent
- Line Total
- Remove

6. Summary Footer
- Subtotal
- Tax Amount
- Discount Amount
- Net Amount

7. Purchase Order Detail Page
Sections:
- PO summary card
- Status and action bar
- Item list
- Receipt history summary if available
- Quick action: Receive Stock

8. Loading, empty, and error states
- Skeleton states for list and detail
- Empty list state
- Inline form validation
- Retry state for failed API calls

9. Use BannerComponent, LoaderBackdropComponent, authorization logic (superadmin have all access), HeaderComponent

Design requirements:
- Use a compact admin dashboard style.
- Keep the item grid central to the page.
- Separate draft editing from immutable approved state.
- Make status chips very clear:
  - DRAFT
  - APPROVED
  - PARTIALLY_RECEIVED
  - RECEIVED
  - CANCELLED
- Do not allow accidental destructive actions.
- Desktop first, but remain usable on tablet widths.

Navigation behavior:
- Clicking a list row opens /pharmacy/purchase-orders/:id
- New Purchase Order opens /pharmacy/purchase-orders/new
- Save draft returns to detail page
- Approve updates status in place and locks editable fields
- Receive Stock navigates to the stock receipt flow

Use these backend APIs:

GET /api/pharmacy/purchase-orders
Query params:
- q
- status
- vendorId
- fromDate
- toDate
- page
- size
- sort

POST /api/pharmacy/purchase-orders
PUT /api/pharmacy/purchase-orders/{id}
GET /api/pharmacy/purchase-orders/{id}
POST /api/pharmacy/purchase-orders/{id}/approve
POST /api/pharmacy/purchase-orders/{id}/cancel

Angular implementation requirements:
- Create page components:
  - purchase-order-list.page
  - purchase-order-form.page
  - purchase-order-detail.page
- Keep models typed
- Use reactive forms
- Use FormArray for PO items
- Support debounced search on list page
- Preserve list filters in query params

Suggested components:
- purchase-order-filter-bar
- purchase-order-table
- purchase-order-summary-card
- purchase-order-item-grid
- purchase-order-status-chip
- purchase-order-totals-footer

Expected files:
- purchase-order-list.page.ts
- purchase-order-list.page.html
- purchase-order-list.page.scss
- purchase-order-form.page.ts
- purchase-order-form.page.html
- purchase-order-form.page.scss
- purchase-order-detail.page.ts
- purchase-order-detail.page.html
- purchase-order-detail.page.scss
- pharmacy-purchase-order.api.ts
- pharmacy-purchase-order.model.ts
- reusable PO components as needed

Implementation notes:
- Do not hardcode data.
- Do not use client-side filtering for server-driven lists.
- Lock editing once status is no longer DRAFT unless the backend explicitly allows it.
- Keep totals calculation consistent with backend semantics.

Acceptance criteria:
- PO list loads from API with filters and pagination
- User can create draft PO
- User can update draft PO
- User can view PO detail
- User can approve or cancel PO
- Approved PO transitions cleanly toward stock receipt
- Loading, empty, and error states are handled
- Layout is production-ready and consistent with an admin panel

Also provide:
- Angular interfaces for request/response models
- a short explanation of component responsibilities
- any assumptions if backend fields differ
```

## Notes

- This prompt is intentionally scoped to `Sprint Purchase Order`.
- Use `docs/pharmacy-stock-purchase-ui-design.md` for broader consistency.


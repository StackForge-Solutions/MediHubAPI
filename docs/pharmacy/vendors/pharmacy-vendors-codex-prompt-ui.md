# Pharmacy Vendors Codex Prompt

## Purpose

Use this prompt when asking Codex to design or implement the `Pharmacy Vendors` pages for the pharmacy inventory module.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-vendors-ui
```

Current checked-out branch:

```text
feature/auth-jwt-identity-role-claims
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-vendors-ui
```

Command to create it explicitly from the current branch name:

```bash
git checkout -b feature/pharmacy-vendors-ui feature/auth-jwt-identity-role-claims
```

## Codex Prompt

```text
Implement the `Pharmacy Vendors` pages for the pharmacy inventory module in Angular.

Project context:
- This is a pharmacy stock and purchase workflow.
- `Pharmacy Vendors` is the vendor master-data module.
- The backend is Spring Boot.
- The UI should fit a hospital/pharmacy admin workflow, not an e-commerce style layout.
- Keep the design clean, dense, and operationally useful.
- Prioritize fast search, quick create/edit flow, and vendor-to-purchase-order navigation.

Routes:
- /pharmacy/vendors
- /pharmacy/vendors/:id

Main goals:
- Show vendor list with search and filters.
- Allow create and edit from a fast drawer flow.
- Provide a vendor detail page with vendor profile and recent purchase context.
- Allow direct navigation to create purchase order for a vendor.

Required page sections:

1. Vendor List Page Header
- Page title: Pharmacy Vendors
- Subtitle: Manage supplier master data for pharmacy purchasing
- Primary action button: Create Vendor

2. Vendor Filter Bar
- Search by vendor name, code, phone, GST
- Active/inactive filter
- City filter optional
- Reset filters button

3. Vendor Table
Columns:
- Vendor Name
- Vendor Code
- Contact Person
- Phone
- Email
- GST No
- City
- Payment Terms
- Active
- Outstanding POs
- Last Purchase Date
- Actions

4. Create/Edit Vendor Drawer
Fields:
- Vendor Name
- Vendor Code
- Contact Person
- Phone
- Email
- GST No
- Drug License No
- Address Line 1
- Address Line 2
- City
- State
- Pincode
- Payment Terms Days
- Active toggle

5. Vendor Detail Page
Sections:
- Vendor summary card
- Contact and tax details
- Address section
- Stats cards:
  - Total Purchase Orders
  - Pending Purchase Orders
  - Total Purchase Value
  - Last Purchase Date
- Recent Purchase Orders table
- Quick action: Create Purchase Order

6. Empty states
- No vendors found
- No recent purchase orders

7. Loading and error states
- Show skeleton or loading placeholders for table and summary
- Show retry option on API error

8. Use BannerComponent, LoaderBackdropComponent, authorization logic (superadmin have all access), HeaderComponent

Design requirements:
- Use a compact admin dashboard style.
- Make active/inactive state obvious through chips or toggles.
- Keep list page optimized for fast admin operations.
- Use a drawer for create/edit so users stay in context.
- Use a full page for vendor detail.
- Design should work well on desktop first, and remain usable on tablet widths.

Navigation behavior:
- Clicking a vendor row goes to /pharmacy/vendors/:id
- Clicking `Edit` opens the edit drawer
- Clicking `Create Purchase Order` navigates to /pharmacy/purchase-orders/new with vendorId prefilled

Use these backend APIs:

GET /api/pharmacy/vendors
Query params:
- q
- active
- page
- size
- sort

Response shape:
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
      "paymentTermsDays": 30,
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

POST /api/pharmacy/vendors
PUT /api/pharmacy/vendors/{id}
GET /api/pharmacy/vendors/{id}
GET /api/pharmacy/vendors/{id}/purchase-orders

Angular implementation requirements:
- Create page components:
  - vendor-list.page
  - vendor-detail.page
- Create reusable components if needed:
  - vendor-filter-bar
  - vendor-table
  - vendor-form-drawer
  - vendor-summary-card
  - vendor-stats-cards
- Create API service for vendor list, detail, create, and update calls
- Keep models typed
- Use route query params to preserve list filter state
- Keep vendor form reactive
- Support debounced search input

Expected files:
- vendor-list.page.ts
- vendor-list.page.html
- vendor-list.page.scss
- vendor-detail.page.ts
- vendor-detail.page.html
- vendor-detail.page.scss
- pharmacy-vendor.api.ts
- pharmacy-vendor.model.ts
- any reusable components needed for this module

Implementation notes:
- Do not hardcode data.
- Do not use client-side filtering for server-driven data.
- Keep form validation clear and inline.
- Highlight inactive vendors without making the page noisy.
- Make `Create Purchase Order` action easy to access from vendor detail.

Acceptance criteria:
- Vendor list loads from API with server-side filters
- Create and edit drawer work with backend validation
- Vendor detail page loads vendor profile and stats
- Recent purchase orders are shown on vendor detail
- Navigation to purchase order creation works correctly
- Loading, empty, and error states are handled
- Layout is production-ready and consistent with an admin panel

Also provide:
- Angular interfaces for request/response models
- a short explanation of component responsibilities
- any assumptions if backend fields differ
```

## Notes

- This prompt is intentionally scoped only to the `Pharmacy Vendors` module.
- Use the main design reference in `docs/pharmacy-stock-purchase-ui-design.md` for consistency with the rest of the module.
- If needed later, create similar prompt files for:
  - `Purchase Order`
  - `Stock Adjustment`
  - `Pharmacy Transactions`


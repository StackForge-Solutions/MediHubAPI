# Stock Receiving and Inventory Foundation Codex Prompt

## Purpose

Use this prompt when asking Codex to design or implement the frontend for `Sprint 3: Stock Receiving and Inventory Foundation` in Angular.

## Feature Branch

Recommended feature branch name:

```text
feature/pharmacy-stock-receiving-ui
```

Current checked-out branch:

```text
feature/auth-jwt-identity-role-claims
```

Command to create a new branch from the currently checked-out branch:

```bash
git checkout -b feature/pharmacy-stock-receiving-ui
```

Command to create it explicitly from the current branch name:

```bash
git checkout -b feature/pharmacy-stock-receiving-ui feature/auth-jwt-identity-role-claims
```

## Codex Prompt

```text
Implement the frontend for `Sprint 3: Stock Receiving and Inventory Foundation` in the pharmacy inventory module using Angular.

Project context:
- This is a pharmacy stock and purchase workflow.
- Sprint 3 starts the real inventory lifecycle by posting stock receipt against approved purchase orders.
- The backend is Spring Boot.
- The UI should fit a hospital/pharmacy admin workflow, not an e-commerce style layout.
- Keep the design clean, dense, and operationally useful.
- Prioritize fast receipt entry, clear pending-vs-received quantities, and visible audit history.

Primary route context:
- /pharmacy/purchase-orders/:id

Main goals:
- let the user receive stock from a purchase-order detail page
- show pending PO items that are eligible for receipt
- collect batch and pricing details per received line
- show receipt history after posting
- keep PO detail state in sync after receipt submission

Required page sections:

1. Purchase Order Detail Header
- show PO number, vendor, order date, invoice details, status, and totals
- show status chip for:
  - DRAFT
  - APPROVED
  - PARTIALLY_RECEIVED
  - RECEIVED
  - CANCELLED
- primary action: Receive Stock when status allows it

2. Items Table
Columns:
- Medicine
- Ordered Qty
- Received Qty
- Pending Qty
- Purchase Price
- MRP
- Selling Price

3. Receive Stock Dialog or Inline Receipt Panel
- receipt date
- invoice number
- invoice date
- note
- receipt line grid for pending items only

4. Receipt Line Entry Grid
Columns:
- Medicine
- Pending Qty
- Batch No
- Expiry Date
- Receive Qty
- Purchase Price
- MRP
- Selling Price
- Line Note

5. Receipt History Section
Columns:
- Medicine
- Batch No
- Expiry Date
- Received Qty
- Purchase Price
- MRP
- Selling Price
- Received At

6. States
- loading state for PO detail and receipt history
- empty state when no receipts exist yet
- validation state for invalid receipt entries
- error banner with retry for API failures

7. Shared components and app conventions
- use BannerComponent
- use LoaderBackdropComponent
- use authorization logic(superadmin have all access)
- use HeaderComponent

Design requirements:
- keep the purchase-order detail page operational and dense
- make pending quantity very clear
- highlight when a PO is partially received
- lock editing of PO line items after any receipt exists
- show receipt history as audit information, not as editable rows
- keep the receive workflow fast for pharmacy staff working through multiple deliveries
- desktop first, but usable on tablet widths

Interaction behavior:
- Receive Stock action is visible only for `APPROVED` and `PARTIALLY_RECEIVED`
- receiving should only allow pending items
- form should prevent submitting empty or zero-qty receipt lines
- after successful receipt:
  - close dialog or reset inline panel
  - refresh PO detail
  - refresh receipt history
  - update visible status chip and received quantities
- if PO becomes `RECEIVED`, hide the receive action
- allow navigation from receipt history or item context to manage-stock detail later if route already exists

Use these backend APIs:

GET /api/pharmacy/purchase-orders/{id}
Response shape:
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

POST /api/pharmacy/purchase-orders/{id}/receive
Request shape:
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

Response shape:
{
  "data": {
    "purchaseOrderId": 501,
    "status": "RECEIVED",
    "receivedItemCount": 1,
    "receivedQty": 100
  }
}

GET /api/pharmacy/purchase-orders/{id}/receipts
Response shape:
{
  "content": [
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
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}

Angular implementation requirements:
- extend the purchase-order detail flow instead of creating a disconnected page
- create a dedicated receipt component if useful:
  - receive-stock-dialog
  - receive-stock-form
  - receipt-history-table
- create typed request and response models
- keep the receipt form reactive
- prefill receipt lines from pending PO items
- keep dates and currency formatted consistently
- show server validation errors at field level where possible
- refresh data from the server after successful receipt instead of mutating local state blindly

Expected files:
- purchase-order-detail.page.ts
- purchase-order-detail.page.html
- purchase-order-detail.page.scss
- receive-stock-dialog.component.ts
- receive-stock-dialog.component.html
- receive-stock-dialog.component.scss
- receipt-history-table.component.ts
- receipt-history-table.component.html
- receipt-history-table.component.scss
- pharmacy-purchase-order.api.ts
- pharmacy-purchase-order.model.ts

Implementation notes:
- do not hardcode data
- do not let users receive stock for cancelled or draft POs
- do not allow receipt lines for items with zero pending quantity
- make partial receipt obvious in the UI
- keep history rows read-only
- support pagination for receipt history if the shared table pattern already expects it
- if the app already uses drawers instead of dialogs for detail actions, follow that pattern

Acceptance criteria:
- PO detail page shows receipt history
- Receive Stock action opens a usable receipt workflow
- only pending items can be received
- receipt submission calls backend with typed payload
- page refreshes PO header, item quantities, and receipt history after success
- loading, empty, validation, and error states are handled
- layout is production-ready and consistent with the rest of the pharmacy admin module

Also provide:
- Angular interfaces for PO detail, receipt request, receipt response, and receipt history models
- a short explanation of component responsibilities
- any assumptions if backend fields differ
```

## Notes

- This document is frontend-only for `Sprint 3: Stock Receiving and Inventory Foundation`.
- Keep it aligned with `docs/pharmacy-development-sequence.md` and `docs/pharmacy-stock-purchase-ui-design.md`.
- This scope should prepare the project for the later `Manage Stocks` pages to consume real inventory data.

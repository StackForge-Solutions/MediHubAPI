# Pharmacy Development Sequence

## Recommended Page Development Order

Develop these modules in this sequence:

1. `Pharmacy Vendors`
2. `Purchase Order`
3. `Manage Stocks`
4. `Stock Adjustment`
5. `Pharmacy Transactions`

## Why This Order

- `Pharmacy Vendors` is master data and has no deeper dependency.
- `Purchase Order` depends on vendors and creates the stock intake workflow.
- `Manage Stocks` becomes useful only after stock receipt creates actual inventory.
- `Stock Adjustment` depends on existing stock, batches, and stock summary.
- `Pharmacy Transactions` is most useful after purchase and adjustment flows already create ledger entries.

## Dependency Flow

```text
Pharmacy Vendors
  -> Purchase Order
  -> Stock Receive / Batch Creation
  -> Manage Stocks
  -> Stock Adjustment
  -> Pharmacy Transactions
```

## Sprint Plan

### Sprint 1: Pharmacy Vendors

Backend:

- create `PharmacyVendor` entity
- create vendor repository
- create vendor DTOs
- implement vendor CRUD APIs
- add validation and duplicate checks

Frontend:

- vendor list page
- vendor create/edit drawer
- vendor detail page basic profile

Reason:

- vendor is the base master data required for purchasing

### Sprint 2: Purchase Order Core

Backend:

- create purchase order header entity
- create purchase order item entity
- implement PO CRUD APIs
- implement PO statuses:
  - `DRAFT`
  - `APPROVED`
  - `CANCELLED`
- link PO to vendor
- reuse medicine search

Frontend:

- PO list page
- PO create page
- PO detail page
- PO item grid and totals

Reason:

- this establishes the purchase workflow before stock receiving

### Sprint 3: Stock Receiving and Inventory Foundation

Backend:

- create `PharmacyStockBatch`
- extend `PharmacyStock`
- implement PO receive API
- update stock summary logic
- write `PharmacyStockTransaction` rows on purchase receipt

Frontend:

- receive stock dialog or page from PO detail
- batch entry form:
  - batch no
  - expiry
  - received qty
  - purchase price
  - MRP
  - selling price
- PO receipt history section

Reason:

- inventory exists only after purchase receipt is posted

### Sprint 4: Manage Stocks

Backend:

- stock list API
- stock summary cards API
- stock detail API
- stock batches API
- stock transactions-by-medicine API

Frontend:

- manage stocks page
- summary cards
- filters
- stock table
- stock detail page

Reason:

- this is the first point where real stock data is available for display

### Sprint 5: Stock Adjustment

Backend:

- create adjustment header entity
- create adjustment item entity
- implement create and detail APIs
- add stock validation
- add batch increase/decrease rules
- update stock summary
- write transaction entries for adjustments

Frontend:

- adjustment list page
- create adjustment page
- adjustment detail page

Reason:

- adjustment must work on top of existing stock and batches

### Sprint 6: Pharmacy Transactions

Backend:

- transaction list API
- transaction detail API
- support filters:
  - date
  - medicine
  - batch
  - vendor
  - transaction type
  - reference

Frontend:

- transactions list page
- transaction detail drawer
- export option

Reason:

- by this stage, all upstream modules are already generating transaction records

## Recommended Backend Build Order

1. `PharmacyVendor`
2. `PharmacyPurchaseOrder`
3. `PharmacyPurchaseOrderItem`
4. `PharmacyStockBatch`
5. `PharmacyStock`
6. `PharmacyStockTransaction`
7. `StockAdjustment`

## Recommended Frontend Build Order

1. `Vendors`
2. `Purchase Orders`
3. `Receive Stock`
4. `Manage Stocks`
5. `Stock Adjustment`
6. `Transactions`

## Important Delivery Rule

Do not start the final `Manage Stocks` UI against fake inventory data if purchase receipt and stock batch creation are not ready.
Otherwise the stock page will need rework once real inventory rules are added.


# Phase 2: User Interface Implementation

## Objective
Implement visually stunning UI screens based on the Stitch exports (Organic Ledger design).

## Tasks

### 1. Product Details Screen (`ProductDetailScreen.kt`)
- Integrate the Stitch HTML structure into Compose using `Surface`, `Card`, and `Text`.
- Apply "Organic Editorialism":
  - Tonal depth: Use `surface-container-highest` and `surface-container-low` for layering instead of borders.
  - "Giá hôm nay" (Price Today) highlights.
  - History List: Zebra-striping list without dividers (`surface-container-low` every second item).
- Connect to `ProductDetailViewModel` observing `Product` and `Flow<List<PriceRecord>>`.

### 2. Update Price Flow
- Create `UpdatePriceDialog` (or BottomSheet) invoked from the product detail screen.
- Form inputs for `costPrice` and `salePrice`.
- Show current difference (e.g. `+10% margin`).
- CTA with gradient styling (`primary` to `primary-container`).
- Triggers `UpdatePriceEvent` in the ViewModel.

### 3. Add Product Flow Updates
- Enhance `AddEditProductScreen.kt`.
- When creating a product for the first time, automatically generate the first `PriceRecord` by utilizing the `updateProductPrice` transaction or embedding the initial history entry in the product JSON payload sent to multi-path update.

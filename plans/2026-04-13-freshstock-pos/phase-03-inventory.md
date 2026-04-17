# Phase 3: Inventory & Shrinkage (Hao Hụt)

[Context: plan.md](plan.md)

## Overview
**Date:** 2026-04-13
**Description:** Deepen the inventory module to meet specific "FreshStock" workflows, notably tracking damaged or expired goods (shrinkage).
**Priority:** High
**Implementation Status:** Pending
**Review Status:** Pending

## Key Insights
- Tracking "Hao hụt" (Shrinkage) is vital for grocery/fresh produce logistics.
- "Loss" needs to be distinct from a standard "Stock Out" (Xuất kho) mainly by enforcing a "Lý do" (Reason) field.

## Requirements
- Expand Product Cards across the app: Display Product Name, Current Stock, AND Sale Price.
- Add "Sửa" (Edit) and "Nhập thêm" (Restock) quick-actions directly onto list items for speed.
- Build `LossEntryScreen` specifying item, quantity damaged, and predefined loss reasons (e.g., Expired, Damaged, Quality control).

## Implementation Steps
1. Enhance `ProductItem` composables to inject Price and quick-action icon buttons.
2. Modify `GetProductsUseCase` to sort accurately by stock urgency (in-stock vs low-stock vs out-of-stock).
3. Create `CreateShrinkageUseCase` mapped under Stock.
4. Implement `ShrinkageScreen` mimicking the UI of StockOut but localized for loss metrics.

## Success Criteria
- [ ] Staff cannot edit Sale Price from the Product list card.
- [ ] Shrinkage generates a specific `StockEntryType.SHRINKAGE` record in Firebase.

# Implementation Plan: Dynamic Pricing & FreshStock History

**Date:** 2026-04-17
**Created By:** Nguyen Huy <huy.nh@ots.vn>
**Status:** IN_PROGRESS
**Complexity:** Medium
**Estimated Effort:** 10 hours

## Overview
Implement the FreshStock Dynamic Pricing system. The system allows managers to update `salePrice` and `costPrice` dynamically, keeps an immutable log of price changes, and provides UI for viewing price analytics. Order tracking is safely separated since they capture snapshot prices, ensuring integrity.

## Problem Statement
Currently, prices are static fields. We need to track historical changes without bloating the `Product` node and provide a dedicated UI for price adjustment and history checking. 

## Goals & Success Metrics
### Primary Goals
- [ ] Implement robust multi-path update in RTDB for Price changes.
- [ ] Build "Cập nhật giá" (Update Price) Screen.
- [ ] Build "Chi tiết Sản phẩm" (Product Details) with Price History list.
- [ ] Incorporate initial price saving into the "Add Product" flow.

### Success Metrics
- Price update transactions successfully hit both `/products` and `/product_price_history` without data corruption.
- Product list fetch size remains constant regardless of history length.

## Proposed Solution (Approach 1: Flat History Nodes)

### Architecture Overview
Use Firebase Realtime Database fan-out updates to update `/products/{pid}` and append a new record to `/product_price_history/{pid}` simultaneously.

### Technology Stack
- Kotlin Flow, ViewModel, Jetpack Compose.
- Firebase Realtime Database `updateChildren()`.

### Why This Approach?
Applying **YAGNI** and **Systems Thinking**: Embedding history natively ruins read performance for lists. Flattening is the NoSQL standard. 

## Alternative Approaches Considered

### Option 2: Embedded History Map
**Pros:** Simpler code, fewer repositories.
**Cons:** Severe performance degradation over time on product lists.
**Rejected because:** Fails scalable system design requirements for frequent pricing adjustments in fresh produce.

## Implementation Tasks

### Phase 1: Domain & Data Layer ✅ DONE
- [x] Task 1.1: Create `PriceRecord` domain model.
- [x] Task 1.2: Update `ProductRepository` to support `updatePrice(productId, newCost, newSale, userId)`.
- [x] Task 1.3: Create `getProductPriceHistory()` in `ProductRepository`.

### Phase 2: UI Implementation — IN_PROGRESS

### Phase 2: UI Implementation
- [ ] Task 2.1: Update `AddEditProductScreen` to toggle initial price history generation.
- [ ] Task 2.2: Implement `ProductDetailScreen` with price history UI (from Stitch export).
- [ ] Task 2.3: Implement `UpdatePriceBottomSheet/Screen` (from Stitch export).

## Risk Analysis
| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Partial DB Failure | Low | Med | Use RTDB multi-path atomic updates (`updateChildren`) |

## Unresolved Questions
- [ ] Do we need a mechanism to batch-delete very old price records (e.g., > 1 year)?

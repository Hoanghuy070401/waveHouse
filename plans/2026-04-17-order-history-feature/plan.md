# Implementation Plan: Order History (Lịch sử bán hàng)

**Date:** 2026-04-17
**Created By:** Nguyen Huy <huy.nh@ots.vn>
**Status:** Draft
**Complexity:** Medium
**Estimated Effort:** ~8-12 hours

## Overview

The WaveHouse POS application currently allows users to create orders and view "Stock History" (Lịch sử xuất nhập), but lacks a dedicated "Order History" (Lịch sử bán hàng) screen. This feature will allow store owners and staff to review past transactions, filter by date, and view detailed order line items.

## Problem Statement

To provide full visibility into sales, we need an interface where users can browse historical orders. A key technical challenge is Firebase RTDB's single-query limitation. We currently order by `warehouseId`. We cannot simultaneously do `orderBy("warehouseId")` AND `startAt(startDate)` AND `endAt(endDate)` natively on multiple fields without composite keys or fan-out structures.

## Goals & Success Metrics

### Primary Goals
- [ ] Create a dedicated Order History screen listing past orders (date, ID, total amount, status).
- [ ] Implement an Order Detail view showing individual items and snapshot prices.
- [ ] Support date filtering (e.g., Today, Yesterday, custom range).

### Success Metrics
- Performance: History screen loads initial items in under 1 second.
- Correctness: Snapshot prices (cost vs sell) perfectly match what happened at the time of sale.

## Proposed Approaches

Due to RTDB query limits, we have two approaches.

### Option 1: Client-Side Aggregation (The "KISS" Approach) - RECOMMENDED
**Pros:** No database migrations needed. Extremely fast to build.
**Cons:** Fetches a large chunk of data at once (e.g., last 500 orders); doesn't scale infinitely for massive historical queries without slow load times.
**Why it's Good:** For a typical SME shop, 500 recent orders is enough for 99% of "order history check" use-cases. If they need analytics, they will use a dedicated reporting dashboard.

### Option 2: Composite Key True Pagination (The Robust Approach)
**Pros:** Highly scalable. Supports infinite scroll without memory bloat.
**Cons:** Requires data migration (adding `compositeKey: "WH1_1713500200"` back-populating old data). Increased complexity in ViewModel via keyset cursors (`startAfter`).

**Recommendation:** Proceed with **Option 1**, keeping things simple (KISS). If the client scales massively, we transition to Option 2.

## Implementation Tasks (Option 1)

### Phase 1: Foundation
- [ ] Task 1.1: Add `getOrdersByDateRange` to `OrderRepositoryImpl` using `limitToLast(100)` or filtering locally after fetching the warehouse chunk.

### Phase 2: UI Implementation
- [ ] Task 2.1: Build `OrderHistoryScreen` (LazyColumn, Date Picker filter, Order Cards).
- [ ] Task 2.2: Build `OrderDetailScreen` (Itemized list, totals, statuses).
- [ ] Task 2.3: Connect navigation routes.

## Database Changes
No schema changes required for Option 1.
For Option 2, `warehouseId_createdAt` must be appended to the `Order` payload.

## Risk Analysis

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Large payload crash | Low | Med | Use `limitToLast(300)` as a hard cap. |
| Inconsistent snapshot price | Low | High | We already use `unitPrice` and `costPrice` snap-ins from OrderItem. |

## Timeline Estimate
| Phase | Estimate | Dependencies |
|---|---|---|
| Phase 1 | 2 hrs | None |
| Phase 2 | 6 hrs | Phase 1 |

## Unresolved Questions
- [ ] Do we want the ability to "Void/Cancel" a Paid order from this history screen?
- [ ] Should we restrict normal Staff from seeing the total cost/profit margin on the detailed screen?

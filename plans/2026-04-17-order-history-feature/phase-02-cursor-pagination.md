# Phase 02: Option 2 - Composite Key RTDB Pagination

## Context
If performance degradation occurs or the application successfully scales to enterprise volumes, pulling hundreds of orders upfront natively is unfeasible. We need server-side pagination.

## Overview
- Date: 2026-04-17
- Priority: Low (Backup approach)
- Status: Draft

## Key Insights
- Firebase Realtime Database limits sorting to exactly one child property per query.
- To sort by time while filtering by a specific tenant (`warehouseId`), the two pieces of info must be merged into a single indexable field: `warehouseId_createdAt`.

## Requirements
- Each order payload must output a new property: `warehouseId_time: "${warehouseId}_${createdAt}"`.
- Order lists must be pulled sequentially, using `orderByChild("warehouseId_time")` and `startAt`/`endAt`.

## Architecture
- `Order` database insert must append the composite key.
- A one-time Cloud Function or client-side batch migration script must run to populate existing fields.
- `OrderRepository` exposes a `getPaginatedOrders(warehouseId, startAfterTimestamp, limit)` method.

## Related Code Files
- `app/src/main/java/com/wavehouse/data/remote/firebase/OrderRepositoryImpl.kt` (modified `createOrder`)
- Database index configuration (`database.rules.json` to index `warehouseId_time`).

## Implementation Steps
1. Add composite logic to `createOrder`: `orderData["warehouseId_time"] = "${order.warehouseId}_${order.createdAt}"`.
2. Add pagination method `getPaginatedOrders(warehouseId, lastKey, limit)`.
3. Update `OrderHistoryViewModel` to manage a "Load More" cursor pattern.

## Todo List
- [ ] Modify `Order` push insertion in Repo.
- [ ] Write migration command file in `docs/` to guide developers on back-filling DB.
- [ ] Support Jetpack Compose `Paging3` or similar custom paginated LazyColumn.

## Success Criteria
- Order list is fetched in strict chunks of 20.
- Pulling down history memory costs remain O(1) in accordance to the visible list.

## Risk Assessment
- Complexity increases exponentially due to Compose UI needing state handling for loading more.
- Migration risks causing breaking discrepancies between new orders and old migrated orders if formatting isn't exactly aligned (leading zeros for timestamps, etc.).

## Security Considerations
- Need to update Firebase Security Rules to `.indexOn: ["warehouseId_time"]` or it will run full-table scans.

## Next Steps
- This phase shouldn't be executed unless specifically prioritized or performance issues on Option 1 arise.

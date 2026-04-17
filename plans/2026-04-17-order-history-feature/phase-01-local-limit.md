# Phase 01: Option 1 - Local Filtering via Limit Query (KISS Approach)

## Context
This is the recommended approach for the WaveHouse MVP context. Real-time Firebase database querying doesn't inherently support multi-filtering. Thus, we lean on retrieving a bounded slice of recent sales and performing subsequent filtering natively on the client device.

## Overview
- Date: 2026-04-17
- Priority: High
- Status: Draft

## Key Insights
- SME businesses usually only want to check "What did I sell today/yesterday".
- Downloading 300 JSON objects from Firebase is fast enough (<1s) and memory footprint is negligible on modern Android devices.

## Requirements
- Add a new data query to fetch the last `limit` orders. We already have `getOrders(warehouseId: String, limit: Int)` in `OrderRepositoryImpl` which uses `limitToLast`.
- Apply Kotlin standard library collections methods to filter and group by date blocks on the repository or ViewModel level.

## Architecture
- Use `OrderRepositoryImpl.getOrders(..., limit = 300)`.
- The ViewModel will map the emitted list to `uiState.filteredOrders`.
- Filter parameters (startDate, endDate) exist in the ViewModel. Whenever they change, re-map from the cached 300 orders.

## Related Code Files
- `app/src/main/java/com/wavehouse/data/remote/firebase/OrderRepositoryImpl.kt`
- `app/src/main/java/com/wavehouse/presentation/order/history/OrderHistoryViewModel.kt` (New)
- `app/src/main/java/com/wavehouse/presentation/order/history/OrderHistoryScreen.kt` (New)

## Implementation Steps
1. Create `OrderHistoryViewModel`.
2. Observe `repository.getOrders()`, saving the list state.
3. Build the UI: Header (Filter by Custom Date picker), Body (`LazyColumn` grouped by Dates using sticky headers).
4. Tap order -> Navigate to detail.

## Todo List
- [ ] Implement UI State classes for Order History.
- [ ] Connect `getOrders` to the History UI.
- [ ] Implement DatePicker range selection logic.
- [ ] Verify sorting behaves correctly (newest -> oldest).

## Success Criteria
- History renders within 1 second. Filter changes apply immediately since they are in-memory.

## Risk Assessment
- Scalability: If a client generates 500+ orders a day, this history will only cover the most recent half-day unless we increase the limit. However, 500 orders/day is highly optimistic for MVP shops.

## Security Considerations
- Validate warehouse permissions so Staff cannot see orders from other warehouses.

## Next Steps
- Consider Option 2 if shop volume crosses 1,000 orders per day continually.

# Phase 4: Business Reports & Polish

[Context: plan.md](plan.md)

## Overview
**Date:** 2026-04-13
**Description:** Implement the visualization dashboard strictly meant for warehouse/store Owners, and apply final UX polish confirming the 1-Tap speed rules.
**Priority:** Medium
**Implementation Status:** Pending
**Review Status:** Pending

## Key Insights
- The Report tab must aggregate data comprehensively: Daily Revenue, Orders count, and Shrinkage vs Sale comparisons.
- Speed is everything. If the Reports tab takes 5 seconds to load because of massive Firestore queries, it violates the UX rules.

## Requirements
- Aggregation: Cloud Functions or efficient client-side chunked querying to assemble Doanh thu (Revenue).
- `ReportScreen` displays large, easy-to-read KPI cards and simple trend charts.
- "Top Sản Phẩm Mùa Vụ": Identify the fastest moving inventory dynamically.

## Implementation Steps
1. Expand `ReportScreen` to include interactive revenue trend charts.
2. Ensure Firebase Rules prevent Staff from accessing the aggregated billing data.
3. Polish UI: Re-verify that the app holds minimal data overload. Shrink font sizes on metadata to ensure clean screens.

## Success Criteria
- [ ] Dashboards load under 2 seconds.
- [ ] Graphical representation cleanly scales on standard phone screens without side-scrolling.

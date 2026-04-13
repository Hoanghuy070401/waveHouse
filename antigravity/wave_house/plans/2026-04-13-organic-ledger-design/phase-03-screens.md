# Phase 3: Screen Refactoring

[Context: plan.md](plan.md)

## Overview
**Date:** 2026-04-13
**Description:** Roll out the finalized components and spacing paradigms across all implemented screens, purging legacy standard UI setups.
**Priority:** Medium
**Implementation Status:** Pending
**Review Status:** Pending

## Key Insights
- Screen lists need to abandon `HorizontalDivider()` loops in favor of 8dp gaps or "Zebra Striping" background shifts.
- Any hard black text must be replaced with `MaterialTheme.colorScheme.onSurface` (mapped to `#181d17`).

## Requirements
- Refactor all List views: `ProductListScreen`, `DashboardScreen` (History), `StockHistory` to use Organic list patterns.
- Refactor top app bars to `GlassTopAppBar`.
- Update KPIs and detail screens to utilize the Display typography for values.
- Replace manual `Card` calls with our localized Organic Ledger Cards.

## Implementation Steps
1. Auth & Splash screens: Switch completely to gradient buttons & appropriate backgrounds.
2. Dashboard & Navigation: Update bottom bar and dashboard layout.
3. Products & Stock Views: Eliminate horizontal dividers natively used between list items. Use staggered surface/surfaceContainerLow items.
4. Confirm iconography matches 24dp size and exact `#40493d` off-black weighting requirement.

## Success Criteria
- [ ] Application visually reads cleanly with clear layout layers without relying on structured strokes.
- [ ] Typography follows the precise Manrope tracking sizes mapped earlier.
- [ ] No 1px dividers are rendering anywhere in data grids.

# Phase 2: Components Overhaul

[Context: plan.md](plan.md)

## Overview
**Date:** 2026-04-13
**Description:** Build generic UI components (Buttons, Cards, Inputs, TopBars, Chips) matching the precise rules of Organic Ledger to ensure reusability.
**Priority:** High
**Implementation Status:** Pending
**Review Status:** Pending

## Key Insights
- Buttons require a unique gradient, meaning `Button` container colors must be transparent and utilize a background modifier.
- The "No-Line" rule explicitly forbids `1px` lines -> use "Zebra Striping" or 8dp gaps.
- Modals/Floating items use an ambient shadow (20-40px blur, 6% alpha).

## Requirements
- `PrimaryGradientButton`: 135-degree gradient from `#0d631b` to `#2e7d32`, no border.
- `GhostBorderCard`: for accessibility fallbacks `outline_variant` at 20%.
- `Glass TopBar`: custom Compose Modifier for backdrop blur of 12px.
- `LogisticsChip`: 100% (CircleSize) roundedness with semantic background mapping.
- Custom Inputs: focus state transitions background to `surface_container_lowest` with a ghost 1px border.

## Implementation Steps
1. Create `core/ui/components/Buttons.kt`. Build primary, secondary, and ghost buttons.
2. Create `core/ui/components/Cards.kt`. Integrate ambient shadow utilities for floating elements.
3. Create `core/ui/components/Inputs.kt`. Build the reactive text fields.
4. Setup `GlassTopAppBar` applying alpha modifiers and blurs.

## Success Criteria
- [ ] Buttons display gradients.
- [ ] Inputs transition background colors cleanly on focus.
- [ ] No hard shadows on cards (only ambient shadows for modals).

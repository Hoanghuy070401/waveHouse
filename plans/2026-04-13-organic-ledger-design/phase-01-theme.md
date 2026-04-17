# Phase 1: Theme Setup

[Context: plan.md](plan.md)

## Overview
**Date:** 2026-04-13
**Description:** Establish the base thematic tokens (Color, Type, Shape) inside `core/ui/theme` mapping directly to the "Organic Ledger" instructions.
**Priority:** High
**Implementation Status:** Pending
**Review Status:** Pending

## Key Insights
- Standard Material 3 tokens need to be carefully overridden to achieve the No-Line rule.
- Base background sits at `surface` (#f7fbf0). 
- Font files `Manrope` and `Work Sans` need to be embedded in resources (`res/font`).

## Requirements
- Introduce explicit hex values corresponding to the design doc and map them accurately to Material 3 variables (`surface`, `surfaceContainerLow`, `primary`, etc.).
- Build typographic scale using Manrope for Display/Headline and Work Sans for Body/Label.
- Adjust global shape config so default interactive roundedness is `xl` (12dp).

## Architecture
- `core/ui/theme/Color.kt` -> Maps hex codes to values.
- `core/ui/theme/Type.kt` -> Typography definitions.
- `core/ui/theme/Theme.kt` -> MaterialTheme wrapping.
- Custom extensions (e.g., custom gradient primary provider if needed).

## Related Code Files
- `app/src/main/java/com/wavehouse/core/ui/theme/*`
- `app/src/main/res/font/*`

## Implementation Steps
1. Download Manrope and Work Sans from Google Fonts. Add to `res/font`.
2. Update `Color.kt` with the organic palette exact hex colors.
3. Update `Type.kt` to bind the new fonts to Material 3 Typography objects with appropriate trackings.
4. Update `Theme.kt` shapes to remove 0px edge defaults, standardize 12dp, and update color roles.

## Success Criteria
- [ ] Font preview reflects new typography.
- [ ] Default Compose components inherit standard 12dp roundness.
- [ ] Base background is correctly rendering as `#f7fbf0`.

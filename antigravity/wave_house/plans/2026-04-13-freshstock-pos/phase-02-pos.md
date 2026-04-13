# Phase 2: POS Module (Highest Priority)

[Context: plan.md](plan.md)

## Overview
**Date:** 2026-04-13
**Description:** Implement the ultra-fast Point-of-Sale selling layout where quick additions, cart adjustments, and checkouts rule supreme.
**Priority:** Critical
**Implementation Status:** Pending
**Review Status:** Pending

## Key Insights
- "1 chạm" (1-tap) philosophy: Zero unnecessary dialogs for standard item additions.
- Architecture: A split screen (vertically stacked on phones). Top: Search and hot-keys. Mid: Live cart. Bottom: Huge Checkout action.

## Requirements
- Maintain a local reactive `CartSession` tracking `Pair<Product, Quantity>`.
- Quick-Add Grid for high-traffic products.
- Checkout immediately writes to Firestore via a transaction, deducts `Stock`, and logs an `ORDER` entry instead of just a raw stock adjustment.

## Implementation Steps
1. Define `CartItem` and `Order` models in Domain.
2. Build `PosScreen.kt`. Top section uses a debounced search bar.
3. Middle list requires `+` and `-` steppers natively on the row to avoid tapping into details.
4. Build `PosViewModel` managing the complex cart state securely.
5. Create `CheckoutUseCase` wrapping standard transactions.

## Success Criteria
- [ ] Items add to cart instantly on tap.
- [ ] Checkout reduces inventory values in Firestore robustly.

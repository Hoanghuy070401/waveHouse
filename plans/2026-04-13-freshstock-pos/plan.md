# Implementation Plan: FreshStock POS & Inventory Pivot

**Date:** 2026-04-13
**Created By:** AI Assistant
**Status:** Draft
**Complexity:** High
**Estimated Effort:** 34 story points

## Overview
Pivoting the `WaveHouse` application structure to match the **FreshStock** Product Requirements Document (PRD). The primary goal is to introduce Role-based Access Control (RBAC), a high-speed POS (Bán hàng) interface, and an expanded inventory management module with shrinkage (Hao hụt) tracking.

## Phases

### Phase 1: Navigation & Role Management (Pending)
- Refactor `BottomNavBar` and `AppNavHost` to 5 new core tabs: 🏠 Trang chủ, 📦 Kho hàng, 🛒 Bán hàng, 📊 Báo cáo, 👤 Tài khoản.
- Enforce Role Management (Owner vs. Staff).
- Add permission gates on tabs (Staff cannot see 'Báo cáo', 'Nhập hàng nâng cao', 'Sửa giá').
- Link: [Phase 01: Core Pivot](phase-01-core-pivot.md)

### Phase 2: POS System (Highest Priority) (Pending)
- Develop `PosScreen` acting as the lightning-fast Point of Sale grid.
- Build reactive Quick Add buttons and a floating/anchored Cart system.
- Implement checkout logic and update global stock synchronously.
- Link: [Phase 02: POS Module](phase-02-pos.md)

### Phase 3: Advanced Inventory & Shrinkage (Pending)
- Refactor `StockOverviewScreen` and product lists to show dynamic stock cards (Name, Stock, Price, Quick Restock/Edit).
- Build the "Hao hụt" (Loss/Shrinkage) flow to record damaged goods and adjust stock deductively.
- Link: [Phase 03: Inventory & Shrinkage](phase-03-inventory.md)

### Phase 4: Reports & Polish (Pending)
- Build the Business Dashboard for Owners (Daily Revenue, Total Orders, Top Products).
- Implement charts for revenue trends and shrinkage breakdown.
- Ensure 1-tap UX principles hold across mobile form factors.
- Link: [Phase 04: Reports](phase-04-reports.md)

## Next Steps
Please review this plan. Upon your approval, we will trigger the implementation workflow by executing:
`/code plans/2026-04-13-freshstock-pos/plan.md`

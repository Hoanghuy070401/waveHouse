# Phase 1: Core Pivot (Navigation & Roles)

[Context: plan.md](plan.md)

## Overview
**Date:** 2026-04-13
**Description:** Structural changes to the NavHost and BottomNavigationBar to establish the 5 core tabs. Introduce Role definitions and strict permission gates based on user type.
**Priority:** High
**Implementation Status:** Pending
**Review Status:** Pending

## Key Insights
- Standard 5-tab breakdown mapping tightly to business roles constraints is essential.
- Current domain model uses `UserRole` (likely `ADMIN`/`STAFF` or `OWNER`/`STAFF`). We need to guarantee that Staff encounters clear UI blockades or invisible hidden tabs for `Báo cáo`.

## Requirements
- Refactor `BottomNavDestination` items: 🏠 Trang chủ (Dashboard), 📦 Kho hàng (Inventory), 🛒 Bán hàng (POS), 📊 Báo cáo (Reports), 👤 Tài khoản (Settings/Profile).
- In `AppNavHost` and underlying BottomBar logic, filter the "Báo cáo" destination for `STAFF`.
- Hide "Sửa giá" (Edit Price) buttons from Staff across the app.

## Implementation Steps
1. Update `BottomNavDestination` in `AppNavHost.kt`.
2. Wrap the global navigation injection with a state block evaluating `currentUser.role`.
3. Build a "Staff Restricted" banner composable that can be cleanly overlaid on the Dashboard.
4. Update `SettingsScreen` to represent the "Tài khoản" view, pulling "Switch Warehouse" to the forefront.

## Success Criteria
- [ ] 5 tabs show for Owner. 4 tabs show for Staff.
- [ ] No routing leak allows Staff to force-navigate to `Routes.Report.route`.

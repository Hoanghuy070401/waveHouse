# Code Review Report — WaveHouse UI + Logic Bugs

> **Date:** 2026-04-20  
> **Scope:** Existing features, UI bugs, logic/algorithm bugs, navigation/state issues

## Code Review Summary

### Scope

- Files reviewed:
  - `app/src/main/java/com/wavehouse/core/ui/navigation/AppNavHost.kt`
  - `app/src/main/java/com/wavehouse/core/ui/navigation/Routes.kt`
  - `app/src/main/java/com/wavehouse/presentation/auth/login/LoginViewModel.kt`
  - `app/src/main/java/com/wavehouse/presentation/auth/register/RegisterViewModel.kt`
  - `app/src/main/java/com/wavehouse/presentation/dashboard/DashboardViewModel.kt`
  - `app/src/main/java/com/wavehouse/presentation/product/list/ProductListScreen.kt`
  - `app/src/main/java/com/wavehouse/presentation/product/list/ProductListViewModel.kt`
  - `app/src/main/java/com/wavehouse/presentation/pos/PosScreen.kt`
  - `app/src/main/java/com/wavehouse/presentation/pos/PosViewModel.kt`
  - `app/src/main/java/com/wavehouse/presentation/order/history/OrderHistoryScreen.kt`
  - `app/src/main/java/com/wavehouse/presentation/order/history/OrderHistoryViewModel.kt`
  - `app/src/main/java/com/wavehouse/presentation/settings/SettingsScreen.kt`
  - `app/src/main/java/com/wavehouse/presentation/settings/SettingsViewModel.kt`
  - `app/src/main/java/com/wavehouse/presentation/report/ReportViewModel.kt`
  - `app/src/main/java/com/wavehouse/presentation/staff/ManageStaffScreen.kt`
  - `app/src/main/java/com/wavehouse/presentation/staff/ManageStaffViewModel.kt`
  - `app/src/main/java/com/wavehouse/data/remote/firebase/OrderRepositoryImpl.kt`
  - `app/src/main/java/com/wavehouse/data/remote/firebase/ProductRepositoryImpl.kt`
  - `app/src/main/java/com/wavehouse/domain/model/Models.kt`
  - `app/src/main/java/com/wavehouse/domain/repository/Repositories.kt`
  - `app/src/main/java/com/wavehouse/domain/usecase/product/ProductUseCases.kt`
- Lines analyzed: ~1,800
- Review focus: current feature coverage + UI/state bugs + inventory/order consistency
- Updated plans: none

### Overall Assessment

Codebase khá tốt về cấu trúc MVVM + Clean Architecture. Feature coverage rộng, nav flow rõ, và state management nhìn chung đúng hướng. Nhưng có vài lỗi thật, không phải style nit: một số flow có thể bị kẹt loading, product search/refresh tạo nhiều collectors, và order checkout không atomic giữa stock deduction với order write.

### Critical Issues

1. **Non-atomic order creation can corrupt stock/order consistency**
   - File: `OrderRepositoryImpl.kt`
   - Problem: stock is decremented per item via transaction first, then order record + `products.currentStock` are written in a separate `updateChildren` call.
   - Impact: if the batch write fails after transactions commit, stock is already deducted but order record is missing / partial.
   - Severity: **High**

### High Priority Findings

1. **POS screen can get stuck in loading forever when user is null**
   - File: `PosViewModel.kt`
   - Problem: `loadProducts()` returns early if `getCurrentUserUseCase()` returns null, but never sets `isLoading = false` or an error.
   - Impact: POS UI can show infinite spinner on expired session / missing user.
   - Severity: **High**

2. **Product list refresh/search can leak collectors and duplicate listeners**
   - File: `ProductListViewModel.kt`
   - Problem: `observeSearch()` calls `loadProducts()` every time query becomes blank; `onRefresh()` also calls `initUser()`. Both launch new collecting coroutines without cancelling old ones.
   - Impact: multiple Firebase listeners, duplicated updates, unnecessary load, harder-to-debug UI jitter.
   - Severity: **High**

### Medium Priority Improvements

1. **Product list ignores error state**
   - File: `ProductListScreen.kt`
   - Problem: view model has `error`, but UI shows empty state instead of error when `products.isEmpty()`.
   - Impact: real failures look like empty inventory.
   - Severity: **Medium**

2. **Settings role gate has dead branch**
   - File: `SettingsScreen.kt`
   - Problem: checks `userRole == "ADMIN" || userRole == "OWNER"`, but actual enum only has `ADMIN`, `WAREHOUSE`, `ACCOUNTANT`, `STAFF`.
   - Impact: confusing maintenance / misleading code; not a runtime break today.
   - Severity: **Low-Medium**

3. **Duplicate/unused navigation route**
   - File: `AppNavHost.kt`, `Routes.kt`
   - Problem: both `Routes.Account` and `Routes.Settings` exist, but only `Routes.Account` is reachable in current nav graph.
   - Impact: dead route, future nav drift.
   - Severity: **Low**

### Low Priority Suggestions

- `DashboardViewModel` swallows warehouse-name fetch exceptions silently; log at least debug level.
- `ManageStaffScreen`/`ViewModel` can use enum-based role checks instead of raw string comparisons for clarity.
- Consider centralizing loading-state reset logic in ViewModels to avoid similar hangs.

### Positive Observations

- Clean separation of `UI → ViewModel → UseCase → Repository`.
- POS quantity logic guards against exceeding stock.
- Order history filtering is client-side and easy to understand.
- Role-based navigation is already partially wired into bottom nav.

### Recommended Actions

1. Fix `PosViewModel.loadProducts()` to always clear loading on null user.
2. Refactor `ProductListViewModel` to keep a single product stream; do not spawn new collectors on every refresh/search reset.
3. Make `OrderRepositoryImpl.createOrder()` fully atomic or add rollback strategy / compensation if stock write fails.
4. Show explicit error UI in `ProductListScreen`.
5. Clean up dead route / dead role branch.

### Metrics

- Type Coverage: not measured
- Test Coverage: not measured
- Linting Issues: not run

### Unresolved Questions

- Should POS order creation be truly atomic at database level, or is compensation acceptable for this app?
- Is `Routes.Settings` intentionally reserved for a future screen, or should it be removed now?

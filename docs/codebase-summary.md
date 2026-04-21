# Codebase Summary — WaveHouse
> **Phiên bản:** 1.2.0 | **Cập nhật:** 2026-04-21

## Documentation Maintenance
**Last Updated:** 2026-04-21
**Document Version:** 1.2
**Maintained By:** Development Team

---

## Tổng Quan Kiến Trúc
Dự án WaveHouse xây dựng theo **Clean Architecture** + **MVVM**, sử dụng **Jetpack Compose** (UI), **Hilt** (DI), **Firebase Realtime Database** (backend chính).

```
Presentation (Compose + ViewModel)
       ↕ suspend / Flow
Domain (UseCases, Repository Interfaces, Models)
       ↕ ApiResult<T>
Data (Firebase Impl, Room Cache, Auth)
```

---

## Cấu Trúc Thư Mục

```
app/src/main/java/com/wavehouse/
├── core/
│   ├── di/            — Hilt modules (DB, Firebase, Network, Repository)
│   ├── network/       — ApiResult<T>, NetworkUtils
│   ├── ui/
│   │   ├── navigation/ — AppNavHost, Routes, BottomNavBar
│   │   └── theme/     — Color, Type, Theme (M3, green palette)
│   ├── utils/         — DateUtils, Extensions, CurrencyInputUtils
│   └── service/       — WaveHouseFcmService (FCM push)
├── data/
│   ├── local/         — Room DB (WaveHouseDatabase, Entities, DAOs, Converters)
│   └── remote/firebase/
│       ├── AuthRepositoryImpl.kt
│       ├── OrderRepositoryImpl.kt
│       ├── ProductRepositoryImpl.kt
│       ├── StockRepositoryImpl.kt
│       ├── SupplierRepositoryImpl.kt
│       └── WarehouseRepositoryImpl.kt
├── domain/
│   ├── model/Models.kt    — Tất cả domain entities
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   └── Repositories.kt   — Product/Stock/Supplier/Order/Warehouse
│   └── usecase/           — AuthUseCases, PosUseCases, ProductUseCases, StockUseCases
└── presentation/
    ├── auth/              — login, register, forgotpassword, emailverification, changepassword
    ├── dashboard/         — DashboardScreen + DashboardViewModel
    ├── product/           — list, detail, addedit, scanner
    ├── stock/             — stockin, stockout, history, lowstock, shrinkage, overview
    ├── pos/               — PosScreen + PosViewModel
    ├── order/
    │   ├── history/       — OrderHistoryScreen + OrderHistoryViewModel
    │   └── detail/        — OrderDetailScreen + OrderDetailViewModel
    ├── supplier/          — list, addedit
    ├── report/            — ReportScreen
    ├── staff/             — ManageStaffScreen
    └── settings/          — SettingsScreen
```

---

## Domain Models Chính (`Models.kt`)

| Model | Mô tả |
|-------|-------|
| `User` | Người dùng: role (Admin/Warehouse/Accountant/Staff), status (Active/Pending) |
| `Product` | Sản phẩm: giá vốn MAC, giá bán, `allowDecimal`, `currentStock` |
| `PriceRecord` | Lịch sử thay đổi giá sản phẩm |
| `StockItem` | Snapshot tồn kho một sản phẩm |
| `StockEntry` | Phiếu nhập/xuất/hao hụt |
| `Order` | Đơn hàng POS: danh sách items, tổng tiền, trạng thái |
| `OrderItem` | Dòng sản phẩm trong đơn hàng (qty, unitPrice, lineTotal) |
| `CartItem` | Item trong giỏ hàng POS (tạm thời, chưa lưu) |
| `Supplier` | Nhà cung cấp |
| `Warehouse` | Kho hàng (qrImageUrl cho thanh toán QR) |
| `DashboardStats` | KPI tổng hợp cho Dashboard |
| `ReportStats` | Thống kê cho màn hình Báo cáo |

---

## Repository Interfaces

| Interface | Các method chính |
|-----------|-----------------|
| `ProductRepository` | `getProducts()`, `searchProducts()`, `createProduct()`, `updateProductPrice()`, `getProductPriceHistory()` |
| `StockRepository` | `createStockIn()`, `createStockOut()`, `createShrinkage()`, `adjustStock()`, `getTodayStats()` |
| `OrderRepository` | `createOrder()`, `confirmPayment()`, `getOrders()`, `getOrderById()` |
| `SupplierRepository` | CRUD suppliers |
| `WarehouseRepository` | `getWarehouses()`, `switchWarehouse()`, `updateQrImageUrl()`, `uploadQrImage()` |
| `AuthRepository` | `login()`, `register()`, `logout()`, `getCurrentUser()`, `resetPassword()` |

---

## Thay Đổi Nổi Bật Gần Đây

### 2026-04-21 (Phiên này)
- Cập nhật toàn bộ docs phản ánh trạng thái thực tế của codebase

### 2026-04-17
- Implement **OrderHistoryScreen** & **OrderDetailScreen** (client-side aggregation, `limitToLast(300)`)
- `OrderHistoryViewModel` dùng `AuthRepository.getCurrentUser()` lấy `warehouseId`
- Thêm navigation shortcut từ POS top bar → Order History
- **POS UI refactor**: Grid sản phẩm cuộn ngang 2 hàng (`LazyRow`)
- Checkout bar compact: 1 hàng ngang (tổng tiền bên trái, nút bên phải)
- `Double.toVndString()` extension cho định dạng tiền nhất quán
- `PrimaryGreen` alias thêm vào `Color.kt` (= `Primary40`)
- `HorizontalDivider` thay thế deprecated `Divider`
- Centralize `todayStartMillis()` trong `DateUtils.kt`

### Before 2026-04-17
- Cờ `allowDecimal` trên Product: cho phép/chặn bán lẻ số thập phân tại POS
- Dynamic pricing: `updateProductPrice()` với ghi chú lý do thay đổi
- Lịch sử giá sản phẩm (`PriceRecord` model + UI)
- RBAC hoàn chỉnh + Pending Approval flow
- Migrate hoàn toàn Firestore → Realtime Database

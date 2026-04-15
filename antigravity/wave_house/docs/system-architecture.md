# System Architecture — WaveHouse

> **Phiên bản:** 1.0.0 (Draft) | **Cập nhật:** 2026-04-13

---

## 1. Kiến Trúc Tổng Thể

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌──────────────┐   ┌─────────────┐   ┌──────────────────┐  │
│  │  Compose UI  │◄──│  ViewModel  │◄──│   UI State Flow  │  │
│  │  (Screens)   │   │  (StateFlow)│   │   (UiState.kt)   │  │
│  └──────────────┘   └──────┬──────┘   └──────────────────┘  │
└─────────────────────────────┼───────────────────────────────┘
                              │ uses
┌─────────────────────────────▼───────────────────────────────┐
│                      DOMAIN LAYER                            │
│  ┌────────────────┐   ┌──────────────────────────────────┐  │
│  │   Use Cases    │   │         Domain Models             │  │
│  │  (Pure Kotlin) │   │  Product, StockEntry, Supplier    │  │
│  └────────┬───────┘   └──────────────────────────────────┘  │
│           │ uses Repository Interfaces                        │
└───────────┼─────────────────────────────────────────────────┘
            │
┌───────────▼─────────────────────────────────────────────────┐
│                       DATA LAYER                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Repository Implementations              │    │
│  │  ProductRepository | StockRepository | AuthRepository│    │
│  └──────────┬──────────────────────────┬───────────────┘    │
│             │                          │                      │
│  ┌──────────▼───────┐     ┌────────────▼──────────────────┐ │
│  │  Remote Sources   │     │      Local Sources            │ │
│  │  ┌─────────────┐ │     │  ┌─────────────┐              │ │
│  │  │ Realtime DB │ │     │  │    Room DB  │              │ │
│  │  │  Retrofit   │ │     │  │  DataStore  │              │ │
│  │  └─────────────┘ │     │  └─────────────┘              │ │
│  └───────────────────┘     └───────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Module Structure (Feature-Based)

```
app/
├── src/main/java/com/wavehouse/
│   ├── WaveHouseApp.kt              # Application class (Hilt)
│   ├── MainActivity.kt              # Single Activity
│   │
│   ├── core/                        # Shared utilities
│   │   ├── di/                      # Hilt modules
│   │   │   ├── NetworkModule.kt     # Retrofit, OkHttp
│   │   │   ├── FirebaseModule.kt    # Realtime DB, Auth, Storage
│   │   │   ├── DatabaseModule.kt    # Room
│   │   │   └── RepositoryModule.kt  # Bind interfaces → impl
│   │   ├── network/
│   │   │   ├── ApiResult.kt         # Sealed class: Success/Error/Loading
│   │   │   ├── NetworkInterceptor.kt
│   │   │   └── TokenAuthenticator.kt
│   │   ├── ui/
│   │   │   ├── theme/               # Material 3 theme, colors, typography
│   │   │   ├── components/          # Reusable Compose components
│   │   │   └── navigation/          # NavHost, Routes
│   │   └── utils/
│   │       ├── Extensions.kt
│   │       ├── DateUtils.kt
│   │       └── BarcodeUtils.kt
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── db/                  # Room Database
│   │   │   │   ├── WaveHouseDatabase.kt
│   │   │   │   ├── dao/             # ProductDao, StockDao, etc.
│   │   │   │   └── entity/          # Room entities
│   │   │   └── datastore/           # User preferences
│   │   ├── remote/
│   │   │   ├── firebase/            # Realtime Database sources
│   │   │   └── api/                 # Retrofit services + DTOs
│   │   └── repository/              # Repository implementations
│   │
│   ├── domain/
│   │   ├── model/                   # Pure domain models
│   │   │   ├── Product.kt
│   │   │   ├── StockEntry.kt
│   │   │   ├── Supplier.kt
│   │   │   ├── Warehouse.kt
│   │   │   └── User.kt
│   │   ├── repository/              # Repository interfaces
│   │   └── usecase/                 # Business logic use cases
│   │       ├── product/
│   │       ├── stock/
│   │       ├── supplier/
│   │       └── auth/
│   │
│   └── presentation/                # Feature screens
│       ├── auth/                    # Login, Register, ForgotPassword
│       ├── dashboard/               # Home dashboard, KPIs
│       ├── product/                 # Product list, detail, add/edit
│       ├── stock/                   # Stock in/out, history
│       ├── supplier/                # Supplier management
│       ├── report/                  # Reports & analytics
│       └── settings/                # App settings, profile
```

---

## 3. Navigation Graph

```
Root NavHost
├── Auth Graph
│   ├── LoginScreen
│   ├── RegisterScreen (Admin only)
│   ├── ForgotPasswordScreen
│   ├── EmailVerificationScreen
│   └── NewPasswordScreen (via Deep Link)
│
└── Main Graph (Bottom Nav)
    ├── DashboardScreen           [Tab 1]
    ├── Product Graph             [Tab 2]
    │   ├── ProductListScreen
    │   ├── ProductDetailScreen
    │   ├── AddEditProductScreen
    │   └── BarcodeScanScreen
    ├── Stock Graph               [Tab 3]
    │   ├── StockOverviewScreen
    │   ├── StockInScreen
    │   ├── StockOutScreen
    │   └── StockHistoryScreen
    ├── ReportScreen              [Tab 4]
    └── SettingsScreen            [Tab 5]
```

---

## 4. Realtime Database Data Model

```
/users/{userId}
  name, email, role, warehouseId, createdAt

/warehouses/{warehouseId}
  name, address, managerId, createdAt

/products/{productId}
  name, sku, barcode, categoryId, unitId
  description, imageUrl
  minStock, warehouseId

/stock/{warehouseId}/items/{productId}
  quantity, lastUpdated, updatedBy

/stock_entries/{entryId}
  type: "IN" | "OUT" | "ADJUST" | "TRANSFER"
  productId, warehouseId, quantity
  note, supplierId?, createdBy, createdAt

/suppliers/{supplierId}
  name, phone, email, address

/categories/{categoryId}
  name, parentId?

/notifications/{userId}/items/{notifId}
  title, body, type, isRead, createdAt
```

---

## 5. Data Flow: Offline-First Strategy

```
User Action
    │
    ▼
ViewModel → UseCase → Repository
                          │
                    ┌─────┴──────┐
                    │            │
              Local (Room)   Remote (Realtime DB)
                    │            │
                    └─────┬──────┘
                          │ Single Source of Truth
                          ▼
                      UI (StateFlow)
```

**Quy tắc:**
1. Write → Room trước → sync lên Realtime Database
2. Read → Room cache → Realtime Database listener cập nhật Room
3. Conflict → Timestamp-based last-write-wins

---

## 6. Security Architecture

```
Realtime Database Security Rules
├── /users   → self read/write; admin read all
├── /products → auth required; role-based write
├── /stock   → warehouseId match; thủ kho write
└── /reports → kế toán read; no write
```

**API Security:**
- Bearer token (Firebase ID Token) trong Authorization header
- Token auto-refresh qua `TokenAuthenticator` (OkHttp Authenticator)
- Certs pinning cho production API

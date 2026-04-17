# WaveHouse — Memory Bank Context

> **Branch:** main | **Cập nhật:** 2026-04-13

---

## Dự Án

**WaveHouse** — Ứng dụng quản lý kho hàng Android
- Package: `com.wavehouse`
- minSdk: 26 (Android 8.0) | targetSdk: 35
- Kiến trúc: MVVM + Clean Architecture

---

## Tech Stack (Đã Xác Nhận)

| Layer | Tech |
|-------|------|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt 2.51 |
| Networking | **Firebase primary** (Retrofit minimal) |
| Cloud DB | Firebase Firestore (offline enabled) |
| Auth | Firebase Authentication |
| Local DB | Room (offline cache) |
| Barcode | ML Kit + CameraX |

---

## Quyết Định Quan Trọng

### Firebase là Primary Data Source
- **Không dùng nhiều Retrofit** — chủ yếu Firebase Firestore
- Retrofit giữ lại trong code nhưng minimal, dùng khi cần REST sau này

### Stock Out Error Message Format
```
"Số tồn trong kho không đủ, hiện tại tồn ${currentStock}"
```
→ File: `StockUseCases.kt > CreateStockOutUseCase`

### Barcode Scanner
- Scan barcode/QR → tìm sản phẩm theo barcode
- **THÊM Yêu Cầu**: Hỗ trợ tìm theo tên trong danh sách theo danh mục
- File: `BarcodeScanScreen.kt` (Phase 3)

### google-services.json
- Vị trí đúng: `app/google-services.json` ✅
- Root `google-services.json` → có thể xoá
- Firebase project đã tạo, sẽ cung cấp SHA fingerprints sau

---

## Cấu Trúc Thư Mục Hiện Tại

```
wave_house/
├── .gitignore
├── .memory-bank/
│   ├── .gitkeep
│   └── context.md          ← FILE NÀY
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── local.properties.example
├── docs/
│   ├── tech-stack.md
│   ├── system-architecture.md
│   ├── project-overview-pdr.md
│   └── code-standards.md
├── plans/
│   └── 2026-04-13-wavehouse/
│       └── plan.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    ├── google-services.json  ← CẦN CÓ
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/wavehouse/
        │   ├── WaveHouseApp.kt
        │   ├── MainActivity.kt
        │   ├── core/
        │   │   ├── di/       (NetworkModule, FirebaseModule, DatabaseModule, RepositoryModule)
        │   │   ├── network/  (ApiResult, NetworkInterceptor)
        │   │   ├── service/  (WaveHouseFcmService)
        │   │   ├── ui/
        │   │   │   ├── theme/      (Color, Type, Theme)
        │   │   │   └── navigation/ (Routes, AppNavHost, BottomNavBar)
        │   │   └── utils/    (Extensions, DateUtils)
        │   ├── data/
        │   │   ├── local/    (db, dao, entity)
        │   │   └── remote/firebase/ (Auth, Product, Stock, SupplierRepositoryImpl)
        │   ├── domain/
        │   │   ├── model/    (Models.kt — tất cả domain models)
        │   │   ├── repository/ (interfaces)
        │   │   └── usecase/  (auth, product, stock)
        │   └── presentation/
        │       ├── auth/     (splash, login)
        │       ├── dashboard/
        │       ├── product/list/
        │       ├── report/
        │       ├── settings/
        │       └── stock/overview/
        └── res/
            ├── values/       (strings.xml, themes.xml)
            └── xml/          (network_security_config, backup_rules, data_extraction_rules)
```

---

## Phase Còn Lại

### Phase 3 (chưa làm)
- `ProductDetailScreen.kt`
- `AddEditProductScreen.kt`
- `BarcodeScanScreen.kt` (tìm theo barcode + tên + danh mục)

### Phase 4 (chưa làm)
- `StockInScreen.kt` + ViewModel
- `StockOutScreen.kt` + ViewModel
- `StockHistoryScreen.kt`
- `LowStockAlertScreen.kt`
- `SupplierListScreen.kt` + `AddEditSupplierScreen.kt`

### Phase 5 (chưa làm)
- `DashboardScreen` charts (weekly in/out)
- `ReportScreen` full implementation
- App icon, Lottie animations
- Unit tests

---

## Lấy SHA Fingerprint để Đăng Ký Firebase

```bash
# Debug SHA-1 & SHA-256
./gradlew signingReport

# Hoặc dùng keytool
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

→ Thêm SHA vào: Firebase Console > Project Settings > Your apps > Add fingerprint

# Tech Stack — WaveHouse Warehouse Management App

> **Phiên bản:** 1.0.0 | **Cập nhật:** 2026-04-17

## Documentation Maintenance
**Last Updated:** 2026-04-17  
**Document Version:** 1.0  
**Maintained By:** Development Team

---

## Tổng Quan

Ứng dụng **WaveHouse** là hệ thống quản lý kho hàng mobile-first, xây dựng trên nền tảng Android Kotlin với kiến trúc Clean Architecture + MVVM, tích hợp Firebase và REST API.

---

## Stack Chính

| Layer | Technology | Phiên bản | Lý do chọn |
|-------|-----------|----------|------------|
| **Language** | Kotlin | 2.0.x | Null-safety, Coroutines, DSL support |
| **UI** | Jetpack Compose | 1.7.x | Declarative UI, ít boilerplate, Material 3 |
| **Architecture** | MVVM + Clean Architecture | — | Separation of concerns, testability |
| **DI** | Hilt (Dagger-based) | 2.51.x | Android-first DI, compile-time safety |
| **Async** | Kotlin Coroutines + Flow | 1.8.x | Reactive stream, lifecycle-aware |
| **Networking** | Retrofit 2 + OkHttp 4 | 2.11.x / 4.12.x | Type-safe HTTP, interceptor support |
| **Cloud DB** | Firebase Firestore | BOM 33.x | Real-time sync, offline persistence |
| **Auth** | Firebase Authentication | BOM 33.x | Social + Email auth, token management |
| **Storage** | Firebase Storage | BOM 33.x | Image/file upload cho sản phẩm |
| **Local DB** | Room | 2.6.x | Offline-first, Firestore cache layer |
| **Navigation** | Navigation Compose | 2.7.x | Type-safe routes, single-activity |
| **Image** | Coil 3 | 3.x | Kotlin-first, Compose integration |
| **Barcode** | ML Kit Barcode Scanning | 17.3.x | Offline scanning, CameraX integration |
| **Camera** | CameraX | 1.4.x | Modern camera API |
| **Serialization** | Kotlinx Serialization + Gson | 1.7.x | JSON mapping |
| **Testing** | JUnit5 + MockK + Turbine | — | Unit test, Flow testing |

---

## Firebase Services Sử Dụng

```
Firebase BOM 33.x
├── firebase-auth-ktx          → Đăng nhập / phân quyền
├── firebase-firestore-ktx     → Database chính (kho hàng, sản phẩm, đơn hàng)
├── firebase-storage-ktx       → Ảnh sản phẩm, tài liệu
├── firebase-messaging-ktx     → Push notification (cảnh báo tồn kho thấp)
└── firebase-analytics-ktx     → Theo dõi hành vi người dùng
```

---

## Retrofit API Clients

```
RetrofitClient (Base)
├── WarehouseApiService        → REST CRUD sản phẩm, kho, nhà cung cấp
├── ReportApiService           → Báo cáo thống kê, xuất Excel/PDF
└── AuthApiService             → Token refresh, OAuth2
```

---

## Build Tools

| Tool | Phiên bản |
|------|----------|
| Android Gradle Plugin | 8.5.x |
| Gradle | 8.9.x |
| Kotlin Gradle Plugin | 2.0.x |
| Version Catalog (TOML) | ✅ |
| Build Config (secrets) | ✅ BuildConfig + local.properties |

---

## Feasibility Check

✅ **MVVM + Clean Architecture** — Phù hợp 100% cho domain phức tạp (kho hàng, đơn nhập xuất)
✅ **Hilt** — Standard Android DI, ổn định, IDE hỗ trợ tốt
✅ **Retrofit** — Cần thiết khi integrate với ERP hoặc backend tùy chỉnh
✅ **Firebase Firestore** — Real-time cập nhật tồn kho, offline support sẵn có
✅ **Room** — Offline cache cho môi trường kho hàng (WiFi không ổn định)
✅ **Barcode/QR Scan** — ML Kit offline, không cần internet untuk scan

---

## Rủi Ro & Giảm Thiểu

| Rủi Ro | Giải Pháp |
|--------|-----------|
| Firestore read cost khi list lớn | Pagination + local Room cache |
| Offline sync conflict | Firestore optimistic locking + timestamp |
| Barcode scan chậm | Background thread, không block UI |
| Security lộ API key | google-services.json + Firestore Rules |

# WaveHouse — Ứng Dụng Quản Lý Kho Hàng Android

> Android · Kotlin · Jetpack Compose · MVVM + Clean Architecture · Hilt · Retrofit · Firebase

## Documentation Maintenance
**Last Updated:** 2026-04-17  
**Document Version:** 1.0  
**Maintained By:** Development Team

---

## Giới Thiệu

**WaveHouse** là ứng dụng quản lý kho hàng dành cho Android, giúp doanh nghiệp vừa và nhỏ số hoá quy trình nhập/xuất kho, quản lý sản phẩm, nhà cung cấp và báo cáo thống kê theo thời gian thực.

### Tính Năng Chính
- 📦 Quản lý sản phẩm với barcode/QR scanning
- 📥📤 Nhập/Xuất kho + lịch sử giao dịch
- 🔔 Cảnh báo tồn kho thấp qua Push Notification
- 📊 Dashboard báo cáo thời gian thực
- 🔒 Phân quyền theo vai trò (Admin / Thủ kho / Kế toán)
- 📶 Offline-first: hoạt động khi mất mạng

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Networking | Retrofit 2 + OkHttp 4 |
| Cloud DB | Firebase Realtime Database |
| Auth | Firebase Authentication |
| Local DB | Room (offline cache) |
| Barcode | ML Kit + CameraX |
| Navigation | Navigation Compose |

---

## Cấu Trúc Dự Án

```
app/src/main/java/com/wavehouse/
├── core/           # Shared: DI modules, theme, navigation, utils
├── data/           # Remote (Firestore/Retrofit) + Local (Room)
├── domain/         # Models, Use Cases, Repository interfaces
└── presentation/   # Feature screens: auth, dashboard, product, stock, ...
```

---

## Cài Đặt & Chạy

### Yêu Cầu
- Android Studio Ladybug (2024.2.x) hoặc mới hơn
- Android SDK: minSdk 26 (Android 8.0), targetSdk 35
- JDK 17
- Firebase project đã cấu hình

### Setup
1. Clone repo:
   ```bash
   git clone <repo-url>
   cd wave_house
   ```
2. Đặt file `google-services.json` vào thư mục `app/`
3. Copy `local.properties.example` → `local.properties` và điền API keys
4. Build & run:
   ```bash
   ./gradlew assembleDebug
   ```

---

## Tài Liệu

| Tài Liệu | Đường Dẫn |
|---------|----------|
| Tech Stack | [docs/tech-stack.md](docs/tech-stack.md) |
| System Architecture | [docs/system-architecture.md](docs/system-architecture.md) |
| Product Requirements | [docs/project-overview-pdr.md](docs/project-overview-pdr.md) |
| Code Standards | [docs/code-standards.md](docs/code-standards.md) |
| Codebase Summary | [docs/codebase-summary.md](docs/codebase-summary.md) |
| Project Roadmap | [docs/project-roadmap.md](docs/project-roadmap.md) |
| Implementation Plan | [plans/2026-04-13-freshstock-pos/plan.md](plans/2026-04-13-freshstock-pos/plan.md) |

---

## Phân Quyền

| Role | Quyền |
|------|-------|
| Admin | CRUD sản phẩm, user, kho, báo cáo |
| Thủ Kho | Nhập/xuất kho, xem tồn kho, scan barcode |
| Kế Toán | Xem báo cáo, xuất dữ liệu |

---

## License

© 2026 WaveHouse. All rights reserved.

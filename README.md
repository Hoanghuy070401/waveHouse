# WaveHouse — Ứng Dụng Quản Lý Kho Hàng & Bán Hàng Android

> Android · Kotlin · Jetpack Compose · MVVM + Clean Architecture · Hilt · Firebase Realtime Database

## Documentation Maintenance
**Last Updated:** 2026-04-21
**Document Version:** 1.2
**Maintained By:** Development Team

---

## Giới Thiệu

**WaveHouse** là ứng dụng quản lý kho hàng và bán hàng (POS) dành cho Android, giúp doanh nghiệp vừa và nhỏ số hoá quy trình nhập/xuất kho, bán hàng tại quầy, quản lý sản phẩm, nhà cung cấp và báo cáo thống kê theo thời gian thực.

### Tính Năng Chính
- 📦 Quản lý sản phẩm với barcode/QR scanning (ML Kit)
- 📥📤 Nhập/Xuất kho + hao hụt + lịch sử giao dịch chi tiết
- 🛒 POS bán hàng nhanh: grid sản phẩm, giỏ hàng, thanh toán
- 📋 Lịch sử đơn hàng & chi tiết giao dịch
- 💰 Giá linh hoạt: cập nhật giá vốn/giá bán có lịch sử (Dynamic Pricing)
- 🔔 Cảnh báo tồn kho thấp qua Push Notification (FCM)
- 📊 Dashboard & báo cáo thời gian thực
- 🔒 Phân quyền theo vai trò: Admin / Thủ kho / Kế toán / Nhân viên
- ⏳ Offline-first: Room DB cache dữ liệu khi mất mạng

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Cloud DB | Firebase Realtime Database |
| Auth | Firebase Authentication |
| Storage | Firebase Storage (ảnh sản phẩm, QR) |
| Local DB | Room (offline cache) |
| Barcode | ML Kit + CameraX |
| Push | Firebase Cloud Messaging (FCM) |
| Navigation | Navigation Compose |

---

## Cấu Trúc Dự Án

```
app/src/main/java/com/wavehouse/
├── core/           # DI modules, theme, navigation, utils, FCM service
├── data/
│   ├── local/      # Room DB (Entities, DAOs, Converters)
│   └── remote/     # Firebase Repository implementations
├── domain/         # Models, Repository interfaces, Use Cases
└── presentation/   # Feature screens:
    ├── auth/       # login, register, forgot password, pending approval
    ├── dashboard/  # KPI overview
    ├── product/    # list, detail, add/edit, scanner
    ├── stock/      # stock-in, stock-out, shrinkage, history, low-stock
    ├── pos/        # POS screen (quick sell)
    ├── order/      # history, detail
    ├── supplier/   # list, add/edit
    ├── report/     # ReportScreen
    ├── staff/      # ManageStaffScreen (Admin only)
    └── settings/   # SettingsScreen
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
   git clone https://github.com/Hoanghuy070401/waveHouse.git
   cd wave_house
   ```
2. Đặt file `google-services.json` vào thư mục `app/`
3. Copy `local.properties.example` → `local.properties` và điền API keys
4. Build & run qua **Android Studio** (JDK chưa cấu hình trong shell)

---

## Phân Quyền

| Role | Quyền |
|------|-------|
| **Admin** | CRUD sản phẩm, kho, user, báo cáo, duyệt nhân viên |
| **Thủ Kho** | Nhập/xuất kho, xem tồn kho, scan barcode |
| **Kế Toán** | Xem báo cáo, lịch sử đơn hàng |
| **Nhân Viên** | Bán hàng POS, xem sản phẩm |

---

## Tài Liệu

| Tài Liệu | Đường Dẫn |
|---------|----------|
| System Architecture | [docs/system-architecture.md](docs/system-architecture.md) |
| Product Requirements (PDR) | [docs/project-overview-pdr.md](docs/project-overview-pdr.md) |
| Code Standards | [docs/code-standards.md](docs/code-standards.md) |
| Codebase Summary | [docs/codebase-summary.md](docs/codebase-summary.md) |
| Project Roadmap | [docs/project-roadmap.md](docs/project-roadmap.md) |
| Design Guidelines | [docs/design-guidelines.md](docs/design-guidelines.md) |
| Tech Stack | [docs/tech-stack.md](docs/tech-stack.md) |

---

## License

© 2026 WaveHouse. All rights reserved.

# Codebase Summary — WaveHouse
> **Phiên bản:** 1.0.0 | **Cập nhật:** 2026-04-17

## Documentation Maintenance
**Last Updated:** 2026-04-20  
**Document Version:** 1.1  
**Maintained By:** Development Team

## Tổng quan
Dự án WaveHouse đang được phát triển theo kiến trúc **Clean Architecture** kết hợp với pattern **MVVM**, sử dụng Jetpack Compose cho tầng UI.

## Các Module & Feature Chính (Tính đến hiện tại)
1. **Core / Shared Utilities** (`app/src/main/java/com/wavehouse/core/`)
   - **DI**: Cấu hình Hilt cho các modules (DatabaseModule, FirebaseModule, NetworkModule, RepositoryModule).
   - **Network**: Quản lý Network interceptor và parse kết quả với `ApiResult`.
   - **UI**: Thanh toán điều hướng (`AppNavHost`, `BottomNavBar`, `Routes`), design system (`Theme`, `Type`, `Color`).
   - **Service**: Tích hợp FCM (`WaveHouseFcmService`).
2. **Data Layer** (`app/src/main/java/com/wavehouse/data/`)
   - **Local DB (Room)**: Cấu hình `WaveHouseDatabase`, các `Converters`, `Daos`, và schema Entities.
   - **Remote (Firebase)**: Các lớp implement kết nối trực tiếp Realtime Database bao gồm `AuthRepositoryImpl`, `OrderRepositoryImpl`, `ProductRepositoryImpl`, `StockRepositoryImpl`, `SupplierRepositoryImpl`, `WarehouseRepositoryImpl`.
3. **Domain Layer** (`app/src/main/java/com/wavehouse/domain/`)
   - **Models**: Định nghĩa dữ liệu core của business logic.
   - **Repository Interfaces**: Định nghĩa contract interface.
   - **Use Cases**: Tổng hợp business logic như `AuthUseCases`, `PosUseCases`, `ProductUseCases`, `StockUseCases`.
4. **Presentation Layer** (`app/src/main/java/com/wavehouse/presentation/`)
   - **Auth**: Đăng nhập (`login`), đăng ký (`register`), quên mật khẩu (`forgotpassword`, `emailverification`), đổi mật khẩu (`changepassword`), màn hình chờ (`splash/PendingApprovalScreen`).
   - **Dashboard**: Màn hình tổng quan KPIs (`DashboardScreen`).
   - **Product**: Quản lý danh sách (`list`), chi tiết (`detail`), thêm/sửa (`addedit`), và tích hợp ML Kit quét mã vạch (`scanner`).
   - **Stock**: Nhập kho (`stockin`), xuất kho (`stockout`), lịch sử (`history`), cảnh báo tồn kho thấp (`lowstock`), hao hụt (`shrinkage`), và tổng quan (`overview`).
   - **POS**: Màn hình bán hàng (`PosScreen`).
   - **Order**: Quản lý đơn hàng (`history`, `detail`).
   - **Supplier**: Quản lý nhà cung cấp (`list`, `addedit`).
   - **Report**: Màn hình báo cáo (`ReportScreen`).
   - **Staff**: Quản lý phân quyền và phê duyệt (`ManageStaffScreen`).
   - **Settings**: Cài đặt ứng dụng (`SettingsScreen`).

## Thay đổi nổi bật gần đây
- Cấu trúc thư mục được tinh chỉnh vững chắc để làm cơ sở cho mọi feature.
- Migrate toàn bộ Remote Layer từ Firestore sang Firebase Realtime Database.
- Cài đặt triệt để Role-based Authentication flow (Login, Register, Pending Approval, Forgot Password) có sử dụng `database.rules.json` mới.
- Xây dựng màn hình `ManageStaffScreen` cho Admin phê duyệt tài khoản mới.
- Scale ra các UseCase và Repositories cho Order, Product, Warehouse đáp ứng kiến trúc.

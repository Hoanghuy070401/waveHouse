# Project Roadmap — WaveHouse
> **Phiên bản:** 1.0.0 | **Cập nhật:** 2026-04-17

## Documentation Maintenance
**Last Updated:** 2026-04-20  
**Document Version:** 1.1  
**Maintained By:** Development Team

## Phase 1: Authentication & Project Foundation (Đã hoàn thành)
- [x] Khởi tạo dự án Android & cấu hình theo chuẩn Clean Architecture (MVVM + Hilt).
- [x] Thiết lập Jetpack Compose Theme & Navigation Graph nền tảng.
- [x] Đăng nhập & Đăng ký sử dụng Firebase Auth.
- [x] Xử lý luồng Quên mật khẩu, Xác thực Email, và Deep Link thiết lập mật khẩu mới.
- [x] Áp dụng Role-based Access Control (RBAC) và luồng phê duyệt Admin.
- [x] Migrate Remote source sang Firebase Realtime Database.
- [x] Xây dựng các interface Repository và Use Case ban đầu.

## Phase 2: Màn Hình Chính & Sản Phẩm (Đã hoàn thành)
- [x] Hoàn thiện giao diện màn hình Dashboard.
- [x] Xây dựng danh sách Sản phẩm (tìm kiếm, phân trang).
- [x] Màn hình Thêm / Chỉnh sửa Sản phẩm, kèm tích hợp Firebase Storage (upload ảnh).
- [x] Tích hợp Barcode / QR Scanning bằng ML Kit.

## Phase 3: Quản Lý Tồn Kho & Bán Hàng (Đã hoàn thành)
- [x] Thiết lập luồng Nhập kho (Inbound) và Xuất kho (Outbound).
- [x] Tạo module Quét mã vạch tự động trừ tồn kho.
- [x] Đồng bộ hóa dữ liệu realtime với Realtime Database (Single Source of Truth).
- [x] Offline caching bằng Room Database.
- [x] Màn hình POS (Point of Sale) cơ bản.
- [x] Màn hình Quản lý đơn hàng (Order History, Order Detail).

## Phase 4: Quản Trị Hệ Thống & Tính Năng Mở Rộng (Đang triển khai)
- [x] Quản lý nhà cung cấp (Suppliers).
- [x] Tích hợp push notification cảnh báo tồn kho thấp (FCM Service).
- [x] Màn hình báo cáo (Report Screen).
- [x] Quản lý nhân viên (Manage Staff Screen).
- [x] Màn hình Cài đặt (Settings).
- [ ] Tinh chỉnh hiệu năng ứng dụng, rà soát lại rule bảo mật của Realtime Database.
- [ ] Tích hợp dynamic pricing và lịch sử giá sản phẩm.
- [ ] Báo cáo xuất Excel/PDF.

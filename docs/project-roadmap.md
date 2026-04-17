# Project Roadmap — WaveHouse
> **Phiên bản:** 1.0.0 | **Cập nhật:** 2026-04-17

## Documentation Maintenance
**Last Updated:** 2026-04-17  
**Document Version:** 1.0  
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

## Phase 3: Quản Lý Tồn Kho & Bán Hàng (Đang triển khai)
- [x] Thiết lập luồng Nhập kho (Inbound) và Xuất kho (Outbound).
- [x] Tạo module Quét mã vạch tự động trừ tồn kho.
- [x] Đồng bộ hóa dữ liệu realtime với Realtime Database (Single Source of Truth).
- [x] Offline caching bằng Room Database.
- [ ] Tính năng POS (Point of sale) - Đang nâng cấp UI/UX.

## Phase 4: Quản Trị Hệ Thống & Tính Năng Mở Rộng
- [x] Quản lý nhà cung cấp (Suppliers).
- [x] Tích hợp push notification cảnh báo tồn kho thấp.
- [x] Chức năng trích xuất / xuất báo cáo căn bản.
- [x] Quản lý nhân viên (Staff/Role management).
- [ ] Tinh chỉnh hiệu năng ứng dụng, rà soát lại rule bảo mật của Realtime Database.

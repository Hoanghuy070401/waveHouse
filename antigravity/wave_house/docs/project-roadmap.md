# Project Roadmap — WaveHouse
> **Phiên bản:** 1.0.0 | **Cập nhật:** 2026-04-15

## Phase 1: Authentication & Project Foundation (Đã hoàn thành)
- [x] Khởi tạo dự án Android & cấu hình theo chuẩn Clean Architecture (MVVM + Hilt).
- [x] Thiết lập Jetpack Compose Theme & Navigation Graph nền tảng.
- [x] Đăng nhập & Đăng ký sử dụng Firebase Auth.
- [x] Xử lý luồng Quên mật khẩu, Xác thực Email, và Deep Link thiết lập mật khẩu mới.
- [x] Xây dựng các interface Repository và Use Case ban đầu.

## Phase 2: Màn Hình Chính & Sản Phẩm (Đang triển khai)
- [ ] Hoàn thiện giao diện màn hình Dashboard.
- [ ] Xây dựng danh sách Sản phẩm (tìm kiếm, phân trang).
- [ ] Màn hình Thêm / Chỉnh sửa Sản phẩm, kèm tích hợp Firebase Storage (upload ảnh).
- [ ] Tích hợp Barcode / QR Scanning bằng ML Kit.

## Phase 3: Quản Lý Tồn Kho & Bán Hàng
- [ ] Thiết lập luồng Nhập kho (Inbound) và Xuất kho (Outbound).
- [ ] Tạo module Quét mã vạch tự động trừ tồn kho.
- [ ] Đồng bộ hóa dữ liệu realtime với Firestore (Single Source of Truth).
- [ ] Offline caching bằng Room Database.

## Phase 4: Quản Trị Hệ Thống & Tính Năng Mở Rộng
- [ ] Quản lý nhà cung cấp (Suppliers).
- [ ] Tích hợp push notification cảnh báo tồn kho thấp.
- [ ] Chức năng trích xuất / xuất báo cáo căn bản.
- [ ] Tinh chỉnh hiệu năng ứng dụng, rà soát lại rule bảo mật của Firestore.

# Project Roadmap — WaveHouse
> **Phiên bản:** 1.2.0 | **Cập nhật:** 2026-04-21

## Documentation Maintenance
**Last Updated:** 2026-04-21
**Document Version:** 1.2
**Maintained By:** Development Team

---

## Phase 1: Authentication & Project Foundation ✅ Hoàn thành
- [x] Khởi tạo dự án Android & cấu hình Clean Architecture (MVVM + Hilt)
- [x] Jetpack Compose Theme & Navigation Graph
- [x] Đăng nhập & Đăng ký (Firebase Auth)
- [x] Luồng Quên mật khẩu, Email verification, Deep Link đặt lại mật khẩu
- [x] Role-based Access Control (RBAC): Admin / Warehouse / Accountant / Staff
- [x] Màn hình Pending Approval — Admin duyệt tài khoản mới
- [x] Migrate Remote source → Firebase Realtime Database
- [x] Repository interfaces & Use Cases ban đầu

---

## Phase 2: Màn Hình Chính & Sản Phẩm ✅ Hoàn thành
- [x] Dashboard tổng quan KPIs (doanh thu, nhập/xuất hôm nay, tồn kho)
- [x] Danh sách Sản phẩm (tìm kiếm realtime, phân trang)
- [x] Thêm/Sửa/Xóa Sản phẩm + upload ảnh Firebase Storage
- [x] Barcode / QR Scanning (ML Kit)
- [x] Màn hình chi tiết sản phẩm với lịch sử giá (`PriceRecord`)

---

## Phase 3: Quản Lý Tồn Kho & Bán Hàng ✅ Hoàn thành
- [x] Nhập kho (In-bound) với tự động tính giá vốn MAC
- [x] Xuất kho (Out-bound) + Điều chỉnh tồn kho + Ghi nhận hao hụt (Shrinkage)
- [x] Lịch sử nhập/xuất theo sản phẩm & theo kho
- [x] Cảnh báo tồn kho thấp (Low Stock Alert)
- [x] POS Screen cơ bản: giỏ hàng, thanh toán
- [x] Lịch sử đơn hàng (`OrderHistoryScreen`) + Chi tiết đơn (`OrderDetailScreen`)
- [x] Hỗ trợ bán lẻ số thập phân (`allowDecimal` flag)
- [x] POS grid sản phẩm cuộn ngang 2 hàng, checkout bar compact

---

## Phase 4: Quản Trị & Tính Năng Mở Rộng 🔄 Đang triển khai
- [x] Quản lý nhà cung cấp (Suppliers CRUD)
- [x] Push notification tồn kho thấp (FCM)
- [x] Màn hình Báo cáo (Report Screen)
- [x] Quản lý nhân viên & phân quyền (ManageStaffScreen)
- [x] Settings: đổi mật khẩu, quản lý QR thanh toán
- [x] Dynamic Pricing: lịch sử giá, cập nhật giá vốn/giá bán có ghi chú
- [ ] Tinh chỉnh hiệu năng & rà soát Security Rules Realtime Database
- [ ] Báo cáo xuất Excel/PDF
- [ ] Top sản phẩm bán chạy (doanh thu theo sản phẩm)

---

## Phase 5: Scale & Polish 📋 Kế hoạch
- [ ] Cursor-based pagination cho Order History (thay limitToLast 300)
- [ ] Multi-warehouse switching trong app
- [ ] Lịch sử giao dịch theo nhà cung cấp
- [ ] Offline mode hoàn toàn (local Room DB + sync queue)
- [ ] In nhãn barcode (Bluetooth printer)
- [ ] Dashboard web admin (tách riêng)

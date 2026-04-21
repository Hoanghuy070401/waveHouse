# Product Development Requirements — WaveHouse

> **Phiên bản:** 1.2.0 | **Cập nhật:** 2026-04-21 | **Nền tảng:** Android (Kotlin)

## Documentation Maintenance
**Last Updated:** 2026-04-21
**Document Version:** 1.2
**Maintained By:** Development Team

---

## 1. Tổng Quan Sản Phẩm

**WaveHouse** là ứng dụng quản lý kho hàng và bán hàng (POS) dành cho thiết bị Android, giúp doanh nghiệp vừa và nhỏ quản lý:
- Danh mục sản phẩm & tồn kho realtime
- Nhập / Xuất kho với lịch sử chi tiết
- Bán hàng nhanh qua POS với hỗ trợ giá linh hoạt và bán lẻ theo số thập phân
- Lịch sử đơn hàng & chi tiết giao dịch
- Báo cáo & thống kê theo ngày/tuần/tháng

---

## 2. Người Dùng Mục Tiêu

| Vai Trò | Quyền Hạn |
|---------|----------|
| **Admin** | Toàn quyền: CRUD sản phẩm, kho, user, báo cáo, duyệt nhân viên |
| **Thủ Kho** (`WAREHOUSE`) | Nhập/xuất kho, xem tồn kho, scan barcode |
| **Kế Toán** (`ACCOUNTANT`) | Xem báo cáo, xem lịch sử đơn hàng |
| **Nhân Viên** (`STAFF`) | Bán hàng qua POS, xem sản phẩm |

---

## 3. Tính Năng Cốt Lõi (MVP)

### 3.1 Xác Thực & Phân Quyền
- [x] Đăng nhập Email/Password (Firebase Auth)
- [x] Đăng ký → chờ Admin phê duyệt (`PENDING` state)
- [x] Phân quyền theo Role (Admin / Warehouse / Accountant / Staff)
- [x] Màn hình Pending Approval cho tài khoản mới
- [x] Quên mật khẩu (email verification + deep link reset)
- [x] Đổi mật khẩu trong app

### 3.2 Quản Lý Sản Phẩm
- [x] Danh sách sản phẩm (tìm kiếm realtime, lọc theo danh mục)
- [x] Thêm/Sửa/Xoá sản phẩm
- [x] Upload ảnh sản phẩm (Firebase Storage)
- [x] Barcode/QR scanning (ML Kit)
- [x] Giá vốn (MAC — tự động cập nhật khi nhập kho)
- [x] Lịch sử giá sản phẩm (`PriceRecord`)
- [x] Cờ `allowDecimal` — cho phép/chặn bán lẻ số thập phân
- [ ] Danh mục sản phẩm phân cấp (Category tree)
- [ ] Quản lý đơn vị tính (Unit of Measure)

### 3.3 Quản Lý Kho
- [x] Xem tồn kho theo kho hàng (`warehouseId`)
- [x] Nhập kho (In-bound + tính giá MAC tự động)
- [x] Xuất kho (Out-bound)
- [x] Điều chỉnh tồn kho (Inventory Adjustment)
- [x] Ghi nhận hao hụt (Shrinkage) với lý do phân loại
- [x] Cảnh báo tồn kho dưới ngưỡng (Low Stock Alert)
- [x] Lịch sử nhập/xuất chi tiết theo sản phẩm
- [ ] Chuyển kho (Stock Transfer giữa các kho)

### 3.4 Bán Hàng (POS)
- [x] POS Screen: thêm sản phẩm nhanh vào giỏ hàng
- [x] Grid sản phẩm cuộn ngang 2 hàng
- [x] Hỗ trợ nhập số lượng thập phân (bán lẻ kg, bó...)
- [x] Nút +/- và nhập trực tiếp số lượng với validation
- [x] Tổng thanh toán cập nhật realtime khi thay đổi số lượng
- [x] Thanh checkout compact (1 hàng: số tiền + nút thanh toán)
- [x] Lịch sử bán hàng (`OrderHistoryScreen`) với lọc ngày
- [x] Chi tiết đơn hàng (`OrderDetailScreen`)
- [x] Shortcut điều hướng sang lịch sử từ POS top bar
- [ ] In hóa đơn Bluetooth

### 3.5 Nhà Cung Cấp
- [x] CRUD nhà cung cấp
- [ ] Lịch sử giao dịch theo nhà cung cấp

### 3.6 Báo Cáo & Thống Kê
- [x] Dashboard tổng quan (KPIs: doanh thu hôm nay, tồn kho, nhập/xuất)
- [x] Báo cáo nhập xuất theo ngày/tuần/tháng
- [ ] Top sản phẩm bán chạy
- [ ] Xuất báo cáo PDF/Excel

### 3.7 Quản Trị Hệ Thống
- [x] Quản lý nhân viên (ManageStaffScreen — Admin duyệt/từ chối)
- [x] Push notification cảnh báo tồn kho thấp (FCM)
- [x] Settings: đổi mật khẩu, QR thanh toán, thông tin kho
- [x] QR thanh toán upload/xóa theo kho

---

## 4. Tính Năng Tương Lai (Post-MVP)

- [ ] Multi-warehouse switching trong app
- [ ] Offline mode hoàn toàn (sync queue khi có mạng)
- [ ] In nhãn barcode (Bluetooth printer)
- [ ] Lịch sử kiểm kê (Stock Count)
- [ ] Tích hợp ERP (SAP, Odoo) qua REST API
- [ ] Dashboard web admin
- [ ] Cursor-based pagination cho Order History (thay client-side aggregation)
- [ ] Báo cáo xuất Excel/PDF

---

## 5. Yêu Cầu Kỹ Thuật

### 5.1 Hiệu Năng
- Màn hình danh sách tải < 2 giây
- Barcode scan phản hồi < 500ms
- POS: cập nhật tổng tiền realtime (< 16ms per frame)

### 5.2 Bảo Mật
- Firebase Realtime Database Security Rules theo `database.rules.json`
- Chỉ đọc/ghi dữ liệu theo `warehouseId` đã được phân quyền
- Không lưu plaintext credentials; Firebase Auth token quản lý session

### 5.3 Khả Năng Mở Rộng
- Repository pattern: dễ swap Firebase → REST API nếu cần
- Multi-tenant: mọi dữ liệu đều scope theo `warehouseId`

---

## 6. UI/UX Requirements

- Material 3 (M3) design system, theme "Organic Ledger" (green palette)
- Bottom Navigation: Dashboard / Sản phẩm / Kho / Bán hàng / Báo cáo
- Form validation realtime
- Loading states & error states rõ ràng
- Empty state screens
- Compact checkout bar trong POS (không dùng full-width panel)

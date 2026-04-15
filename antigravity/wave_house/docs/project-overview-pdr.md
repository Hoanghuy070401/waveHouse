# Product Development Requirements — WaveHouse

> **Phiên bản:** 1.0.0 | **Cập nhật:** 2026-04-13 | **Nền tảng:** Android (Kotlin)

---

## 1. Tổng Quan Sản Phẩm

**WaveHouse** là ứng dụng quản lý kho hàng dành cho thiết bị Android, giúp doanh nghiệp vừa và nhỏ quản lý:
- Danh mục sản phẩm & tồn kho
- Nhập / Xuất kho
- Đơn đặt hàng từ nhà cung cấp
- Báo cáo & thống kê

---

## 2. Người Dùng Mục Tiêu

| Vai Trò | Quyền Hạn |
|---------|----------|
| **Admin** | Toàn quyền: CRUD sản phẩm, kho, user, báo cáo |
| **Thủ Kho** | Nhập/xuất kho, xem tồn kho, scan barcode |
| **Kế Toán** | Xem báo cáo, xuất dữ liệu |
| **Nhân Viên** | Xem sản phẩm, tạo phiếu yêu cầu |

---

## 3. Tính Năng Cốt Lõi (MVP)

### 3.1 Xác Thực & Phân Quyền
- [x] Đăng nhập Email/Password (Firebase Auth)
- [ ] Phân quyền theo Role (Admin/Thủ kho/Kế toán)
- [x] Đổi mật khẩu, quên mật khẩu

### 3.2 Quản Lý Sản Phẩm
- [ ] Danh sách sản phẩm (phân trang, tìm kiếm, lọc)
- [ ] Thêm/Sửa/Xoá sản phẩm
- [ ] Upload ảnh sản phẩm (Firebase Storage)
- [ ] Barcode/QR scanning (ML Kit)
- [ ] Danh mục sản phẩm (Category)
- [ ] Quản lý đơn vị tính (Unit of Measure)

### 3.3 Quản Lý Kho
- [ ] Xem tồn kho theo vị trí kho (Location)
- [ ] Nhập kho (Purchase Order)
- [ ] Xuất kho (Sales Order / Transfer)
- [ ] Điều chỉnh tồn kho (Inventory Adjustment)
- [ ] Cảnh báo tồn kho dưới ngưỡng (Low Stock Alert)
- [ ] Chuyển kho (Stock Transfer)

### 3.4 Nhà Cung Cấp
- [ ] CRUD nhà cung cấp
- [ ] Lịch sử giao dịch theo nhà cung cấp

### 3.5 Báo Cáo & Thống Kê
- [ ] Dashboard tổng quan (tổng tồn kho, nhập/xuất hôm nay)
- [ ] Báo cáo nhập xuất theo ngày/tuần/tháng
- [ ] Top sản phẩm nhập/xuất nhiều nhất
- [ ] Xuất báo cáo PDF/Excel (tương lai)

### 3.6 Thông Báo
- [ ] Push notification khi tồn kho thấp (FCM)
- [ ] Thông báo khi có đơn hàng mới

---

## 4. Tính Năng Tương Lai (Post-MVP)

- [ ] Tích hợp ERP (SAP, Odoo) qua REST API
- [ ] Multi-warehouse support
- [ ] In nhãn barcode (Bluetooth printer)
- [ ] Offline mode hoàn toàn với sync queue
- [ ] Lịch sử kiểm kê (Stock Count)
- [ ] Dashboard web admin

---

## 5. Yêu Cầu Kỹ Thuật

### 5.1 Hiệu Năng
- Màn hình danh sách tải < 2 giây
- Barcode scan phản hồi < 500ms
- Offline mode: app hoạt động đầy đủ khi mất mạng

### 5.2 Bảo Mật
- Tất cả API calls phải có Bearer token
- Firestore Security Rules: chỉ cho phép đọc/ghi theo role
- Không lưu plaintext credentials

### 5.3 Khả Năng Mở Rộng
- Firestore structure hỗ trợ multi-tenant (nhiều công ty)
- Repository pattern: dễ swap Firebase → SQL nếu cần

---

## 6. UI/UX Requirements

- Material 3 design system
- Dark mode support
- Bottom Navigation: Dashboard / Sản phẩm / Kho / Báo cáo / Cài đặt
- Form validation real-time
- Loading states & error states rõ ràng
- Empty state screens

---

## 7. Acceptance Criteria (MVP)

| Feature | Criterion |
|---------|-----------|
| Login | Đăng nhập thành công & redirect đúng role |
| Product List | Hiển thị paginated list, search hoạt động |
| Barcode Scan | Scan QR/barcode → tìm sản phẩm đúng |
| Stock In | Tạo phiếu nhập → cập nhật tồn kho realtime |
| Stock Out | Tạo phiếu xuất → trừ tồn kho, cảnh báo nếu không đủ |
| Dashboard | Hiển thị đúng KPIs từ Firestore |
| Offline | App hiển thị cached data khi mất mạng |

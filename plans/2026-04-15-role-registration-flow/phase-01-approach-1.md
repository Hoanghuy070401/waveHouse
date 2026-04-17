# Cách 1: Luồng Đăng ký Đa phân nhánh (Join vs Create)

## Mô tả
Thêm UI vào RegisterScreen để hỏi người dùng xem mục đích của họ là gì. Tuỳ theo mục đích sẽ quyết định Role và Warehouse ID.

## Chi tiết kỹ thuật

### Bước 1: UI RegisterScreen Update
Thêm một thành phần UI dạng Toggle/Tab ở trên cùng của Form:
- Chọn 1: **"Tôi là Chủ cửa hàng"** (Tạo hệ thống kho mới).
- Chọn 2: **"Tôi là Nhân viên"** (Tham gia kho hiện có).

**Nếu chọn "Tôi là Chủ cửa hàng":**
- Default Role = `ADMIN`.
- Form yêu cầu điền thêm: **Tên cửa hàng/kho**.
- Hệ thống sẽ tự random một `warehouseId` mới, tạo vào collection `warehouses` và gắn `warehouseId` này cho Admin.

**Nếu chọn "Tôi là Nhân viên":**
- Default Role = `STAFF` (Họ không được phép chọn chức năng cao cấp như Thủ Kho hay Kế Toán trên form, để tránh gian lận. Admin sẽ phê duyệt sau trong màn ManageStaff).
- Form yêu cầu điền thêm: **Mã kho hàng (Warehouse ID / Invite Code)** do Admin cung cấp.
- Hệ thống check ID kho xác lệ, gắn user vào kho này dưới quyền STAFF.

### Ưu điểm
- Giải quyết hoàn toàn vấn đề Owner và Staff.
- Owner có thể tự bắt đầu sử dụng app ngay lập tức (Self-serve).
- Không cần backend custom (Firebase Functions).

### Nhược điểm
- Form RegisterScreen bị dài ra.
- Owner phải cung cấp `warehouseId` cho nhân viên khá thủ công.

### File cần điều chỉnh
- `RegisterScreen.kt` (Thêm Toggle, TextFields mới).
- `RegisterViewModel.kt` (State cho Toggle, validate Mã kho/Tên kho).
- `AuthRepositoryImpl.kt` (Tạo object Warehouse, cập nhật logic đăng ký tuỳ role).

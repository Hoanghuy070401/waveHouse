# Kế hoạch: Xử lý định dạng Vai trò trong lúc Đăng ký (RBAC Registration Flow)

## Hiện trạng
Hiện tại, màn hình `RegisterScreen` đang gán cứng (hardcode) tất cả người dùng mới đăng ký thành `UserRole.STAFF` và `warehouseId = ""`.
Do ứng dụng quản lý kho (B2B/SaaS) yêu cầu dữ liệu phải gắn với 1 kho hàng (Cửa hàng) cụ thể, việc tự động gán `STAFF` mà không có kho dẫn đến việc user mới không thể làm gì. User cũng không có cách nào tự trở thành `ADMIN` để setup dữ liệu.

## Phân tích vấn đề
Có 2 nhu cầu đăng ký chính:
1. **Chủ cửa hàng mới (Owner):** Muốn dùng app, tải app về, tạo tài khoản. Họ **phải** là `ADMIN` và hệ thống cần tạo mới cho họ 1 `warehouseId`.
2. **Nhân viên (Staff):** Tải app về, tạo tài khoản. Họ **phải** được liên kết vào kho (`warehouseId`) của Chủ cửa hàng ở trên và được cấp đúng role (Thủ kho, Kế toán, Nhân viên).

## Đề xuất 2 Hướng tiếp cận (Approaches)

### Cách 1: Luồng Đăng ký Đa phân nhánh (Self-Serve Tenant) - Khuyên dùng 🏆
- **Ý tưởng:** Trên màn hình đăng ký, user chọn 1 trong 2 tuỳ chọn: "Tạo kho mới (Là quản lý)" hoặc "Tham gia kho hiện có (Là nhân viên)".
- Mở rộng Form đăng ký để xử lý cả 2 luồng. Admin có quyền gửi Mã giới thiệu cho nhân viên.

### Cách 2: Owner Đăng ký mở, Nhân viên từ tính năng Mời (Invite Only)
- **Ý tưởng:** Màn hình Đăng ký chỉ dành cho Chủ cửa hàng (Mặc định tạo ra ADMIN & Tạo kho). Trở thành Nhân viên KHÔNG đăng ký qua màn hình này, mà Admin sẽ trong app nhập Email nhân viên → Hệ thống tạo sẵn tài khoản và gửi link đổi mật khẩu qua mail.
- Giảm thiểu việc nhân viên đăng ký nhầm, nhưng logic phức tạp hơn và cần Firebase Functions hoặc Node.js server để tự động tạo Auth account.

*Vui lòng xem chi tiết từng cách trong các file `phase-` để lựa chọn.*

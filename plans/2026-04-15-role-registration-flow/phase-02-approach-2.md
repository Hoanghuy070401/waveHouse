# Cách 2: Mời nhân viên không qua màn đăng ký chung (Invite Only)

## Mô tả
Màn hình Đăng ký app mặc định chỉ để dành cho việc **Tạo Kho Mới (Tạo Chủ cửa hàng/Admin)**. 
Nhân viên sẽ được tạo từ màn hình Quản lý nhân viên bằng Admin.

## Chi tiết kỹ thuật

### Bước 1: Owner Registration Flow
- Sửa mặc định `RegisterScreen`: Khi đăng ký mặc định là `ADMIN`, yêu cầu nhập tên Kho -> tạo Warehouse cho họ. (Tương tự Admin side của Cách 1).

### Bước 2: Admin Invites Staff
- Ở màn hình `ManageStaffScreen`, thay vì nhân viên đã có sẵn account, Admin bấm nút "Thêm nhân viên mới".
- Admin sẽ tự chọn Role (STAFF, WAREHOUSE, ACCOUNTANT), nhập Email và Mật khẩu tạm thiết lập sẵn, sau đó bấm Lưu.
- Firebase Auth hỗ trợ Secondary App Instance (hoặc tốt nhất là Cloud Functions Firebase Admin SDK) để tạo Account bằng Email và Password tạm mà không làm văng phiên đăng nhập hiện tại của Admin.
- Account nhân viên được ghi thẳng vào bảng Users với Role được chỉ định và `warehouseId` của Admin đó.
- Gửi email yêu cầu nhân viên đổi pass khi vừa login lần đầu.

### Ưu điểm
- Tính bảo mật cực cao, triệt tiêu mọi khả năng nhân viên lạ "ngẫu nhiên" đăng ký và điền bừa warehouse ID của hệ thống.
- Nhân viên vào app là có quyền đúng chuẩn luôn (do Admin tự tạo), không cần phải pending chờ Admin đi duyệt.

### Nhược điểm
- Đòi hỏi phải sử dụng **Firebase Cloud Functions (Node.js)** để Admin SDK tạo account Auth. Firebase client SDK trên Android không cho phép tạo 2 account song song (nếu gọi `createUserWithEmailAndPassword`, session cũ sẽ bị out).
- Tốn nhiều thời gian thiết lập hạ tầng Cloud hơn so với Cách 1 thuần Frontend.

## Kết luận
Dựa theo kiến trúc Client-heavy thuần tuý của dự án hiện nay, **Cách 1 là phương án thực tế và nhanh chóng hơn**. Nếu được đồng ý, chúng ta sẽ bắt đầu thực thi Cách 1 (Luồng Join/Create trong Register).

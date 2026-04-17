# Phase 01: Yêu cầu khôi phục mật khẩu (UI & Logic)

**Context Links:** [plan.md](./plan.md)
**Priority:** High
**Status:** Not Started

## 1. Overview
Thiết kế và lập trình hai màn hình: `ForgotPasswordScreen` (nhập email) và `ForgotPasswordSuccessScreen` (thông báo gửi email thành công). Cần xử lý UX bao gồm validate email form, trạng thái Loading, bảo mật (che masked email), launch Gmail app từ Android (Intent) và bộ đếm ngược 30 giây (CountdownTimer).

## 2. Requirements & Architecture
- **Màn hình `ForgotPasswordScreen`:** 1 Header navigation quay lại, 1 Text Field nhập Email (validate Regex), 1 Button gửi Action.
- **Màn hình `ForgotPasswordSuccessScreen`:** Nhận tham số email để che đi (ví dụ `a****@gmail.com`). Nút "Mở Gmail" dùng `Intent.CATEGORY_APP_EMAIL` (hoặc `Intent.ACTION_MAIN` với gói mail). Nút "Gửi lại" bị disable và đếm lùi 30 giây bằng coroutine delay.
- **`ForgotPasswordState`:** Chứa trường `email`, trạng thái `isLoading`, `error`, `countdown`.
- **Firebase call:** `ActionCodeSettings` được cấu hình để gửi link Deep Link về lại App (Cần chuẩn bị URL). Phương thức `sendPasswordResetEmail(email, actionCodeSettings)`.

## 3. Related Code Files
- `app/src/main/java/com/wavehouse/presentation/auth/forgotpassword/ForgotPasswordScreen.kt`
- `app/src/main/java/com/wavehouse/presentation/auth/forgotpassword/ForgotPasswordSuccessScreen.kt`
- `app/src/main/java/com/wavehouse/presentation/auth/forgotpassword/ForgotPasswordViewModel.kt`
- `app/src/main/java/com/wavehouse/core/ui/navigation/Routes.kt`
- `app/src/main/java/com/wavehouse/core/ui/navigation/AppNavHost.kt`

## 4. Implementation Steps
1. **Define Routes:** Thêm route `ForgotPassword` và `ForgotPasswordSuccess/{email}` vào `Routes.kt`.
2. **ViewModel:** Xây dựng `ForgotPasswordViewModel` quản lý email input, validate, call Firebase `sendPasswordResetEmail` và hàm đếm ngược 30 giây.
3. **Screen UI 1:** Tạo `ForgotPasswordScreen` với màu xanh chủ đạo. Bắt lỗi validate chưa hợp lệ -> disable button. Không lộ thông tin email không tồn tại.
4. **Screen UI 2:** Tạo `ForgotPasswordSuccessScreen` nhận email. Implement hàm maskEmail (lấy ký tự đầu và che domain trước @).
5. **Intent Integration:** Thêm nút mở hòm thư bằng Launch Intent (`com.google.android.gm` hoặc fallback `Intent.CATEGORY_APP_EMAIL`).

## 5. Trạng thái lỗi (Error States)
- Lỗi kết nối / Firebase error: "Không thể gửi email. Vui lòng thử lại" hiển thị bằng Snackbar.
- Thông báo "Kiểm tra cả Spam" được đặt làm ghi chú cố định trong UI.

## 6. Next steps
Sang Phase 2 để xử lý phía bên kia của Deep link khi user mở từ email.

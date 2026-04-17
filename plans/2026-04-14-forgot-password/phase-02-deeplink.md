# Phase 02: Xử lý Deep Link và Đặt mật khẩu mới

**Context Links:** [plan.md](./plan.md)
**Priority:** High
**Status:** Not Started

## 1. Overview
Sau khi gửi link email (chứa `ActionCodeSettings` của Firebase auth), người dùng click vào link trong Gmail sẽ điều hướng về lại App. App cần bắt được deep link này (parse lấy `oobCode` của Firebase), hiển thị màn hình NewPasswordScreen và xác nhận cập nhật mật khẩu mới lên Firebase.

## 2. Requirements & Architecture
- **Màn hình `NewPasswordScreen`:** Chứa 2 trường mật khẩu mới và xác nhận mật khẩu mới. Nút xác nhận.
- **Deep Link Navigation:** Config trong `AndroidManifest.xml` một `intent-filter` cho `MainActivity` để bắt Host. Trong Compose `AppNavHost`, thêm `navArgument` và `deepLinks` vào màn `NewPasswordScreen`.
- **Firebase call:** `confirmPasswordReset(oobCode, newPassword)`.

## 3. Related Code Files
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/wavehouse/presentation/auth/forgotpassword/NewPasswordScreen.kt`
- `app/src/main/java/com/wavehouse/presentation/auth/forgotpassword/NewPasswordViewModel.kt`
- `app/src/main/java/com/wavehouse/core/ui/navigation/AppNavHost.kt`
- `app/src/main/java/com/wavehouse/core/ui/navigation/Routes.kt`

## 4. Implementation Steps
1. **Config Action URL:** Update hàm `sendPasswordResetEmail` tại Phase 01: tạo `ActionCodeSettings` chỉ định `url = "https://wavehouse.app/reset"` và `androidPackageName`.
2. **Intent Filter:** Cấu hình `AndroidManifest.xml` thêm `intent-filter` `android:autoVerify="true"` cho scheme `https`, host `wavehouse.app` pathPrefix `/reset`.
3. **NavHost setup:** Tại `AppNavHost`, route `NewPassword` khai báo chuỗi `deepLinks` của Navigation Compose để lấy `oobCode`. Mặc định link của Firebase sẽ gửi về dưới dạng `https://[URL]/?mode=resetPassword&oobCode=...`.
4. **New Password UI:** Giao diện với 2 input mật khẩu (ẩn/hiện tuỳ chọn). Validate ≥ 6 ký tự và 2 field phải giống hệt nhau.
5. **Update Logic:** `confirmPasswordReset` call. Nếu mã `oobCode` hết hạn/sai, show Snackbar lỗi: "Link đã hết hạn. Vui lòng gửi lại". Ngược lại, thông báo thành công và Pop back Stack về Login.

## 5. Rủi ro (Risk Assessment)
- Link điều hướng web của Firebase có thể bị Browser nuốt nếu App Link không autoVerify hoặc cài đặt scheme sai logic. (Có thể dùng Scheme riêng như `wavehouse://auth/reset` nhưng cần cài đặt Firebase Custom Auth handler. Giải pháp đơn giản là dùng scheme app riêng hoặc custom Firebase Hosting).
- Cần tuân thủ document firebase custom handler nếu dùng deeplink native triệt để.

## 6. Success Criteria
- Test bằng cách click link Firebase trong mail trên thiết bị ảo -> app mở thẳng vào `NewPasswordScreen`.
- Thay đổi mk thành công, đăng nhập được bằng mật khẩu mới.

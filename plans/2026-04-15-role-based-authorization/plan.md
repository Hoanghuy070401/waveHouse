# Kế hoạch Triển khai: Phân quyền theo Role (RBAC)

## Tổng quan
Kế hoạch này trình bày 2 phương pháp để triển khai hệ thống phân quyền (RBAC) dựa trên Firebase cho ứng dụng WaveHouse, sử dụng enum `UserRole` (`ADMIN`, `WAREHOUSE`, `ACCOUNTANT`, `STAFF`). 
Hiện tại, logic giới hạn giao diện ở phía client đã được cài đặt một phần thông qua các getter trong model `User`, nhưng bảo mật phía server (Firestore Rules) cần được thực thi nghiêm ngặt.

## Phương án 1: RBAC dựa trên Firestore Document (Đề xuất)
Lưu trữ và xác thực quyền của người dùng trực tiếp qua Firestore Security Rules bằng cách tra cứu (`get()`) trực tiếp tài liệu `/users/{uid}`.
**Ưu điểm**: Dễ dàng cài đặt, role cập nhật theo thời gian thực (real-time), không cần thêm hạ tầng backend riêng.
**Nhược điểm**: Phát sinh một chi phí cực kỳ nhỏ cho phần lượt đọc (read cost) và thời gian xử lí của luồng kiểm tra rule.

<!-- ## Phương án 2: RBAC Custom Claims
Sử dụng Firebase Auth Custom Claims, được quản lý và cập nhật thông qua Firebase Cloud Functions.
**Ưu điểm**: Hiệu suất đọc database cực tốt (role được lưu trong cache ID token), độ bảo mật cao.
**Nhược điểm**: Luồng cấp mới role và thực thi bị trễ thời gian, ứng dụng client cần tự refresh token phức tạp, bắt buộc phải setup môi trường Cloud Functions. -->

## Khuyến nghị
**Phương án 1** được khuyến nghị dành cho giai đoạn hiện tại này. Nó hoàn toàn thoả mãn quy mô ứng dụng vừa và nhỏ mà không tạo gánh nặng duy trì Cloud Functions. Phí phát sinh read db là chuyện nhỏ so với thời gian phát triển nó mang lại.

## Trạng thái triển khai: ✅ HOÀN THÀNH (2026-04-15)

### Những gì đã được thực hiện:
| Hạng mục | File | Trạng thái |
|----------|------|-----------|
| Firebase Realtime DB security rules | `database.rules.json` | ✅ |
| Contract repository mới | `AuthRepository.kt` | ✅ |
| Impl: observeCurrentUser, updateUserRole, getAllUsers | `AuthRepositoryImpl.kt` | ✅ |
| Use cases mới | `AuthUseCases.kt` | ✅ |
| AppViewModel — observe role realtime | `AppViewModel.kt` | ✅ |
| AppNavHost — inject AppViewModel, guard ManageStaff | `AppNavHost.kt` | ✅ |
| Màn hình quản lý nhân viên | `ManageStaffScreen.kt` | ✅ |
| ViewModel quản lý nhân viên | `ManageStaffViewModel.kt` | ✅ |



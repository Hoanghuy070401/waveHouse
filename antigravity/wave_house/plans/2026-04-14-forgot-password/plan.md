# Implementation Plan: Cập nhật luồng Forgot Password (Quên mật khẩu)

**Date:** 2026-04-14
**Updated By:** AntiGravity Assistant
**Status:** Draft
**Complexity:** Medium
**Estimated Effort:** ~4-6 hours

## Context
Xây dựng tính năng "Quên mật khẩu" hoàn chỉnh để người dùng có thể tự khôi phục tài khoản thông qua Firebase Email Auth. Yêu cầu chính là UX thân thiện (che email bảo mật, auto-open Gmail, countdown timer 30s) và thực hiện đổi mật khẩu ngay bên trong app thay vì trên giao diện web của Firebase (qua cấu hình Deep Link).

## Implementation Phases

### Phase 01: Giao diện Yêu cầu khôi phục (Request & Success)
Tạo UI và Logic xử lý việc nhập email, gọi Firebase gửi link reset và flow xác nhận gửi email.
- **Tình trạng:** Not Started
- **File:** [phase-01-ui.md](./phase-01-ui.md)

### Phase 02: Deep Link & Đặt mật khẩu mới
Xử lý Deep Linking khi bấm vào email (thay vì web mặc định), hiển thị giao diện nhập lại mật khẩu mới, cập nhật lên Firebase và chuyển về màn Login.
- **Tình trạng:** Not Started
- **File:** [phase-02-deeplink.md](./phase-02-deeplink.md)

## Next Steps
Review plan và thực thi Phase 01.

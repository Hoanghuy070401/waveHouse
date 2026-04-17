# Research: Registration Flows for RBAC in Firebase Multi-tenant Apps

## Context
WaveHouse is an inventory management app utilizing Role-Based Access Control (RBAC). Currently, open registration defaults every new user to `STAFF` and sets `warehouseId` to `""`. This causes confusion as the user does not know what role they are making or how to become an Admin to manage a warehouse.

## User Flow Options

### 1. Invitation-Only (No Open Registration)
- **Concept:** Only Admins can create new user accounts or generate invite links.
- **Pros:** Highest security, roles and warehouse IDs are correctly mapped from the start.
- **Cons:** High friction. The first user (Company Owner) needs a way to register first.

### 2. Multi-tenant Self-Registration (Store Creation vs. Joining)
- **Concept:** 
  - On the `RegisterScreen`, user selects: **"Tạo cửa hàng mới"** (Create Store) or **"Tham gia cửa hàng"** (Join Store).
  - If **Create Store**, they are assigned `ADMIN` and a new `warehouseId` is generated.
  - If **Join Store**, they just register as `STAFF` (or select a role) but must enter an `inviteCode` or `warehouseId` provided by their Admin to link to the existing store.
- **Pros:** Logical workflow, clear distinction between Owner and Staff.
- **Cons:** Requires adding Warehouse creation logic during registration.

### 3. Open Registration with Role Selection & Approval
- **Concept:** User registers open, dropdown asks for "Loại tài khoản: Quản trị viên, Thủ kho, Kế toán, Nhân viên".
- **Pros:** User gets to choose.
- **Cons:** If they choose Admin but join an existing warehouse, it's a security risk. Admins must still approve or invite codes must be used to verify.

## WaveHouse Requirements
Currently the app asks for `warehouseId: String` in User model. Without a valid `warehouseId`, even an Admin cannot manage products.
Therefore, **Approach 2** is the industry standard for SaaS/B2B apps.

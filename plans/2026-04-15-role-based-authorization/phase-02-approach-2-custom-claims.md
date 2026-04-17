# Giai đoạn 2: Phương án 2 - RBAC bằng Custom Claims

## 1. Firebase Cloud Functions Webhook
Triển khai một function TypeScript sử dụng Firebase Admin SDK, can thiệp vào giai đoạn set Claims token khi thay đổi roles cho Member.

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
admin.initializeApp();

export const setAdminRole = functions.https.onCall(async (data, context) => {
  // Check quyền người đang gửi yêu cầu thiết lập quyền (phải là ADMIN mới sửa được tiếp)
  if (context.auth?.token.role !== 'ADMIN') throw new functions.https.HttpsError('permission-denied', 'Unauthorized');
  
  const targetUid = data.uid;
  const targetRole = data.role; // Ví dụ cắm quyền: "WAREHOUSE"
  
  await admin.auth().setCustomUserClaims(targetUid, { role: targetRole });
  // Backup: Cập nhật lại luôn cả Firestore document /users/{uid} để Android App tự sync state nhanh hơn
  await admin.firestore().collection('users').doc(targetUid).update({ role: targetRole });
  
  return { message: `Cấp quyền ${targetRole} thành công cho User ID: ${targetUid}` };
});
```

## 2. Cập nhật bảng Security Rules Firestore 
Cấu trúc Rule giảm tải việc read, lấy giá trị biến lưu cố định bên trong token.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    function isAdmin() { return request.auth.token.role == 'ADMIN'; }
    function isWarehouse() { return request.auth.token.role == 'WAREHOUSE'; }

    match /products/{document=**} {
      allow read: if request.auth != null;
      allow write: if isAdmin();
    }
  }
}
```

## 3. Cập nhật bên nền tảng Android App (Voucher Token Manual Refresher)
Mỗi sự kiện thay đổi, token cần bị cưỡng chế refresh lấy token mới, tránh delay đợi hết hạn.

```kotlin
// Yêu cầu khởi tạo refresh ID token sau mỗi đợt có tin nhắn báo role thay đổi
FirebaseAuth.getInstance().currentUser?.getIdToken(true)
```

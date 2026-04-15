# Phase 1: Approach 1 - Firestore Document-Based RBAC

## 1. Update Firestore Security Rules (`firestore.rules`)
Write rules using a helper function to fetch the user document and extract the role locally in Firebase rules.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    function getUserRole() {
      // Returns 'ADMIN', 'WAREHOUSE', 'ACCOUNTANT', or 'STAFF'
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role;
    }
    
    function isAdmin() { return getUserRole() == 'ADMIN'; }
    function isWarehouse() { return getUserRole() == 'WAREHOUSE'; }
    function isAccountant() { return getUserRole() == 'ACCOUNTANT'; }

    // Admin has full access to products. Others only read.
    match /products/{document=**} {
      allow read: if request.auth != null;
      allow write: if isAdmin();
    }
    
    // Warehouse and Admin can update stock. Staff might only read.
    match /stock/{document=**} {
      allow read: if request.auth != null;
      allow write: if isAdmin() || isWarehouse();
    }
    
    // User can read their own profile data, only Admin can edit roles.
    match /users/{userId} {
      allow read: if request.auth != null && (request.auth.uid == userId || isAdmin());
      allow write: if isAdmin();
    }
  }
}
```

## 2. Client-Side Implementation
- Sync the user document via `AuthRepository` real-time listener (already somewhat available).
- Route constraints: Update Navigation logic, preventing Staff from opening restricted screens (`NavGraph` or within the Screen elements).
- Limit UI visibility based on the retrieved `User` properties (e.g. `if (user.isAdmin) { ShowAdminMenu() }`).

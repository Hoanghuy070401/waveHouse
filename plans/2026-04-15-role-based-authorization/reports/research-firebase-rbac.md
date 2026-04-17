# Research: Firebase Role-Based Access Control (RBAC) in Android

## 1. Firebase Custom Claims
Store role directly in ID Token.
**Pros**: High performance (no extra reads needed for security rules), cryptographically secure.
**Cons**: Requires server-side (Cloud Functions) Admin SDK to set roles. Changes have propagation delay (~1hr or requires forced refresh). Strict 1000-byte limit.

## 2. Firestore Document-Based Roles
Store role in user document `/users/{uid}`.
**Pros**: Real-time updates, unlimited size, no need for backend logic (Cloud Functions), easier to manage via console/client.
**Cons**: Requires a `get()` call in security rules (read quota usage per rule check). 

## Existing State
`UserRole` exists in `com.wavehouse.domain.model.Models.kt`: `ADMIN, WAREHOUSE, ACCOUNTANT, STAFF`.
Client app relies on getters like `val isAdmin get() = role == UserRole.ADMIN` for UI logic.

## Unresolved Questions
1. Is the budget for Firestore Reads extremely tight? If so, Approach 1 is better.
2. Are Cloud Functions already set up for this project?

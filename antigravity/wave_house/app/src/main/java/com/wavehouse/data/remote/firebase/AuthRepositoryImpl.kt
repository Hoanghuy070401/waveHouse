package com.wavehouse.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.User
import com.wavehouse.domain.model.UserRole
import com.wavehouse.domain.model.UserStatus
import com.wavehouse.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseDatabase
) : AuthRepository {

    private val usersRef = database.getReference("users")
    private val warehousesRef = database.getReference("warehouses")

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        isOwner: Boolean,
        warehouseCode: String
    ): ApiResult<User> = safeApiCall {
        // Bước 1: Tạo Firebase Auth BEFORE ghi DB — nếu fail không để lại rác trong DB
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Đăng ký thất bại")

        try {
            val finalWarehouseId: String
            val role: UserRole
            val status: UserStatus

            if (isOwner) {
                // Bước 2a (Chủ cửa hàng): Tạo kho với mã join 6 ký tự ngẫu nhiên
                val joinCode = generateJoinCode()
                val newRef = warehousesRef.push()
                finalWarehouseId = newRef.key ?: throw Exception("Không thể khởi tạo kho")
                newRef.setValue(
                    mapOf(
                        "id" to finalWarehouseId,
                        "name" to warehouseCode.trim(),   // warehouseCode = tên cửa hàng
                        "joinCode" to joinCode,            // Mã 6 ký tự để nhân viên nhập
                        "managerId" to uid,
                        "status" to "ACTIVE",
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
                role = UserRole.ADMIN
                status = UserStatus.ACTIVE  // Chủ cửa hàng luôn active
            } else {
                // Bước 2b (Nhân viên): Tìm kho theo joinCode 6 ký tự (không dùng push key)
                val joinCode = warehouseCode.trim().uppercase()
                val snapshot = warehousesRef
                    .orderByChild("joinCode")
                    .equalTo(joinCode)
                    .get().await()
                if (!snapshot.exists() || snapshot.children.none()) {
                    throw Exception("Mã cửa hàng không hợp lệ hoặc không tồn tại")
                }
                val warehouseSnap = snapshot.children.first()
                finalWarehouseId = warehouseSnap.key
                    ?: throw Exception("Không xác định được kho")
                role = UserRole.STAFF
                status = UserStatus.PENDING  // Nhân viên chờ Admin duyệt
            }

            // Bước 3: Ghi User vào Realtime DB
            val user = User(
                id = uid,
                name = name.trim(),
                email = email.trim(),
                role = role,
                status = status,
                warehouseId = finalWarehouseId,
                createdAt = System.currentTimeMillis()
            )
            usersRef.child(uid).setValue(
                mapOf(
                    "name" to user.name,
                    "email" to user.email,
                    "role" to role.name,
                    "status" to status.name,
                    "warehouseId" to finalWarehouseId,
                    "createdAt" to user.createdAt
                )
            ).await()
            user
        } catch (e: Exception) {
            // Rollback: Xoá Auth account nếu ghi DB thất bại
            authResult.user?.delete()?.await()
            throw e
        }
    }

    /** Tạo mã join 6 ký tự uppercase alphanumeric (alphabet + số — bỏ các chữu dễ cố đọc nhầm 0/O, I/1) */
    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    override suspend fun login(email: String, password: String): ApiResult<User> =
        safeApiCall {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Đăng nhập thất bại")
            val uid = firebaseUser.uid

            // Lấy thông tin user từ DB — không fallback sang STAFF để tránh sai role
            fetchUserFromDatabase(uid)
                ?: throw Exception("Không tìm thấy thông tin tài khoản. Vui lòng liên hệ Admin.")
        }

    override suspend fun logout(): ApiResult<Unit> = safeApiCall {
        firebaseAuth.signOut()
    }

    override suspend fun sendEmailVerification(): ApiResult<Unit> = safeApiCall {
        val user = firebaseAuth.currentUser
            ?: throw Exception("Chưa đăng nhập")
        user.sendEmailVerification().await()
    }

    override suspend fun reloadAndCheckVerified(): Boolean {
        return try {
            firebaseAuth.currentUser?.reload()?.await()
            firebaseAuth.currentUser?.isEmailVerified == true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getCurrentUser(): User? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        return fetchUserFromDatabase(uid)
    }

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid == null) {
                trySend(null)
            } else {
                usersRef.child(uid).get().addOnSuccessListener { snapshot ->
                    trySend(snapshot.toUser())
                }.addOnFailureListener {
                    trySend(null)
                }
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): ApiResult<Unit> = safeApiCall {
        val user = firebaseAuth.currentUser ?: throw Exception("Chưa đăng nhập")
        val email = user.email ?: throw Exception("Không tìm thấy email")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    override suspend fun sendPasswordResetEmail(email: String): ApiResult<Unit> = safeApiCall {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    override suspend fun confirmPasswordReset(
        oobCode: String,
        newPassword: String
    ): ApiResult<Unit> = safeApiCall {
        firebaseAuth.confirmPasswordReset(oobCode, newPassword).await()
    }

    override suspend fun updateProfile(name: String, avatarUrl: String?): ApiResult<Unit> =
        safeApiCall {
            val uid = firebaseAuth.currentUser?.uid ?: throw Exception("Chưa đăng nhập")
            val updates = mutableMapOf<String, Any>("name" to name)
            avatarUrl?.let { updates["avatarUrl"] = it }
            usersRef.child(uid).updateChildren(updates).await()
        }


    override suspend fun updateUserRole(
        targetUserId: String,
        newRole: UserRole
    ): ApiResult<Unit> = safeApiCall {
        usersRef.child(targetUserId).child("role").setValue(newRole.name).await()
    }

    override suspend fun approveStaff(targetUserId: String): ApiResult<Unit> = safeApiCall {
        usersRef.child(targetUserId).child("status").setValue(UserStatus.ACTIVE.name).await()
    }

    override suspend fun getWarehouseJoinCode(warehouseId: String): ApiResult<String> = safeApiCall {
        val snapshot = warehousesRef.child(warehouseId).child("joinCode").get().await()
        snapshot.getValue(String::class.java) ?: throw Exception("Không tìm thấy mã kết nối")
    }

    /**
     * Lắng nghe realtime thay đổi profile user (kể cả role, status).
     *
     * Fix: Kết hợp AuthStateListener + RTDB ValueEventListener.
     * - Vấn đề cũ: lấy uid 1 lần khi tạo flow, nếu uid=null → close() ngay →
     *   Admin sau khi login vẫn bị áp initialValue=STAFF mãi.
     * - Fix: Mỗi khi Auth state thay đổi, gắn lại listener vào đúng UID mới.
     */
    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        var dbListener: ValueEventListener? = null
        var currentListenUid: String? = null

        fun removeCurrentDbListener() {
            val uid = currentListenUid
            val listener = dbListener
            if (uid != null && listener != null) {
                usersRef.child(uid).removeEventListener(listener)
            }
            dbListener = null
            currentListenUid = null
        }

        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid == currentListenUid) return@AuthStateListener  // Không đổi, bỏ qua

            removeCurrentDbListener()  // Xoá listener cũ

            if (uid == null) {
                trySend(null)
                return@AuthStateListener
            }

            // Gắn listener mới vào UID mới
            val valueListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.toUser())
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(null)
                }
            }
            dbListener = valueListener
            currentListenUid = uid
            usersRef.child(uid).addValueEventListener(valueListener)
        }

        firebaseAuth.addAuthStateListener(authListener)
        awaitClose {
            removeCurrentDbListener()
            firebaseAuth.removeAuthStateListener(authListener)
        }
    }

    override suspend fun getAllUsers(): ApiResult<List<User>> = safeApiCall {
        val snapshot = usersRef.get().await()
        snapshot.children.mapNotNull { it.toUser() }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private suspend fun fetchUserFromDatabase(uid: String): User? {
        return try {
            val snapshot = usersRef.child(uid).get().await()
            snapshot.toUser()
        } catch (e: Exception) {
            null
        }
    }
}

/** Extension: DataSnapshot → User domain model */
private fun DataSnapshot?.toUser(): User? {
    if (this == null || !exists()) return null
    return try {
        User(
            id = key ?: "",
            name = child("name").getValue(String::class.java) ?: "",
            email = child("email").getValue(String::class.java) ?: "",
            role = try {
                UserRole.valueOf(child("role").getValue(String::class.java) ?: "STAFF")
            } catch (e: IllegalArgumentException) { UserRole.STAFF },
            status = try {
                UserStatus.valueOf(child("status").getValue(String::class.java) ?: "ACTIVE")
            } catch (e: IllegalArgumentException) { UserStatus.ACTIVE },
            warehouseId = child("warehouseId").getValue(String::class.java) ?: "",
            avatarUrl = child("avatarUrl").getValue(String::class.java),
            createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}

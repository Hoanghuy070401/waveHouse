package com.wavehouse.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.User
import com.wavehouse.domain.model.UserRole
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
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun login(email: String, password: String): ApiResult<User> =
        safeApiCall {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Đăng nhập thất bại")
            fetchUserFromFirestore(uid) ?: throw Exception("Không tìm thấy thông tin người dùng")
        }

    override suspend fun logout(): ApiResult<Unit> = safeApiCall {
        firebaseAuth.signOut()
    }

    override suspend fun getCurrentUser(): User? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        return fetchUserFromFirestore(uid)
    }

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = firebaseAuth.addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid == null) {
                trySend(null)
            } else {
                // Fetch user async
                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        trySend(doc.toUser())
                    }
                    .addOnFailureListener {
                        trySend(null)
                    }
            }
        }
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): ApiResult<Unit> = safeApiCall {
        val user = firebaseAuth.currentUser ?: throw Exception("Chưa đăng nhập")
        // Re-authenticate first
        val email = user.email ?: throw Exception("Không tìm thấy email")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    override suspend fun sendPasswordResetEmail(email: String): ApiResult<Unit> = safeApiCall {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    override suspend fun updateProfile(name: String, avatarUrl: String?): ApiResult<Unit> =
        safeApiCall {
            val uid = firebaseAuth.currentUser?.uid ?: throw Exception("Chưa đăng nhập")
            val updates = mutableMapOf<String, Any>("name" to name)
            avatarUrl?.let { updates["avatarUrl"] = it }
            firestore.collection("users").document(uid).update(updates).await()
        }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private suspend fun fetchUserFromFirestore(uid: String): User? {
        val doc = firestore.collection("users").document(uid).get().await()
        return doc.toUser()
    }
}

/** Extension: Firestore DocumentSnapshot → User domain model */
private fun com.google.firebase.firestore.DocumentSnapshot?.toUser(): User? {
    if (this == null || !exists()) return null
    return try {
        User(
            id = id,
            name = getString("name") ?: "",
            email = getString("email") ?: "",
            role = UserRole.valueOf(getString("role") ?: "STAFF"),
            warehouseId = getString("warehouseId") ?: "",
            avatarUrl = getString("avatarUrl"),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}

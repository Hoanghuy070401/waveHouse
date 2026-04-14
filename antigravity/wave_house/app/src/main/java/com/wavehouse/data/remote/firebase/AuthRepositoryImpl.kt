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

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): ApiResult<User> = safeApiCall {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw Exception("Đăng ký thất bại")
        val user = User(
            id = uid,
            name = name,
            email = email,
            role = UserRole.STAFF,
            warehouseId = "",
            createdAt = System.currentTimeMillis()
        )
        usersRef.child(uid).setValue(
            mapOf(
                "name" to name,
                "email" to email,
                "role" to "STAFF",
                "warehouseId" to "",
                "createdAt" to user.createdAt
            )
        ).await()
        user
    }

    override suspend fun login(email: String, password: String): ApiResult<User> =
        safeApiCall {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Đăng nhập thất bại")
            val uid = firebaseUser.uid
            
            fetchUserFromDatabase(uid) ?: User(
                id = uid,
                name = firebaseUser.displayName ?: email.substringBefore("@"),
                email = firebaseUser.email ?: email,
                role = UserRole.STAFF,
                warehouseId = "",
                createdAt = System.currentTimeMillis()
            )
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
            role = UserRole.valueOf(child("role").getValue(String::class.java) ?: "STAFF"),
            warehouseId = child("warehouseId").getValue(String::class.java) ?: "",
            avatarUrl = child("avatarUrl").getValue(String::class.java),
            createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}

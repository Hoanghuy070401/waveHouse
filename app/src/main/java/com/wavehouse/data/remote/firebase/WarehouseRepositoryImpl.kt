package com.wavehouse.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.Warehouse
import com.wavehouse.domain.model.WarehouseStatus
import com.wavehouse.domain.repository.WarehouseRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class WarehouseRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage
) : WarehouseRepository {

    private val warehousesRef = database.getReference("warehouses")
    private val usersRef = database.getReference("users")

    override fun getWarehouses(userId: String): Flow<ApiResult<List<Warehouse>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val warehouses = snapshot.children.mapNotNull { doc ->
                    try {
                        val membersSnapshot = doc.child("members")
                        val isMember = membersSnapshot.children.any { it.getValue(String::class.java) == userId }
                                || membersSnapshot.child(userId).exists() // Support map or list
                        
                        if (!isMember) return@mapNotNull null
                        
                        Warehouse(
                            id = doc.key ?: "",
                            name = doc.child("name").getValue(String::class.java) ?: "",
                            address = doc.child("address").getValue(String::class.java),
                            managerId = doc.child("managerId").getValue(String::class.java) ?: "",
                            status = WarehouseStatus.valueOf(doc.child("status").getValue(String::class.java) ?: "ACTIVE"),
                            memberCount = doc.child("members").childrenCount.toInt(),
                            createdAt = doc.child("createdAt").getValue(Long::class.java) ?: 0L
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing warehouse: ${doc.key}")
                        null
                    }
                }
                trySend(ApiResult.Success(warehouses))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ApiResult.Error(error.message))
            }
        }
        warehousesRef.addValueEventListener(listener)
        awaitClose { warehousesRef.removeEventListener(listener) }
    }

    override suspend fun getWarehouseById(id: String): ApiResult<Warehouse> = safeApiCall {
        val doc = warehousesRef.child(id).get().await()
        Warehouse(
            id = doc.key ?: "",
            name = doc.child("name").getValue(String::class.java) ?: "",
            address = doc.child("address").getValue(String::class.java),
            managerId = doc.child("managerId").getValue(String::class.java) ?: "",
            status = WarehouseStatus.valueOf(doc.child("status").getValue(String::class.java) ?: "ACTIVE"),
            memberCount = doc.child("members").childrenCount.toInt(),
            qrImageUrl = doc.child("qrImageUrl").getValue(String::class.java),
            createdAt = doc.child("createdAt").getValue(Long::class.java) ?: 0L
        )
    }

    override fun observeWarehouse(id: String): Flow<ApiResult<Warehouse>> = callbackFlow {
        trySend(ApiResult.Loading)
        val ref = warehousesRef.child(id)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(ApiResult.Error("Không tìm thấy kho"))
                    return
                }
                try {
                    val warehouse = Warehouse(
                        id = snapshot.key ?: "",
                        name = snapshot.child("name").getValue(String::class.java) ?: "",
                        address = snapshot.child("address").getValue(String::class.java),
                        managerId = snapshot.child("managerId").getValue(String::class.java) ?: "",
                        status = WarehouseStatus.valueOf(
                            snapshot.child("status").getValue(String::class.java) ?: "ACTIVE"
                        ),
                        memberCount = snapshot.child("members").childrenCount.toInt(),
                        qrImageUrl = snapshot.child("qrImageUrl").getValue(String::class.java),
                        createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                    )
                    trySend(ApiResult.Success(warehouse))
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing warehouse: ${snapshot.key}")
                    trySend(ApiResult.Error(e.message ?: "Lỗi parse warehouse"))
                }
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ApiResult.Error(error.message))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun switchWarehouse(userId: String, warehouseId: String): ApiResult<Unit> = safeApiCall {
        usersRef.child(userId).child("warehouseId").setValue(warehouseId).await()
    }

    override suspend fun updateQrImageUrl(warehouseId: String, qrImageUrl: String?): ApiResult<Unit> = safeApiCall {
        warehousesRef.child(warehouseId).child("qrImageUrl").setValue(qrImageUrl).await()
    }

    override suspend fun uploadQrImage(warehouseId: String, imageBytes: ByteArray): ApiResult<String> = safeApiCall {
        val ref = storage.reference.child("warehouse_qr/$warehouseId.jpg")
        ref.putBytes(imageBytes).await()
        ref.downloadUrl.await().toString()
    }

    override suspend fun deleteQrImage(imageUrl: String): ApiResult<Unit> = safeApiCall {
        runCatching { storage.getReferenceFromUrl(imageUrl).delete().await() }
            .onFailure { Timber.w(it, "Không thể xoá ảnh QR cũ: $imageUrl") }
        Unit
    }
}

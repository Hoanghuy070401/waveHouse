package com.wavehouse.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
    private val database: FirebaseDatabase
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
            createdAt = doc.child("createdAt").getValue(Long::class.java) ?: 0L
        )
    }

    override suspend fun switchWarehouse(userId: String, warehouseId: String): ApiResult<Unit> = safeApiCall {
        usersRef.child(userId).child("warehouseId").setValue(warehouseId).await()
    }
}

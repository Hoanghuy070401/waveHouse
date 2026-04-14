package com.wavehouse.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.Warehouse
import com.wavehouse.domain.model.WarehouseStatus
import com.wavehouse.domain.repository.WarehouseRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject

class WarehouseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : WarehouseRepository {

    private val warehousesCollection get() = firestore.collection("warehouses")

    override fun getWarehouses(userId: String): Flow<ApiResult<List<Warehouse>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val listener = warehousesCollection
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(ApiResult.Error(error.message ?: "Lỗi tải kho hàng"))
                    return@addSnapshotListener
                }
                val warehouses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Warehouse(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            address = doc.getString("address"),
                            managerId = doc.getString("managerId") ?: "",
                            status = WarehouseStatus.valueOf(doc.getString("status") ?: "ACTIVE"),
                            memberCount = (doc.get("members") as? List<*>)?.size ?: 0,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing warehouse: ${doc.id}")
                        null
                    }
                } ?: emptyList()
                trySend(ApiResult.Success(warehouses))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getWarehouseById(id: String): ApiResult<Warehouse> = safeApiCall {
        val doc = warehousesCollection.document(id).get().addOnFailureListener { throw it }.result
        Warehouse(
            id = doc.id,
            name = doc.getString("name") ?: "",
            address = doc.getString("address"),
            managerId = doc.getString("managerId") ?: "",
            status = WarehouseStatus.valueOf(doc.getString("status") ?: "ACTIVE"),
            memberCount = (doc.get("members") as? List<*>)?.size ?: 0,
            createdAt = doc.getLong("createdAt") ?: 0L
        )
    }

    override suspend fun switchWarehouse(userId: String, warehouseId: String): ApiResult<Unit> = safeApiCall {
        firestore.collection("users").document(userId)
            .update("warehouseId", warehouseId)
            .addOnFailureListener { throw it }
    }
}

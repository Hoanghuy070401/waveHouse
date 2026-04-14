package com.wavehouse.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.Supplier
import com.wavehouse.domain.repository.SupplierRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SupplierRepository {

    override fun getSuppliers(warehouseId: String): Flow<ApiResult<List<Supplier>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val listener = firestore.collection("suppliers")
            .whereEqualTo("warehouseId", warehouseId)
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(ApiResult.Error(error.message ?: "Lỗi tải nhà cung cấp"))
                    return@addSnapshotListener
                }
                val suppliers = snapshot?.documents?.mapNotNull { doc ->
                    Supplier(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        phone = doc.getString("phone"),
                        email = doc.getString("email"),
                        address = doc.getString("address"),
                        warehouseId = doc.getString("warehouseId") ?: warehouseId,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(ApiResult.Success(suppliers))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getSupplierById(id: String): ApiResult<Supplier> = safeApiCall {
        val doc = firestore.collection("suppliers").document(id).get().await()
        Supplier(
            id = doc.id,
            name = doc.getString("name") ?: throw Exception("Không tìm thấy nhà cung cấp"),
            phone = doc.getString("phone"),
            email = doc.getString("email"),
            address = doc.getString("address"),
            warehouseId = doc.getString("warehouseId") ?: "",
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    override suspend fun createSupplier(supplier: Supplier): ApiResult<String> = safeApiCall {
        val docRef = firestore.collection("suppliers").document()
        docRef.set(mapOf(
            "id" to docRef.id,
            "name" to supplier.name,
            "phone" to supplier.phone,
            "email" to supplier.email,
            "address" to supplier.address,
            "warehouseId" to supplier.warehouseId,
            "createdAt" to System.currentTimeMillis()
        )).await()
        docRef.id
    }

    override suspend fun updateSupplier(supplier: Supplier): ApiResult<Unit> = safeApiCall {
        firestore.collection("suppliers").document(supplier.id).update(mapOf(
            "name" to supplier.name,
            "phone" to supplier.phone,
            "email" to supplier.email,
            "address" to supplier.address
        )).await()
    }

    override suspend fun deleteSupplier(id: String): ApiResult<Unit> = safeApiCall {
        firestore.collection("suppliers").document(id).delete().await()
    }
}

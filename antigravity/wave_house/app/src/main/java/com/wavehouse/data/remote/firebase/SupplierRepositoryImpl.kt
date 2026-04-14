package com.wavehouse.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.Supplier
import com.wavehouse.domain.repository.SupplierRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : SupplierRepository {

    private val suppliersRef = database.getReference("suppliers")

    override fun getSuppliers(warehouseId: String): Flow<ApiResult<List<Supplier>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val query = suppliersRef.orderByChild("warehouseId").equalTo(warehouseId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val suppliers = snapshot.children.mapNotNull { it.toSupplier() }.sortedBy { it.name }
                trySend(ApiResult.Success(suppliers))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ApiResult.Error(error.message))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun getSupplierById(id: String): ApiResult<Supplier> = safeApiCall {
        val snapshot = suppliersRef.child(id).get().await()
        snapshot.toSupplier() ?: throw Exception("Không tìm thấy nhà cung cấp")
    }

    override suspend fun createSupplier(supplier: Supplier): ApiResult<String> = safeApiCall {
        val id = supplier.id.takeIf { it.isNotEmpty() } ?: suppliersRef.push().key ?: UUID.randomUUID().toString()
        suppliersRef.child(id).setValue(mapOf(
            "name" to supplier.name,
            "phone" to supplier.phone,
            "email" to supplier.email,
            "address" to supplier.address,
            "warehouseId" to supplier.warehouseId,
            "createdAt" to System.currentTimeMillis()
        )).await()
        id
    }

    override suspend fun updateSupplier(supplier: Supplier): ApiResult<Unit> = safeApiCall {
        suppliersRef.child(supplier.id).updateChildren(mapOf(
            "name" to supplier.name,
            "phone" to supplier.phone,
            "email" to supplier.email,
            "address" to supplier.address
        )).await()
    }

    override suspend fun deleteSupplier(id: String): ApiResult<Unit> = safeApiCall {
        suppliersRef.child(id).removeValue().await()
    }

    private fun DataSnapshot?.toSupplier(): Supplier? {
        if (this == null || !exists()) return null
        return try {
            Supplier(
                id = key ?: "",
                name = child("name").getValue(String::class.java) ?: return null,
                phone = child("phone").getValue(String::class.java),
                email = child("email").getValue(String::class.java),
                address = child("address").getValue(String::class.java),
                warehouseId = child("warehouseId").getValue(String::class.java) ?: "",
                createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            )
        } catch (e: Exception) { null }
    }
}

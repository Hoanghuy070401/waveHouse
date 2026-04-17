package com.wavehouse.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.*
import com.wavehouse.domain.repository.StockRepository
import com.wavehouse.core.utils.todayStartMillis
import com.wavehouse.core.utils.last7DaysLabels
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : StockRepository {

    private val stockRef = database.getReference("stock")
    private val entriesRef = database.getReference("stock_entries")

    override fun getStockItems(warehouseId: String): Flow<ApiResult<List<StockItem>>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = snapshot.children.mapNotNull { it.toStockItem() }
                    trySend(ApiResult.Success(items))
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(ApiResult.Error(error.message))
                }
            }
            val query = stockRef.child(warehouseId).child("items")
            query.addValueEventListener(listener)
            awaitClose { query.removeEventListener(listener) }
        }

    override fun getLowStockItems(warehouseId: String): Flow<ApiResult<List<StockItem>>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = snapshot.children.mapNotNull { it.toStockItem() }
                        .filter { it.stockStatus != StockStatus.IN_STOCK }
                    trySend(ApiResult.Success(items))
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(ApiResult.Error(error.message))
                }
            }
            val query = stockRef.child(warehouseId).child("items")
            query.addValueEventListener(listener)
            awaitClose { query.removeEventListener(listener) }
        }

    override fun getStockHistory(warehouseId: String): Flow<ApiResult<List<StockEntry>>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val query = entriesRef.orderByChild("warehouseId").equalTo(warehouseId).limitToLast(100)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val entries = snapshot.children.mapNotNull { it.toStockEntry() }
                        .sortedByDescending { it.createdAt }
                    trySend(ApiResult.Success(entries))
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(ApiResult.Error(error.message))
                }
            }
            query.addValueEventListener(listener)
            awaitClose { query.removeEventListener(listener) }
        }

    override fun getProductStockHistory(
        productId: String,
        warehouseId: String
    ): Flow<ApiResult<List<StockEntry>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val query = entriesRef.orderByChild("warehouseId").equalTo(warehouseId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = snapshot.children.mapNotNull { it.toStockEntry() }
                    .filter { it.productId == productId }
                    .sortedByDescending { it.createdAt }
                trySend(ApiResult.Success(entries))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ApiResult.Error(error.message))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun createStockIn(
        productId: String,
        warehouseId: String,
        quantity: Double,
        unitCostPrice: Double?,
        supplierId: String?,
        note: String?
    ): ApiResult<Unit> = safeApiCall {
        val itemPath = "stock/$warehouseId/items/$productId"
        val productPath = "products/$productId"
        val entryId = entriesRef.push().key ?: UUID.randomUUID().toString()
        val entryPath = "stock_entries/$entryId"

        // ── Atomic read-compute-write trên product node ─────────────────────
        // runTransaction đảm bảo MAC tính đúng kể cả khi 2 nhập kho đồng thời
        data class ProductUpdate(val newQty: Double, val newMac: Double)
        var productUpdate: ProductUpdate? = null

        suspendCancellableCoroutine<Unit> { cont ->
            database.getReference(productPath).runTransaction(
                object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(
                        data: com.google.firebase.database.MutableData
                    ): com.google.firebase.database.Transaction.Result {
                        val currentQty = data.child("currentStock").getValue(Double::class.java) ?: 0.0
                        val currentCostPrice = data.child("costPrice").getValue(Double::class.java) ?: 0.0
                        val newQty = currentQty + quantity

                        // MAC = (Tồn kho cũ × Giá vốn cũ + Lô mới × Giá lô mới) / Tổng tồn mới
                        val newMac = if (unitCostPrice != null && unitCostPrice > 0.0) {
                            val oldValue = currentQty * currentCostPrice
                            val newBatchValue = quantity * unitCostPrice
                            if (newQty > 0.0) (oldValue + newBatchValue) / newQty else unitCostPrice
                        } else currentCostPrice

                        val macRounded = Math.round(newMac * 100.0) / 100.0

                        data.child("currentStock").value = newQty
                        data.child("costPrice").value = macRounded
                        productUpdate = ProductUpdate(newQty, macRounded)
                        return com.google.firebase.database.Transaction.success(data)
                    }

                    override fun onComplete(
                        error: com.google.firebase.database.DatabaseError?,
                        committed: Boolean,
                        snapshot: com.google.firebase.database.DataSnapshot?
                    ) {
                        if (error != null) cont.resumeWith(Result.failure(Exception(error.message)))
                        else cont.resumeWith(Result.success(Unit))
                    }
                }
            )
        }

        val update = productUpdate ?: throw Exception("Không thể cập nhật tồn kho")

        // Cập nhật stock item path + ghi stock entry
        val updates = mapOf(
            "$itemPath/quantity" to update.newQty,
            "$itemPath/productId" to productId,
            "$itemPath/warehouseId" to warehouseId,
            "$itemPath/lastUpdated" to System.currentTimeMillis(),
            "$entryPath/id" to entryId,
            "$entryPath/type" to "IN",
            "$entryPath/productId" to productId,
            "$entryPath/warehouseId" to warehouseId,
            "$entryPath/quantity" to quantity,
            "$entryPath/unitCostPrice" to (unitCostPrice ?: update.newMac),
            "$entryPath/macAfter" to update.newMac,
            "$entryPath/note" to note,
            "$entryPath/supplierId" to supplierId,
            "$entryPath/createdAt" to System.currentTimeMillis()
        )
        database.reference.updateChildren(updates).await()
    }

    override suspend fun createStockOut(
        productId: String,
        warehouseId: String,
        quantity: Double,
        note: String?
    ): ApiResult<Unit> = safeApiCall {
        val itemPath = "stock/$warehouseId/items/$productId"
        val productPath = "products/$productId"
        val currentSnap = database.getReference(itemPath).get().await()
        val currentQty = currentSnap.child("quantity").getValue(Double::class.java) ?: 0.0

        if (quantity > currentQty) throw Exception("Tồn kho không đủ")
        val newQty = currentQty - quantity

        val entryId = entriesRef.push().key ?: UUID.randomUUID().toString()
        val entryPath = "stock_entries/$entryId"

        val updates = mapOf(
            "$itemPath/quantity" to newQty,
            "$itemPath/lastUpdated" to System.currentTimeMillis(),
            "$productPath/currentStock" to newQty,
            "$entryPath/id" to entryId,
            "$entryPath/type" to "OUT",
            "$entryPath/productId" to productId,
            "$entryPath/warehouseId" to warehouseId,
            "$entryPath/quantity" to quantity,
            "$entryPath/note" to note,
            "$entryPath/createdAt" to System.currentTimeMillis()
        )
        database.reference.updateChildren(updates).await()
    }

    override suspend fun adjustStock(
        productId: String,
        warehouseId: String,
        newQuantity: Double,
        note: String?
    ): ApiResult<Unit> = safeApiCall {
        val itemPath = "stock/$warehouseId/items/$productId"
        database.getReference(itemPath).updateChildren(mapOf(
            "quantity" to newQuantity,
            "lastUpdated" to System.currentTimeMillis()
        )).await()
        database.getReference("products/$productId")
            .updateChildren(mapOf("currentStock" to newQuantity)).await()
    }

    override suspend fun createShrinkage(
        productId: String,
        warehouseId: String,
        quantity: Double,
        reason: ShrinkageReason,
        note: String?
    ): ApiResult<Unit> = safeApiCall {
        val itemPath = "stock/$warehouseId/items/$productId"
        val productPath = "products/$productId"
        val currentSnap = database.getReference(itemPath).get().await()
        val currentQty = currentSnap.child("quantity").getValue(Double::class.java) ?: 0.0

        if (quantity > currentQty) throw Exception("Tồn kho không đủ")
        val newQty = currentQty - quantity

        val entryId = entriesRef.push().key ?: UUID.randomUUID().toString()
        val entryPath = "stock_entries/$entryId"

        val updates = mapOf(
            "$itemPath/quantity" to newQty,
            "$itemPath/lastUpdated" to System.currentTimeMillis(),
            "$productPath/currentStock" to newQty,
            "$entryPath/id" to entryId,
            "$entryPath/type" to "SHRINKAGE",
            "$entryPath/productId" to productId,
            "$entryPath/warehouseId" to warehouseId,
            "$entryPath/quantity" to quantity,
            "$entryPath/shrinkageReason" to reason.name,
            "$entryPath/note" to note,
            "$entryPath/createdAt" to System.currentTimeMillis()
        )
        database.reference.updateChildren(updates).await()
    }

    override fun getTodayStats(warehouseId: String): Flow<ApiResult<DashboardStats>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val todayStart = todayStartMillis()
            val query = entriesRef.orderByChild("warehouseId").equalTo(warehouseId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val entries = snapshot.children.mapNotNull { it.toStockEntry() }
                        .filter { it.createdAt > todayStart }
                    
                    val todayIn = entries.filter { it.type == StockEntryType.IN }.sumOf { it.quantity }
                    val todayOut = entries.filter { it.type == StockEntryType.OUT }.sumOf { it.quantity }
                    trySend(ApiResult.Success(DashboardStats(
                        todayStockIn = todayIn,
                        todayStockOut = todayOut
                    )))
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(ApiResult.Error(error.message))
                }
            }
            query.addValueEventListener(listener)
            awaitClose { query.removeEventListener(listener) }
        }

    override fun getReportStats(warehouseId: String, days: Int): Flow<ApiResult<ReportStats>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            trySend(ApiResult.Success(ReportStats()))
            awaitClose { }
        }
}

private fun DataSnapshot.toStockItem(): StockItem? {
    return try {
        StockItem(
            productId = child("productId").getValue(String::class.java) ?: return null,
            productName = child("productName").getValue(String::class.java) ?: "",
            productSku = child("productSku").getValue(String::class.java) ?: "",
            productImageUrl = child("productImageUrl").getValue(String::class.java),
            warehouseId = child("warehouseId").getValue(String::class.java) ?: "",
            quantity = child("quantity").getValue(Double::class.java) ?: 0.0,
            minStock = child("minStock").getValue(Double::class.java) ?: 0.0,
            salePrice = child("salePrice").getValue(Double::class.java) ?: 0.0,
            lastUpdated = child("lastUpdated").getValue(Long::class.java) ?: 0L,
            updatedBy = child("updatedBy").getValue(String::class.java) ?: ""
        )
    } catch (e: Exception) { null }
}

private fun DataSnapshot.toStockEntry(): StockEntry? {
    return try {
        StockEntry(
            id = key ?: "",
            type = StockEntryType.valueOf(child("type").getValue(String::class.java) ?: "IN"),
            productId = child("productId").getValue(String::class.java) ?: return null,
            productName = child("productName").getValue(String::class.java) ?: "",
            productSku = child("productSku").getValue(String::class.java) ?: "",
            warehouseId = child("warehouseId").getValue(String::class.java) ?: "",
            quantity = child("quantity").getValue(Double::class.java) ?: 0.0,
            unitCostPrice = child("unitCostPrice").getValue(Double::class.java),
            macAfter = child("macAfter").getValue(Double::class.java),
            note = child("note").getValue(String::class.java),
            supplierId = child("supplierId").getValue(String::class.java),
            supplierName = child("supplierName").getValue(String::class.java),
            shrinkageReason = child("shrinkageReason").getValue(String::class.java)?.let {
                try { ShrinkageReason.valueOf(it) } catch (_: Exception) { null }
            },
            createdBy = child("createdBy").getValue(String::class.java) ?: "",
            createdByName = child("createdByName").getValue(String::class.java) ?: "",
            createdAt = child("createdAt").getValue(Long::class.java) ?: 0L
        )
    } catch (e: Exception) { null }
}

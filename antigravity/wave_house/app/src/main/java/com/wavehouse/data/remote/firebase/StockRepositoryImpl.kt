package com.wavehouse.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.*
import com.wavehouse.domain.repository.StockRepository
import com.wavehouse.core.utils.todayStartMillis
import com.wavehouse.core.utils.last7DaysLabels
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : StockRepository {

    override fun getStockItems(warehouseId: String): Flow<ApiResult<List<StockItem>>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val listener = firestore
                .collection("stock")
                .document(warehouseId)
                .collection("items")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(ApiResult.Error(error.message ?: "Lỗi tải tồn kho"))
                        return@addSnapshotListener
                    }
                    val items = snapshot?.documents?.mapNotNull { doc ->
                        doc.toStockItem()
                    } ?: emptyList()
                    trySend(ApiResult.Success(items))
                }
            awaitClose { listener.remove() }
        }

    override fun getLowStockItems(warehouseId: String): Flow<ApiResult<List<StockItem>>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val listener = firestore
                .collection("stock")
                .document(warehouseId)
                .collection("items")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(ApiResult.Error(error.message ?: "Lỗi tải tồn kho thấp"))
                        return@addSnapshotListener
                    }
                    val items = snapshot?.documents
                        ?.mapNotNull { it.toStockItem() }
                        ?.filter { it.stockStatus != StockStatus.IN_STOCK }
                        ?: emptyList()
                    trySend(ApiResult.Success(items))
                }
            awaitClose { listener.remove() }
        }

    override fun getStockHistory(warehouseId: String): Flow<ApiResult<List<StockEntry>>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val listener = firestore
                .collection("stock_entries")
                .whereEqualTo("warehouseId", warehouseId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(ApiResult.Error(error.message ?: "Lỗi tải lịch sử"))
                        return@addSnapshotListener
                    }
                    val entries = snapshot?.documents?.mapNotNull { it.toStockEntry() } ?: emptyList()
                    trySend(ApiResult.Success(entries))
                }
            awaitClose { listener.remove() }
        }

    override fun getProductStockHistory(
        productId: String,
        warehouseId: String
    ): Flow<ApiResult<List<StockEntry>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val listener = firestore.collection("stock_entries")
            .whereEqualTo("warehouseId", warehouseId)
            .whereEqualTo("productId", productId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(ApiResult.Error(error.message ?: "Lỗi"))
                    return@addSnapshotListener
                }
                trySend(ApiResult.Success(snapshot?.documents?.mapNotNull { it.toStockEntry() } ?: emptyList()))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createStockIn(
        productId: String,
        warehouseId: String,
        quantity: Int,
        supplierId: String?,
        note: String?
    ): ApiResult<Unit> = safeApiCall {
        firestore.runTransaction { transaction ->
            val stockRef = firestore.collection("stock")
                .document(warehouseId).collection("items").document(productId)
            val currentDoc = transaction.get(stockRef)
            val currentQty = currentDoc.getLong("quantity")?.toInt() ?: 0
            val newQty = currentQty + quantity

            // Update stock
            transaction.set(stockRef, mapOf(
                "productId" to productId,
                "warehouseId" to warehouseId,
                "quantity" to newQty,
                "lastUpdated" to System.currentTimeMillis()
            ))

            // Create entry
            val entryRef = firestore.collection("stock_entries").document()
            transaction.set(entryRef, mapOf(
                "id" to entryRef.id,
                "type" to "IN",
                "productId" to productId,
                "warehouseId" to warehouseId,
                "quantity" to quantity,
                "note" to note,
                "supplierId" to supplierId,
                "createdAt" to System.currentTimeMillis()
            ))
        }.await()
    }

    override suspend fun createStockOut(
        productId: String,
        warehouseId: String,
        quantity: Int,
        note: String?
    ): ApiResult<Unit> = safeApiCall {
        firestore.runTransaction { transaction ->
            val stockRef = firestore.collection("stock")
                .document(warehouseId).collection("items").document(productId)
            val currentDoc = transaction.get(stockRef)
            val currentQty = currentDoc.getLong("quantity")?.toInt() ?: 0
            if (quantity > currentQty) throw Exception("Tồn kho không đủ")

            transaction.update(stockRef, mapOf(
                "quantity" to (currentQty - quantity),
                "lastUpdated" to System.currentTimeMillis()
            ))

            val entryRef = firestore.collection("stock_entries").document()
            transaction.set(entryRef, mapOf(
                "id" to entryRef.id,
                "type" to "OUT",
                "productId" to productId,
                "warehouseId" to warehouseId,
                "quantity" to quantity,
                "note" to note,
                "createdAt" to System.currentTimeMillis()
            ))
        }.await()
    }

    override suspend fun adjustStock(
        productId: String,
        warehouseId: String,
        newQuantity: Int,
        note: String?
    ): ApiResult<Unit> = safeApiCall {
        val stockRef = firestore.collection("stock")
            .document(warehouseId).collection("items").document(productId)
        stockRef.update(mapOf(
            "quantity" to newQuantity,
            "lastUpdated" to System.currentTimeMillis()
        )).await()
    }

    override suspend fun createShrinkage(
        productId: String,
        warehouseId: String,
        quantity: Int,
        reason: ShrinkageReason,
        note: String?
    ): ApiResult<Unit> = safeApiCall {
        firestore.runTransaction { transaction ->
            val stockRef = firestore.collection("stock")
                .document(warehouseId).collection("items").document(productId)
            val currentDoc = transaction.get(stockRef)
            val currentQty = currentDoc.getLong("quantity")?.toInt() ?: 0
            if (quantity > currentQty) throw Exception("Tồn kho không đủ")

            transaction.update(stockRef, mapOf(
                "quantity" to (currentQty - quantity),
                "lastUpdated" to System.currentTimeMillis()
            ))

            val entryRef = firestore.collection("stock_entries").document()
            transaction.set(entryRef, mapOf(
                "id" to entryRef.id,
                "type" to "SHRINKAGE",
                "productId" to productId,
                "warehouseId" to warehouseId,
                "quantity" to quantity,
                "shrinkageReason" to reason.name,
                "note" to note,
                "createdAt" to System.currentTimeMillis()
            ))
        }.await()
    }

    override fun getTodayStats(warehouseId: String): Flow<ApiResult<DashboardStats>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val todayStart = todayStartMillis()
            val listener = firestore.collection("stock_entries")
                .whereEqualTo("warehouseId", warehouseId)
                .whereGreaterThan("createdAt", todayStart)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(ApiResult.Error(error.message ?: "Lỗi thống kê"))
                        return@addSnapshotListener
                    }
                    val entries = snapshot?.documents?.mapNotNull { it.toStockEntry() } ?: emptyList()
                    val todayIn = entries.filter { it.type == StockEntryType.IN }.sumOf { it.quantity }
                    val todayOut = entries.filter { it.type == StockEntryType.OUT }.sumOf { it.quantity }
                    trySend(ApiResult.Success(DashboardStats(
                        todayStockIn = todayIn,
                        todayStockOut = todayOut
                    )))
                }
            awaitClose { listener.remove() }
        }

    override fun getReportStats(warehouseId: String, days: Int): Flow<ApiResult<ReportStats>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            // Simplified: return empty stats, full aggregation via Cloud Functions later
            trySend(ApiResult.Success(ReportStats()))
            awaitClose { }
        }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toStockItem(): StockItem? {
    return try {
        StockItem(
            productId = getString("productId") ?: return null,
            productName = getString("productName") ?: "",
            productSku = getString("productSku") ?: "",
            productImageUrl = getString("productImageUrl"),
            warehouseId = getString("warehouseId") ?: "",
            quantity = getLong("quantity")?.toInt() ?: 0,
            minStock = getLong("minStock")?.toInt() ?: 0,
            salePrice = getDouble("salePrice") ?: 0.0,
            lastUpdated = getLong("lastUpdated") ?: 0L,
            updatedBy = getString("updatedBy") ?: ""
        )
    } catch (e: Exception) { null }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toStockEntry(): StockEntry? {
    return try {
        StockEntry(
            id = id,
            type = StockEntryType.valueOf(getString("type") ?: "IN"),
            productId = getString("productId") ?: return null,
            productName = getString("productName") ?: "",
            productSku = getString("productSku") ?: "",
            warehouseId = getString("warehouseId") ?: "",
            quantity = getLong("quantity")?.toInt() ?: 0,
            note = getString("note"),
            supplierId = getString("supplierId"),
            supplierName = getString("supplierName"),
            shrinkageReason = getString("shrinkageReason")?.let {
                try { ShrinkageReason.valueOf(it) } catch (_: Exception) { null }
            },
            createdBy = getString("createdBy") ?: "",
            createdByName = getString("createdByName") ?: "",
            createdAt = getLong("createdAt") ?: 0L
        )
    } catch (e: Exception) { null }
}

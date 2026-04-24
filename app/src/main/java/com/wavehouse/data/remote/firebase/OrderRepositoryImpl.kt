package com.wavehouse.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.*
import com.wavehouse.domain.repository.OrderRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject


class OrderRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : OrderRepository {

    private val ordersRef = database.getReference("orders")
    private val debtTransactionsRef = database.getReference("debt_transactions")

    override suspend fun createOrder(order: Order): ApiResult<String> = safeApiCall {
        val orderId = ordersRef.push().key ?: UUID.randomUUID().toString()

        // ── Bước 1: Trừ tồn kho atomic từng item (runTransaction) ─────────────
        // Lưu kết quả newQty của mỗi item sau transaction để sync product node
        val newStockQtys = mutableMapOf<String, Double>() // productId → newQty

        for (item in order.items) {
            val warehouseId = order.warehouseId
            val stockQtyRef = database.getReference("stock/$warehouseId/items/${item.productId}/quantity")

            var committedQty = 0.0
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                stockQtyRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                    override fun doTransaction(
                        currentData: com.google.firebase.database.MutableData
                    ): com.google.firebase.database.Transaction.Result {
                        val current = currentData.getValue(Double::class.java) ?: 0.0
                        val next = (current - item.quantity).coerceAtLeast(0.0)
                        currentData.value = next
                        committedQty = next
                        return com.google.firebase.database.Transaction.success(currentData)
                    }
                    override fun onComplete(
                        error: com.google.firebase.database.DatabaseError?,
                        committed: Boolean,
                        snapshot: com.google.firebase.database.DataSnapshot?
                    ) {
                        if (error != null) cont.resumeWith(Result.failure(Exception(error.message)))
                        else {
                            // Đọc giá trị thực sự committed từ snapshot (tránh stale)
                            val actual = snapshot?.getValue(Double::class.java) ?: committedQty
                            newStockQtys[item.productId] = actual
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                })
            }
        }

        // ── Bước 2: Atomic batch write — order record + sync product.currentStock ──
        // Dùng 1 updateChildren duy nhất → tất cả ghi thành công hoặc không gì cả
        val warehouseId = order.warehouseId
        val orderData = mapOf(
            "id"             to orderId,
            "warehouseId"    to warehouseId,
            "totalAmount"    to order.totalAmount,
            "paidAmount"     to order.paidAmount,
            "debtAmount"     to order.debtAmount,
            "paymentMethod"  to order.paymentMethod.name,
            "status"         to order.status.name,
            "createdBy"      to order.createdBy,
            "createdByName"  to order.createdByName,
            "createdAt"      to order.createdAt,
            "paidAt"         to order.paidAt,
            "items"          to order.items.map {
                mapOf(
                    "productId"   to it.productId,
                    "productName" to it.productName,
                    "productSku"  to it.productSku,
                    "unitPrice"   to it.unitPrice,
                    "costPrice"   to it.costPrice,
                    "quantity"    to it.quantity
                )
            }
        )

        val batchUpdates = hashMapOf<String, Any?>()
        batchUpdates["orders/$orderId"] = orderData

        // Sync mỗi product.currentStock bằng giá trị đã committed từ transaction (không đọc lại)
        for ((productId, newQty) in newStockQtys) {
            batchUpdates["stock/$warehouseId/items/$productId/lastUpdated"] = System.currentTimeMillis()
            batchUpdates["products/$productId/currentStock"] = newQty
        }

        // Ghi stock_entries (OUT) cho từng item để hiển thị trong Lịch sử giao dịch
        val now = System.currentTimeMillis()
        for (item in order.items) {
            val entryId = database.getReference("stock_entries").push().key
                ?: UUID.randomUUID().toString()
            val entryPath = "stock_entries/$entryId"
            batchUpdates["$entryPath/id"] = entryId
            batchUpdates["$entryPath/type"] = StockEntryType.OUT.name
            batchUpdates["$entryPath/productId"] = item.productId
            batchUpdates["$entryPath/productName"] = item.productName
            batchUpdates["$entryPath/productSku"] = item.productSku
            batchUpdates["$entryPath/warehouseId"] = warehouseId
            batchUpdates["$entryPath/quantity"] = item.quantity
            batchUpdates["$entryPath/unitCostPrice"] = item.costPrice
            batchUpdates["$entryPath/note"] = "Bán hàng · Đơn ${orderId.takeLast(6).uppercase()}"
            batchUpdates["$entryPath/createdBy"] = order.createdBy
            batchUpdates["$entryPath/createdByName"] = order.createdByName
            batchUpdates["$entryPath/createdAt"] = now
            batchUpdates["$entryPath/source"] = "SALE"
            batchUpdates["$entryPath/orderId"] = orderId  // ← Link về Order gốc
        }

        try {
            database.reference.updateChildren(batchUpdates).await()
        } catch (e: Exception) {
            // Rollback: order write failed after stock was already deducted via transactions.
            // Add back the deducted quantities so stock stays consistent with the missing order.
            Timber.e(e, "Order batch write failed — rolling back stock for ${order.items.size} items")
            for (item in order.items) {
                try {
                    val stockQtyRef = database.getReference("stock/$warehouseId/items/${item.productId}/quantity")
                    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                        stockQtyRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                            override fun doTransaction(
                                currentData: com.google.firebase.database.MutableData
                            ): com.google.firebase.database.Transaction.Result {
                                val current = currentData.getValue(Double::class.java) ?: 0.0
                                currentData.value = current + item.quantity
                                return com.google.firebase.database.Transaction.success(currentData)
                            }
                            override fun onComplete(
                                error: com.google.firebase.database.DatabaseError?,
                                committed: Boolean,
                                snapshot: com.google.firebase.database.DataSnapshot?
                            ) {
                                // Best-effort rollback — always resume to continue unwinding other items.
                                cont.resumeWith(Result.success(Unit))
                            }
                        })
                    }
                } catch (rollbackErr: Exception) {
                    Timber.e(rollbackErr, "Rollback failed for product ${item.productId}")
                }
            }
            throw e
        }
        Timber.d("Order created: $orderId, deducted ${order.items.size} items")

        orderId
    }


    override suspend fun confirmPayment(orderId: String): ApiResult<Unit> = safeApiCall {
        val snapshot = ordersRef.child(orderId).get().await()
        val debtAmount = snapshot.child("debtAmount").getValue(Double::class.java) ?: 0.0
        val finalStatus = if (debtAmount > 0) OrderStatus.DEBT else OrderStatus.PAID
        
        ordersRef.child(orderId).updateChildren(
            mapOf(
                "status" to finalStatus.name,
                "paidAt" to System.currentTimeMillis()
            )
        ).await()
    }

    override suspend fun payDebt(
        orderId: String,
        paymentAmount: Double,
        warehouseId: String,
        customerPhone: String,
        customerName: String?,
        paymentMethod: PaymentMethod,
        note: String?,
        createdBy: String,
        createdByName: String
    ): ApiResult<Unit> = safeApiCall {
        val snapshot = ordersRef.child(orderId).get().await()
        val currentPaid = snapshot.child("paidAmount").getValue(Double::class.java) ?: 0.0
        val currentDebt = snapshot.child("debtAmount").getValue(Double::class.java) ?: 0.0

        val newPaid = currentPaid + paymentAmount
        val newDebt = (currentDebt - paymentAmount).coerceAtLeast(0.0)
        val finalStatus = if (newDebt <= 0) OrderStatus.PAID else OrderStatus.DEBT

        // ── Build atomic batch: cập nhật Order + tạo DebtTransaction ─────
        val txnId = debtTransactionsRef.push().key ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val batchUpdates = hashMapOf<String, Any?>(
            "orders/$orderId/paidAmount" to newPaid,
            "orders/$orderId/debtAmount" to newDebt,
            "orders/$orderId/status" to finalStatus.name,
            "orders/$orderId/wasDebt" to true, // giữ dấu kể cả khi PAID
            "debt_transactions/$txnId/id" to txnId,
            "debt_transactions/$txnId/warehouseId" to warehouseId,
            "debt_transactions/$txnId/orderId" to orderId,
            "debt_transactions/$txnId/customerPhone" to customerPhone,
            "debt_transactions/$txnId/customerName" to customerName,
            "debt_transactions/$txnId/amount" to paymentAmount,
            "debt_transactions/$txnId/paymentMethod" to paymentMethod.name,
            "debt_transactions/$txnId/note" to note,
            "debt_transactions/$txnId/createdBy" to createdBy,
            "debt_transactions/$txnId/createdByName" to createdByName,
            "debt_transactions/$txnId/createdAt" to now
        )

        if (finalStatus == OrderStatus.PAID) {
            batchUpdates["orders/$orderId/paidAt"] = now
        }

        database.reference.updateChildren(batchUpdates).await()
        Timber.d("payDebt: order=$orderId paid=$paymentAmount remaining=$newDebt txn=$txnId")
    }

    override suspend fun cancelOrder(orderId: String): ApiResult<Unit> = safeApiCall {
        ordersRef.child(orderId).child("status").setValue(OrderStatus.CANCELLED.name).await()
    }

    override fun getOrders(warehouseId: String, limit: Int): Flow<ApiResult<List<Order>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val query = ordersRef.orderByChild("warehouseId").equalTo(warehouseId).limitToLast(limit)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toOrder() }
                    .sortedByDescending { it.createdAt }
                trySend(ApiResult.Success(orders))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ApiResult.Error(error.message))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun getOrderById(orderId: String): ApiResult<Order> = safeApiCall {
        val snapshot = ordersRef.child(orderId).get().await()
        snapshot.toOrder() ?: throw Exception("Không tìm thấy đơn hàng")
    }

    override fun getDebtOrders(warehouseId: String): Flow<ApiResult<List<Order>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val query = ordersRef.orderByChild("warehouseId").equalTo(warehouseId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Bao gồm cả đơn đang nợ (DEBT) và đơn đã trả hết (wasDebt=true + PAID)
                // — giúp tab "Đã thu xong" hiển được lịch sử khách đã clear nợ
                val orders = snapshot.children.mapNotNull { it.toOrder() }
                    .filter { it.status == OrderStatus.DEBT || it.wasDebt }
                    .sortedByDescending { it.createdAt }
                trySend(ApiResult.Success(orders))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ApiResult.Error(error.message))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override fun getDebtTransactions(
        warehouseId: String,
        customerPhone: String
    ): Flow<ApiResult<List<DebtTransaction>>> = callbackFlow {
        trySend(ApiResult.Loading)
        // Query bằng warehouseId index (cần thêm index trong Firebase rules nếu cần scale)
        val query = debtTransactionsRef
            .orderByChild("warehouseId")
            .equalTo(warehouseId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val txns = snapshot.children
                    .mapNotNull { it.toDebtTransaction() }
                    .filter { it.customerPhone == customerPhone }
                    .sortedByDescending { it.createdAt }
                trySend(ApiResult.Success(txns))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ApiResult.Error(error.message))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override fun getTodayOrders(warehouseId: String): Flow<ApiResult<List<Order>>> {
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        return callbackFlow {
            trySend(ApiResult.Loading)
            val query = ordersRef.orderByChild("warehouseId").equalTo(warehouseId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val orders = snapshot.children.mapNotNull { it.toOrder() }
                        .filter { it.createdAt >= startOfDay }
                        .sortedByDescending { it.createdAt }
                    trySend(ApiResult.Success(orders))
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(ApiResult.Error(error.message))
                }
            }
            query.addValueEventListener(listener)
            awaitClose { query.removeEventListener(listener) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun DataSnapshot.toOrder(): Order? {
        return try {
            val itemsRaw = child("items").value as? List<Map<String, Any>>
            val items = itemsRaw?.map {
                OrderItem(
                    productId = it["productId"] as? String ?: "",
                    productName = it["productName"] as? String ?: "",
                    productSku = it["productSku"] as? String ?: "",
                    unitPrice = (it["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                    costPrice = (it["costPrice"] as? Number)?.toDouble() ?: 0.0,
                    quantity = (it["quantity"] as? Number)?.toDouble() ?: 0.0
                )
            } ?: emptyList()

            Order(
                id = key ?: "",
                warehouseId = child("warehouseId").getValue(String::class.java) ?: "",
                items = items,
                totalAmount = child("totalAmount").getValue(Double::class.java) ?: 0.0,
                paidAmount = child("paidAmount").getValue(Double::class.java) ?: child("totalAmount").getValue(Double::class.java) ?: 0.0,
                debtAmount = child("debtAmount").getValue(Double::class.java) ?: 0.0,
                wasDebt = child("wasDebt").getValue(Boolean::class.java) ?: false,
                paymentMethod = PaymentMethod.valueOf(child("paymentMethod").getValue(String::class.java) ?: "CASH"),
                status = OrderStatus.valueOf(child("status").getValue(String::class.java) ?: "PENDING"),
                createdBy = child("createdBy").getValue(String::class.java) ?: "",
                createdByName = child("createdByName").getValue(String::class.java) ?: "",
                createdAt = child("createdAt").getValue(Long::class.java) ?: 0L,
                paidAt = child("paidAt").getValue(Long::class.java),
                customerName = child("customerName").getValue(String::class.java),
                customerPhone = child("customerPhone").getValue(String::class.java),
                note = child("note").getValue(String::class.java),
                discountAmount = child("discountAmount").getValue(Double::class.java) ?: 0.0
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse order: $key")
            null
        }
    }

    private fun DataSnapshot.toDebtTransaction(): DebtTransaction? {
        return try {
            DebtTransaction(
                id = child("id").getValue(String::class.java) ?: key ?: "",
                warehouseId = child("warehouseId").getValue(String::class.java) ?: "",
                orderId = child("orderId").getValue(String::class.java) ?: "",
                customerPhone = child("customerPhone").getValue(String::class.java) ?: "",
                customerName = child("customerName").getValue(String::class.java),
                amount = child("amount").getValue(Double::class.java) ?: 0.0,
                paymentMethod = try {
                    PaymentMethod.valueOf(child("paymentMethod").getValue(String::class.java) ?: "CASH")
                } catch (_: Exception) { PaymentMethod.CASH },
                note = child("note").getValue(String::class.java),
                createdBy = child("createdBy").getValue(String::class.java) ?: "",
                createdByName = child("createdByName").getValue(String::class.java) ?: "",
                createdAt = child("createdAt").getValue(Long::class.java) ?: 0L
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse debt_transaction: $key")
            null
        }
    }

    override suspend fun cleanupExpiredWasDebtOrders(
        warehouseId: String,
        olderThanDays: Int
    ): ApiResult<Unit> = safeApiCall {
        val cutoff = System.currentTimeMillis() - olderThanDays.toLong() * 24 * 60 * 60 * 1000

        // Query tất cả đơn của warehouse rồi filter client-side
        // (Firebase RTDB không hỗ trợ multi-field query)
        val snapshot = ordersRef
            .orderByChild("warehouseId")
            .equalTo(warehouseId)
            .get()
            .await()

        val expiredOrders = snapshot.children
            .mapNotNull { it.toOrder() }
            .filter { order ->
                order.wasDebt &&
                order.status != OrderStatus.DEBT &&
                (order.paidAt ?: 0L) < cutoff
            }

        if (expiredOrders.isEmpty()) {
            Timber.d("cleanupExpiredWasDebtOrders: nothing to clean for $warehouseId")
            return@safeApiCall
        }

        // Batch reset wasDebt = false — đơn này sẽ không còn xuất hiện trong getDebtOrders
        val batchUpdates = hashMapOf<String, Any?>()
        for (order in expiredOrders) {
            batchUpdates["orders/${order.id}/wasDebt"] = false
        }
        database.reference.updateChildren(batchUpdates).await()
        Timber.d("cleanupExpiredWasDebtOrders: reset ${expiredOrders.size} expired orders (>${olderThanDays}d)")
    }
}

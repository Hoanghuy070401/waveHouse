package com.wavehouse.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.*
import com.wavehouse.domain.repository.OrderRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : OrderRepository {

    private val ordersCollection get() = firestore.collection("orders")

    override suspend fun createOrder(order: Order): ApiResult<String> = safeApiCall {
        val batch = firestore.batch()

        // 1. Create order doc
        val orderRef = ordersCollection.document()
        val orderData = hashMapOf(
            "warehouseId" to order.warehouseId,
            "totalAmount" to order.totalAmount,
            "paymentMethod" to order.paymentMethod.name,
            "status" to order.status.name,
            "createdBy" to order.createdBy,
            "createdByName" to order.createdByName,
            "createdAt" to order.createdAt,
            "paidAt" to order.paidAt,
            "items" to order.items.map {
                hashMapOf(
                    "productId" to it.productId,
                    "productName" to it.productName,
                    "productSku" to it.productSku,
                    "unitPrice" to it.unitPrice,
                    "quantity" to it.quantity
                )
            }
        )
        batch.set(orderRef, orderData)

        // 2. Deduct stock for each item
        for (item in order.items) {
            val productRef = firestore.collection("products").document(item.productId)
            // Using FieldValue.increment for atomic decrement
            batch.update(productRef, "currentStock", com.google.firebase.firestore.FieldValue.increment(-item.quantity.toLong()))
        }

        batch.commit().addOnFailureListener { throw it }
        Timber.d("Order created: ${orderRef.id}")
        orderRef.id
    }

    override suspend fun confirmPayment(orderId: String): ApiResult<Unit> = safeApiCall {
        ordersCollection.document(orderId).update(
            mapOf(
                "status" to OrderStatus.PAID.name,
                "paidAt" to System.currentTimeMillis()
            )
        ).addOnFailureListener { throw it }
    }

    override suspend fun cancelOrder(orderId: String): ApiResult<Unit> = safeApiCall {
        ordersCollection.document(orderId).update("status", OrderStatus.CANCELLED.name)
            .addOnFailureListener { throw it }
    }

    override fun getOrders(warehouseId: String, limit: Int): Flow<ApiResult<List<Order>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val listener = ordersCollection
            .whereEqualTo("warehouseId", warehouseId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(ApiResult.Error(error.message ?: "Lỗi tải đơn hàng"))
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toOrder()
                } ?: emptyList()
                trySend(ApiResult.Success(orders))
            }
        awaitClose { listener.remove() }
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
            val listener = ordersCollection
                .whereEqualTo("warehouseId", warehouseId)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(ApiResult.Error(error.message ?: "Lỗi tải đơn hàng"))
                        return@addSnapshotListener
                    }
                    val orders = snapshot?.documents?.mapNotNull { it.toOrder() } ?: emptyList()
                    trySend(ApiResult.Success(orders))
                }
            awaitClose { listener.remove() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toOrder(): Order? {
        return try {
            val items = (get("items") as? List<Map<String, Any>>)?.map {
                OrderItem(
                    productId = it["productId"] as? String ?: "",
                    productName = it["productName"] as? String ?: "",
                    productSku = it["productSku"] as? String ?: "",
                    unitPrice = (it["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                    quantity = (it["quantity"] as? Number)?.toInt() ?: 0
                )
            } ?: emptyList()

            Order(
                id = id,
                warehouseId = getString("warehouseId") ?: "",
                items = items,
                totalAmount = getDouble("totalAmount") ?: 0.0,
                paymentMethod = PaymentMethod.valueOf(getString("paymentMethod") ?: "CASH"),
                status = OrderStatus.valueOf(getString("status") ?: "PENDING"),
                createdBy = getString("createdBy") ?: "",
                createdByName = getString("createdByName") ?: "",
                createdAt = getLong("createdAt") ?: 0L,
                paidAt = getLong("paidAt")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse order: $id")
            null
        }
    }
}

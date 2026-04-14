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
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : OrderRepository {

    private val ordersRef = database.getReference("orders")

    override suspend fun createOrder(order: Order): ApiResult<String> = safeApiCall {
        val orderId = ordersRef.push().key ?: UUID.randomUUID().toString()
        val updates = mutableMapOf<String, Any?>()
        
        val orderData = mapOf(
            "id" to orderId,
            "warehouseId" to order.warehouseId,
            "totalAmount" to order.totalAmount,
            "paymentMethod" to order.paymentMethod.name,
            "status" to order.status.name,
            "createdBy" to order.createdBy,
            "createdByName" to order.createdByName,
            "createdAt" to order.createdAt,
            "paidAt" to order.paidAt,
            "items" to order.items.map {
                mapOf(
                    "productId" to it.productId,
                    "productName" to it.productName,
                    "productSku" to it.productSku,
                    "unitPrice" to it.unitPrice,
                    "quantity" to it.quantity
                )
            }
        )
        updates["orders/$orderId"] = orderData

        // Deduct stock for each item
        for (item in order.items) {
            val productRef = database.getReference("products/${item.productId}")
            val currentStockSnap = productRef.child("currentStock").get().await()
            val currentStock = currentStockSnap.getValue(Long::class.java)?.toInt() ?: 0
            updates["products/${item.productId}/currentStock"] = currentStock - item.quantity
        }

        database.reference.updateChildren(updates).await()
        Timber.d("Order created: $orderId")
        orderId
    }

    override suspend fun confirmPayment(orderId: String): ApiResult<Unit> = safeApiCall {
        ordersRef.child(orderId).updateChildren(
            mapOf(
                "status" to OrderStatus.PAID.name,
                "paidAt" to System.currentTimeMillis()
            )
        ).await()
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
                    quantity = (it["quantity"] as? Number)?.toInt() ?: 0
                )
            } ?: emptyList()

            Order(
                id = key ?: "",
                warehouseId = child("warehouseId").getValue(String::class.java) ?: "",
                items = items,
                totalAmount = child("totalAmount").getValue(Double::class.java) ?: 0.0,
                paymentMethod = PaymentMethod.valueOf(child("paymentMethod").getValue(String::class.java) ?: "CASH"),
                status = OrderStatus.valueOf(child("status").getValue(String::class.java) ?: "PENDING"),
                createdBy = child("createdBy").getValue(String::class.java) ?: "",
                createdByName = child("createdByName").getValue(String::class.java) ?: "",
                createdAt = child("createdAt").getValue(Long::class.java) ?: 0L,
                paidAt = child("paidAt").getValue(Long::class.java)
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse order: $key")
            null
        }
    }
}

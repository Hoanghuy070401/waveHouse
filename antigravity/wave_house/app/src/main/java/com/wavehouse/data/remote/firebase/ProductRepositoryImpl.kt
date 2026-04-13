package com.wavehouse.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.Category
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.model.StockStatus
import com.wavehouse.domain.model.UnitOfMeasure
import com.wavehouse.domain.repository.ProductRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProductRepository {

    override fun getProducts(warehouseId: String): Flow<ApiResult<List<Product>>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val listener = firestore.collection("products")
                .whereEqualTo("warehouseId", warehouseId)
                .orderBy("name")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(ApiResult.Error(error.message ?: "Lỗi tải sản phẩm"))
                        return@addSnapshotListener
                    }
                    val products = snapshot?.documents?.mapNotNull { it.toProduct() } ?: emptyList()
                    trySend(ApiResult.Success(products))
                }
            awaitClose { listener.remove() }
        }

    override fun searchProducts(warehouseId: String, query: String): Flow<ApiResult<List<Product>>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            // Firestore doesn't support full-text search — client-side filter
            val listener = firestore.collection("products")
                .whereEqualTo("warehouseId", warehouseId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(ApiResult.Error(error.message ?: "Lỗi tìm kiếm"))
                        return@addSnapshotListener
                    }
                    val q = query.lowercase()
                    val products = snapshot?.documents
                        ?.mapNotNull { it.toProduct() }
                        ?.filter {
                            it.name.lowercase().contains(q)
                                    || it.sku.lowercase().contains(q)
                                    || it.barcode?.contains(q) == true
                        } ?: emptyList()
                    trySend(ApiResult.Success(products))
                }
            awaitClose { listener.remove() }
        }

    override fun getProductsByCategory(
        warehouseId: String,
        categoryId: String
    ): Flow<ApiResult<List<Product>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val listener = firestore.collection("products")
            .whereEqualTo("warehouseId", warehouseId)
            .whereEqualTo("categoryId", categoryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(ApiResult.Error(error.message ?: "Lỗi"))
                    return@addSnapshotListener
                }
                trySend(ApiResult.Success(snapshot?.documents?.mapNotNull { it.toProduct() } ?: emptyList()))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getProductById(id: String): ApiResult<Product> = safeApiCall {
        val doc = firestore.collection("products").document(id).get().await()
        doc.toProduct() ?: throw Exception("Không tìm thấy sản phẩm")
    }

    override suspend fun getProductByBarcode(barcode: String): ApiResult<Product> = safeApiCall {
        val snapshot = firestore.collection("products")
            .whereEqualTo("barcode", barcode)
            .limit(1)
            .get().await()
        snapshot.documents.firstOrNull()?.toProduct()
            ?: throw Exception("Không tìm thấy sản phẩm với barcode: $barcode")
    }

    override suspend fun createProduct(product: Product): ApiResult<String> = safeApiCall {
        val docRef = firestore.collection("products").document()
        val data = product.toMap().toMutableMap()
        data["id"] = docRef.id
        docRef.set(data).await()
        docRef.id
    }

    override suspend fun updateProduct(product: Product): ApiResult<Unit> = safeApiCall {
        firestore.collection("products").document(product.id)
            .update(product.toMap().plus("updatedAt" to System.currentTimeMillis()))
            .await()
    }

    override suspend fun deleteProduct(id: String): ApiResult<Unit> = safeApiCall {
        firestore.collection("products").document(id).delete().await()
    }

    override suspend fun uploadProductImage(
        productId: String,
        imageBytes: ByteArray
    ): ApiResult<String> {
        // TODO: Implement Firebase Storage upload
        return ApiResult.Error("Not implemented yet")
    }

    override fun getCategories(): Flow<ApiResult<List<Category>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val listener = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(ApiResult.Error(error.message ?: "Lỗi tải danh mục"))
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    Category(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null
                    )
                } ?: emptyList()
                trySend(ApiResult.Success(categories))
            }
        awaitClose { listener.remove() }
    }

    override fun getUnitsOfMeasure(): Flow<ApiResult<List<UnitOfMeasure>>> = callbackFlow {
        // Return default units if collection not populated
        trySend(ApiResult.Success(defaultUnits))
        awaitClose {}
    }
}

private val defaultUnits = listOf(
    UnitOfMeasure("1", "Cái", "cái"),
    UnitOfMeasure("2", "Thùng", "thùng"),
    UnitOfMeasure("3", "Kilogram", "kg"),
    UnitOfMeasure("4", "Lít", "l"),
    UnitOfMeasure("5", "Mét", "m"),
    UnitOfMeasure("6", "Hộp", "hộp"),
    UnitOfMeasure("7", "Gói", "gói"),
    UnitOfMeasure("8", "Bộ", "bộ"),
)

private fun com.google.firebase.firestore.DocumentSnapshot.toProduct(): Product? = try {
    Product(
        id = id,
        name = getString("name") ?: return null,
        sku = getString("sku") ?: "",
        barcode = getString("barcode"),
        categoryId = getString("categoryId"),
        categoryName = getString("categoryName"),
        unitId = getString("unitId"),
        unitName = getString("unitName"),
        description = getString("description"),
        imageUrl = getString("imageUrl"),
        minStock = getLong("minStock")?.toInt() ?: 0,
        warehouseId = getString("warehouseId") ?: "",
        currentStock = getLong("currentStock")?.toInt() ?: 0,
        createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
        updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
    )
} catch (e: Exception) { null }

private fun Product.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "sku" to sku,
    "barcode" to barcode,
    "categoryId" to categoryId,
    "categoryName" to categoryName,
    "unitId" to unitId,
    "unitName" to unitName,
    "description" to description,
    "imageUrl" to imageUrl,
    "minStock" to minStock,
    "warehouseId" to warehouseId,
    "currentStock" to currentStock,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

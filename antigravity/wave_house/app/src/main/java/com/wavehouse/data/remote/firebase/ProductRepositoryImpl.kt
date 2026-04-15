package com.wavehouse.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.network.safeApiCall
import com.wavehouse.domain.model.Category
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.model.UnitOfMeasure
import com.wavehouse.domain.repository.ProductRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage
) : ProductRepository {

    private val productsRef = database.getReference("products")
    private val categoriesRef = database.getReference("categories")

    override fun getProducts(warehouseId: String): Flow<ApiResult<List<Product>>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val query = productsRef.orderByChild("warehouseId").equalTo(warehouseId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val products = snapshot.children.mapNotNull { it.toProduct() }
                        .sortedBy { it.name }
                    trySend(ApiResult.Success(products))
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(ApiResult.Error(error.message))
                }
            }
            query.addValueEventListener(listener)
            awaitClose { query.removeEventListener(listener) }
        }

    override fun searchProducts(warehouseId: String, query: String): Flow<ApiResult<List<Product>>> =
        callbackFlow {
            trySend(ApiResult.Loading)
            val dbQuery = productsRef.orderByChild("warehouseId").equalTo(warehouseId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val q = query.lowercase()
                    val products = snapshot.children
                        .mapNotNull { it.toProduct() }
                        .filter {
                            it.name.lowercase().contains(q)
                                    || it.sku.lowercase().contains(q)
                                    || it.barcode?.contains(q) == true
                        }
                    trySend(ApiResult.Success(products))
                }
                override fun onCancelled(error: DatabaseError) {
                    trySend(ApiResult.Error(error.message))
                }
            }
            dbQuery.addValueEventListener(listener)
            awaitClose { dbQuery.removeEventListener(listener) }
        }

    override fun getProductsByCategory(
        warehouseId: String,
        categoryId: String
    ): Flow<ApiResult<List<Product>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val query = productsRef.orderByChild("warehouseId").equalTo(warehouseId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = snapshot.children.mapNotNull { it.toProduct() }
                    .filter { it.categoryId == categoryId }
                trySend(ApiResult.Success(products))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ApiResult.Error(error.message))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun getProductById(id: String): ApiResult<Product> = safeApiCall {
        val snapshot = productsRef.child(id).get().await()
        snapshot.toProduct() ?: throw Exception("Không tìm thấy sản phẩm")
    }

    override suspend fun getProductByBarcode(barcode: String): ApiResult<Product> = safeApiCall {
        val snapshot = productsRef.orderByChild("barcode").equalTo(barcode).limitToFirst(1).get().await()
        snapshot.children.firstOrNull()?.toProduct()
            ?: throw Exception("Không tìm thấy sản phẩm với barcode: $barcode")
    }

    override suspend fun createProduct(product: Product): ApiResult<String> = safeApiCall {
        // Đảm bảo có ID
        val id = product.id.takeIf { it.isNotEmpty() } ?: productsRef.push().key ?: UUID.randomUUID().toString()
        val p = product.copy(id = id)
        
        productsRef.child(id).setValue(p.toMap()).await()
        id
    }

    override suspend fun updateProduct(product: Product): ApiResult<Unit> = safeApiCall {
        productsRef.child(product.id).updateChildren(
            product.toMap().plus("updatedAt" to System.currentTimeMillis())
        ).await()
    }

    override suspend fun deleteProduct(id: String): ApiResult<Unit> = safeApiCall {
        productsRef.child(id).removeValue().await()
    }

    override suspend fun uploadProductImage(
        productId: String,
        imageBytes: ByteArray
    ): ApiResult<String> = safeApiCall {
        val ref = storage.reference.child("product_images/${productId}_${System.currentTimeMillis()}.jpg")
        ref.putBytes(imageBytes).await()
        ref.downloadUrl.await().toString()
    }

    override fun getCategories(): Flow<ApiResult<List<Category>>> = callbackFlow {
        trySend(ApiResult.Loading)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categories = snapshot.children.mapNotNull { doc ->
                    val name = doc.child("name").getValue(String::class.java) ?: return@mapNotNull null
                    Category(id = doc.key ?: "", name = name)
                }
                trySend(ApiResult.Success(categories))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(ApiResult.Error(error.message))
            }
        }
        categoriesRef.addValueEventListener(listener)
        awaitClose { categoriesRef.removeEventListener(listener) }
    }

    override fun getUnitsOfMeasure(): Flow<ApiResult<List<UnitOfMeasure>>> = callbackFlow {
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

private fun DataSnapshot.toProduct(): Product? {
    return try {
        // Because fields might be null or stored differently, we read them dynamically
        Product(
            id = key ?: return null,
            name = child("name").getValue(String::class.java) ?: return null,
            sku = child("sku").getValue(String::class.java) ?: "",
            barcode = child("barcode").getValue(String::class.java),
            categoryId = child("categoryId").getValue(String::class.java),
            categoryName = child("categoryName").getValue(String::class.java),
            unitId = child("unitId").getValue(String::class.java),
            unitName = child("unitName").getValue(String::class.java),
            description = child("description").getValue(String::class.java),
            imageUrl = child("imageUrl").getValue(String::class.java),
            costPrice = child("costPrice").getValue(Double::class.java) ?: 0.0,
            salePrice = child("salePrice").getValue(Double::class.java) ?: 0.0,
            minStock = child("minStock").getValue(Double::class.java) ?: 0.0,
            warehouseId = child("warehouseId").getValue(String::class.java) ?: "",
            currentStock = child("currentStock").getValue(Double::class.java) ?: 0.0,
            createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
            updatedAt = child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
        )
    } catch (e: Exception) { null }
}

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
    "costPrice" to costPrice,
    "salePrice" to salePrice,
    "minStock" to minStock,
    "warehouseId" to warehouseId,
    "currentStock" to currentStock,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

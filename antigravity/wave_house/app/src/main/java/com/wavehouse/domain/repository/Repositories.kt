package com.wavehouse.domain.repository

import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.model.Category
import com.wavehouse.domain.model.UnitOfMeasure
import kotlinx.coroutines.flow.Flow

/** Repository interface cho Product */
interface ProductRepository {

    fun getProducts(warehouseId: String): Flow<ApiResult<List<Product>>>

    fun searchProducts(warehouseId: String, query: String): Flow<ApiResult<List<Product>>>

    fun getProductsByCategory(warehouseId: String, categoryId: String): Flow<ApiResult<List<Product>>>

    suspend fun getProductById(id: String): ApiResult<Product>

    suspend fun getProductByBarcode(barcode: String): ApiResult<Product>

    suspend fun createProduct(product: Product): ApiResult<String>

    suspend fun updateProduct(product: Product): ApiResult<Unit>

    suspend fun deleteProduct(id: String): ApiResult<Unit>

    suspend fun uploadProductImage(productId: String, imageBytes: ByteArray): ApiResult<String>

    fun getCategories(): Flow<ApiResult<List<Category>>>

    fun getUnitsOfMeasure(): Flow<ApiResult<List<UnitOfMeasure>>>
}

/** Repository interface cho Stock */
interface StockRepository {

    fun getStockItems(warehouseId: String): Flow<ApiResult<List<com.wavehouse.domain.model.StockItem>>>

    fun getLowStockItems(warehouseId: String): Flow<ApiResult<List<com.wavehouse.domain.model.StockItem>>>

    fun getStockHistory(warehouseId: String): Flow<ApiResult<List<com.wavehouse.domain.model.StockEntry>>>

    fun getProductStockHistory(productId: String, warehouseId: String): Flow<ApiResult<List<com.wavehouse.domain.model.StockEntry>>>

    suspend fun createStockIn(
        productId: String,
        warehouseId: String,
        quantity: Int,
        supplierId: String?,
        note: String?
    ): ApiResult<Unit>

    suspend fun createStockOut(
        productId: String,
        warehouseId: String,
        quantity: Int,
        note: String?
    ): ApiResult<Unit>

    suspend fun adjustStock(
        productId: String,
        warehouseId: String,
        newQuantity: Int,
        note: String?
    ): ApiResult<Unit>

    fun getTodayStats(warehouseId: String): Flow<ApiResult<com.wavehouse.domain.model.DashboardStats>>
}

/** Repository interface cho Supplier */
interface SupplierRepository {

    fun getSuppliers(): Flow<ApiResult<List<com.wavehouse.domain.model.Supplier>>>

    suspend fun getSupplierById(id: String): ApiResult<com.wavehouse.domain.model.Supplier>

    suspend fun createSupplier(supplier: com.wavehouse.domain.model.Supplier): ApiResult<String>

    suspend fun updateSupplier(supplier: com.wavehouse.domain.model.Supplier): ApiResult<Unit>

    suspend fun deleteSupplier(id: String): ApiResult<Unit>
}

package com.wavehouse.domain.repository

import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.*
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

    suspend fun updateProductPrice(
        productId: String,
        costPrice: Double,
        salePrice: Double,
        updatedBy: String,
        updatedByName: String?,
        reason: String?
    ): ApiResult<Unit>

    fun getProductPriceHistory(productId: String): Flow<ApiResult<List<PriceRecord>>>

    suspend fun deleteProduct(id: String): ApiResult<Unit>

    suspend fun uploadProductImage(productId: String, imageBytes: ByteArray): ApiResult<String>

    fun getCategories(): Flow<ApiResult<List<Category>>>

    fun getUnitsOfMeasure(): Flow<ApiResult<List<UnitOfMeasure>>>
}

/** Repository interface cho Stock */
interface StockRepository {

    fun getStockItems(warehouseId: String): Flow<ApiResult<List<StockItem>>>

    fun getLowStockItems(warehouseId: String): Flow<ApiResult<List<StockItem>>>

    fun getStockHistory(warehouseId: String): Flow<ApiResult<List<StockEntry>>>

    fun getProductStockHistory(productId: String, warehouseId: String): Flow<ApiResult<List<StockEntry>>>

    suspend fun createStockIn(
        productId: String,
        warehouseId: String,
        quantity: Double,
        unitCostPrice: Double? = null,   // Giá nhập lô này → dùng tính MAC
        supplierId: String?,
        note: String?
    ): ApiResult<Unit>

    suspend fun createStockOut(
        productId: String,
        warehouseId: String,
        quantity: Double,
        note: String?
    ): ApiResult<Unit>

    suspend fun createShrinkage(
        productId: String,
        warehouseId: String,
        quantity: Double,
        reason: ShrinkageReason,
        note: String?
    ): ApiResult<Unit>

    suspend fun adjustStock(
        productId: String,
        warehouseId: String,
        newQuantity: Double,
        note: String?
    ): ApiResult<Unit>

    fun getTodayStats(warehouseId: String): Flow<ApiResult<DashboardStats>>

    fun getReportStats(warehouseId: String, days: Int): Flow<ApiResult<ReportStats>>
}

/** Repository interface cho Supplier */
interface SupplierRepository {

    fun getSuppliers(warehouseId: String): Flow<ApiResult<List<Supplier>>>

    suspend fun getSupplierById(id: String): ApiResult<Supplier>

    suspend fun createSupplier(supplier: Supplier): ApiResult<String>

    suspend fun updateSupplier(supplier: Supplier): ApiResult<Unit>

    suspend fun deleteSupplier(id: String): ApiResult<Unit>
}

/** Repository interface cho Order (POS) */
interface OrderRepository {

    suspend fun createOrder(order: Order): ApiResult<String>

    suspend fun confirmPayment(orderId: String): ApiResult<Unit>

    suspend fun cancelOrder(orderId: String): ApiResult<Unit>

    fun getOrders(warehouseId: String, limit: Int = 50): Flow<ApiResult<List<Order>>>

    suspend fun getOrderById(orderId: String): ApiResult<Order>

    fun getTodayOrders(warehouseId: String): Flow<ApiResult<List<Order>>>
}

/** Repository interface cho Warehouse */
interface WarehouseRepository {

    fun getWarehouses(userId: String): Flow<ApiResult<List<Warehouse>>>

    suspend fun getWarehouseById(id: String): ApiResult<Warehouse>

    suspend fun switchWarehouse(userId: String, warehouseId: String): ApiResult<Unit>
}

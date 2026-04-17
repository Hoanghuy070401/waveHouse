package com.wavehouse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wavehouse.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE warehouseId = :warehouseId ORDER BY name ASC")
    fun getProducts(warehouseId: String): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products 
        WHERE warehouseId = :warehouseId 
        AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun searchProducts(warehouseId: String, query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getUnsyncedProducts(): List<ProductEntity>
}

@Dao
interface StockDao {

    @Query("SELECT * FROM stock_items WHERE warehouseId = :warehouseId ORDER BY productId ASC")
    fun getStockItems(warehouseId: String): Flow<List<com.wavehouse.data.local.entity.StockItemEntity>>

    @Query("SELECT * FROM stock_items WHERE productId = :productId AND warehouseId = :warehouseId LIMIT 1")
    suspend fun getStockItem(productId: String, warehouseId: String): com.wavehouse.data.local.entity.StockItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStockItem(item: com.wavehouse.data.local.entity.StockItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStockItems(items: List<com.wavehouse.data.local.entity.StockItemEntity>)
}

@Dao
interface StockEntryDao {

    @Query("""
        SELECT * FROM stock_entries 
        WHERE warehouseId = :warehouseId 
        ORDER BY createdAt DESC
    """)
    fun getStockEntries(warehouseId: String): Flow<List<com.wavehouse.data.local.entity.StockEntryEntity>>

    @Query("""
        SELECT * FROM stock_entries 
        WHERE warehouseId = :warehouseId AND type = :type
        ORDER BY createdAt DESC
    """)
    fun getStockEntriesByType(warehouseId: String, type: String): Flow<List<com.wavehouse.data.local.entity.StockEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockEntry(entry: com.wavehouse.data.local.entity.StockEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockEntries(entries: List<com.wavehouse.data.local.entity.StockEntryEntity>)

    @Query("SELECT * FROM stock_entries WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedEntries(): List<com.wavehouse.data.local.entity.StockEntryEntity>
}

@Dao
interface SupplierDao {

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getSuppliers(): Flow<List<com.wavehouse.data.local.entity.SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: String): com.wavehouse.data.local.entity.SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: com.wavehouse.data.local.entity.SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<com.wavehouse.data.local.entity.SupplierEntity>)

    @Update
    suspend fun updateSupplier(supplier: com.wavehouse.data.local.entity.SupplierEntity)

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteSupplier(id: String)
}

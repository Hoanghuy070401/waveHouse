package com.wavehouse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String,
    val barcode: String?,
    val categoryId: String?,
    val unitId: String?,
    val description: String?,
    val imageUrl: String?,
    val minStock: Int,
    val warehouseId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean = true
)

@Entity(tableName = "stock_items", primaryKeys = ["productId", "warehouseId"])
data class StockItemEntity(
    val productId: String,
    val warehouseId: String,
    val quantity: Int,
    val lastUpdated: Long,
    val updatedBy: String,
    val isSynced: Boolean = true
)

@Entity(tableName = "stock_entries")
data class StockEntryEntity(
    @PrimaryKey val id: String,
    val type: String, // IN, OUT, ADJUST, TRANSFER
    val productId: String,
    val warehouseId: String,
    val quantity: Int,
    val note: String?,
    val supplierId: String?,
    val createdBy: String,
    val createdAt: Long,
    val isSynced: Boolean = true
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val address: String?,
    val createdAt: Long,
    val isSynced: Boolean = true
)

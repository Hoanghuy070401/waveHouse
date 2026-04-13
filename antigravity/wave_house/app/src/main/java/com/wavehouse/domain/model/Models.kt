package com.wavehouse.domain.model

/** Vai trò người dùng trong hệ thống */
enum class UserRole {
    ADMIN,       // Toàn quyền
    WAREHOUSE,   // Thủ kho: nhập/xuất kho
    ACCOUNTANT,  // Kế toán: xem báo cáo
    STAFF        // Nhân viên: xem sản phẩm
}

/** Domain model: User */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val warehouseId: String,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN
    val canEditStock: Boolean get() = role == UserRole.ADMIN || role == UserRole.WAREHOUSE
    val canViewReports: Boolean get() = role == UserRole.ADMIN || role == UserRole.ACCOUNTANT
    val initials: String get() = name.split(" ")
        .filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }
}

/** Domain model: Product */
data class Product(
    val id: String,
    val name: String,
    val sku: String,
    val barcode: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val unitId: String? = null,
    val unitName: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val minStock: Int = 0,
    val warehouseId: String,
    val currentStock: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val stockStatus: StockStatus get() = when {
        currentStock <= 0 -> StockStatus.OUT_OF_STOCK
        currentStock <= minStock -> StockStatus.LOW_STOCK
        else -> StockStatus.IN_STOCK
    }
}

enum class StockStatus { IN_STOCK, LOW_STOCK, OUT_OF_STOCK }

/** Domain model: Category */
data class Category(
    val id: String,
    val name: String,
    val parentId: String? = null
)

/** Domain model: Unit of Measure */
data class UnitOfMeasure(
    val id: String,
    val name: String,   // "Cái", "Thùng", "Kg", "Lít"
    val abbreviation: String // "cái", "thùng", "kg", "l"
)

/** Domain model: StockItem (tồn kho tại một kho) */
data class StockItem(
    val productId: String,
    val productName: String,
    val productSku: String,
    val productImageUrl: String?,
    val warehouseId: String,
    val quantity: Int,
    val minStock: Int,
    val lastUpdated: Long,
    val updatedBy: String
) {
    val stockStatus: StockStatus get() = when {
        quantity <= 0 -> StockStatus.OUT_OF_STOCK
        quantity <= minStock -> StockStatus.LOW_STOCK
        else -> StockStatus.IN_STOCK
    }
}

/** Loại giao dịch kho */
enum class StockEntryType(val label: String, val isIncoming: Boolean) {
    IN("Nhập kho", true),
    OUT("Xuất kho", false),
    ADJUST("Điều chỉnh", true),
    TRANSFER("Chuyển kho", false)
}

/** Domain model: StockEntry (phiếu nhập/xuất) */
data class StockEntry(
    val id: String,
    val type: StockEntryType,
    val productId: String,
    val productName: String,
    val productSku: String,
    val warehouseId: String,
    val quantity: Int,
    val note: String? = null,
    val supplierId: String? = null,
    val supplierName: String? = null,
    val createdBy: String,
    val createdByName: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Domain model: Supplier */
data class Supplier(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Domain model: Warehouse */
data class Warehouse(
    val id: String,
    val name: String,
    val address: String? = null,
    val managerId: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Dashboard KPIs */
data class DashboardStats(
    val totalProducts: Int = 0,
    val totalStockValue: Long = 0L,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val todayStockIn: Int = 0,
    val todayStockOut: Int = 0,
    val weeklyInData: List<Int> = emptyList(),
    val weeklyOutData: List<Int> = emptyList()
)

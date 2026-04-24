package com.wavehouse.domain.model

/** Vai trò người dùng trong hệ thống */
enum class UserRole {
    ADMIN,       // Toàn quyền (= Owner trong PRD)
    WAREHOUSE,   // Thủ kho: nhập/xuất kho
    ACCOUNTANT,  // Kế toán: xem báo cáo
    STAFF        // Nhân viên: chỉ bán hàng + xem kho cơ bản
}

/** Trạng thái tài khoản nhân viên */
enum class UserStatus {
    ACTIVE,   // Đã được Admin duyệt
    PENDING   // Đang chờ Admin duyệt (nhân viên mới đăng ký)
}

/** Domain model: User */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val warehouseId: String,
    val status: UserStatus = UserStatus.ACTIVE,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN
    val canEditStock: Boolean get() = role == UserRole.ADMIN || role == UserRole.WAREHOUSE
    val canEditPrice: Boolean get() = role == UserRole.ADMIN
    val canViewReports: Boolean get() = role == UserRole.ADMIN || role == UserRole.ACCOUNTANT
    val canManageStaff: Boolean get() = role == UserRole.ADMIN
    val canSell: Boolean get() = true // Tất cả role đều bán được
    val initials: String get() = name.split(" ")
        .filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }
}

// ═══════════════════════════════════════════════════════════════
// PRODUCT
// ═══════════════════════════════════════════════════════════════

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
    val costPrice: Double = 0.0,       // Giá vốn (MAC — tự động cập nhật)
    val salePrice: Double = 0.0,       // Giá bán
    val minStock: Double = 0.0,
    val warehouseId: String,
    val currentStock: Double = 0.0,
    /** Trọng lượng quy đổi: 1 bó/thùng/túi = ? kg. null = đơn vị là kg (không cần quy đổi) */
    val weightPerUnit: Double? = null,
    /** Cho phép bán lẻ (số thập phân). false = chỉ bán số nguyên (bó, thùng, tú i) */
    val allowDecimal: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val stockStatus: StockStatus get() = when {
        currentStock <= 0.0 -> StockStatus.OUT_OF_STOCK
        currentStock <= minStock -> StockStatus.LOW_STOCK
        else -> StockStatus.IN_STOCK
    }

    /** Giá trị tồn kho = số lượng × giá vốn (MAC) */
    val stockValue: Double get() = currentStock * costPrice

    /** Tổng trọng lượng tồn kho quy về kg (để tính hao hụt) */
    val stockWeightKg: Double? get() = weightPerUnit?.let { currentStock * it }
}

/** Domain model: PriceRecord (Lịch sử giá) */
data class PriceRecord(
    val id: String,
    val costPrice: Double,
    val salePrice: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedBy: String,
    val updatedByName: String? = null,
    val reason: String? = null
)

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

// ═══════════════════════════════════════════════════════════════
// STOCK
// ═══════════════════════════════════════════════════════════════

/** Domain model: StockItem (tồn kho tại một kho) */
data class StockItem(
    val productId: String,
    val productName: String,
    val productSku: String,
    val productImageUrl: String?,
    val warehouseId: String,
    val quantity: Double,
    val minStock: Double,
    val salePrice: Double = 0.0,
    val lastUpdated: Long,
    val updatedBy: String
) {
    val stockStatus: StockStatus get() = when {
        quantity <= 0.0 -> StockStatus.OUT_OF_STOCK
        quantity <= minStock -> StockStatus.LOW_STOCK
        else -> StockStatus.IN_STOCK
    }
}

/** Loại giao dịch kho */
enum class StockEntryType(val label: String, val isIncoming: Boolean) {
    IN("Nhập kho", true),
    OUT("Xuất kho", false),
    ADJUST("Điều chỉnh", true),
    TRANSFER("Chuyển kho", false),
    SHRINKAGE("Hao hụt", false)    // Hàng hỏng, hết hạn, hủy
}

/** Lý do hao hụt */
enum class ShrinkageReason(val label: String) {
    EXPIRED("Hết hạn sử dụng"),
    DAMAGED("Hư hỏng / Dập nát"),
    QUALITY("Không đạt chất lượng"),
    DONATED("Tặng từ thiện"),
    OTHER("Lý do khác")
}

/** Domain model: StockEntry (phiếu nhập/xuất) */
data class StockEntry(
    val id: String,
    val type: StockEntryType,
    val productId: String,
    val productName: String,
    val productSku: String,
    /** URL ảnh sản phẩm — snapshot khi tạo phiếu (để hiển thị trong lịch sử dù sau này sản phẩm bị xoá) */
    val productImageUrl: String? = null,
    val warehouseId: String,
    val quantity: Double,
    /** Giá nhập lô này (dùng để tính MAC). null = không ghi nhận giá */
    val unitCostPrice: Double? = null,
    /** Giá vốn bình quân SAU KHI nhập lô này (kết quả MAC) */
    val macAfter: Double? = null,
    val note: String? = null,
    val supplierId: String? = null,
    val supplierName: String? = null,
    val shrinkageReason: ShrinkageReason? = null,  // Chỉ cho type = SHRINKAGE
    val createdBy: String,
    val createdByName: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Nguồn gốc giao dịch: SALE (từ POS), MANUAL (thủ công), ADJUST, SHRINKAGE */
    val source: String? = null,
    /** ID đơn hàng gốc — chỉ có khi source=SALE, dùng để điều hướng sang OrderDetailScreen */
    val orderId: String? = null
)

// ═══════════════════════════════════════════════════════════════
// SUPPLIER
// ═══════════════════════════════════════════════════════════════

/** Domain model: Supplier */
data class Supplier(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val warehouseId: String,
    val createdAt: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════
// POS / ORDER
// ═══════════════════════════════════════════════════════════════

/** Phương thức thanh toán */
enum class PaymentMethod(val label: String) {
    CASH("Tiền mặt"),
    QR("QR Code")
}

/** Trạng thái đơn hàng */
enum class OrderStatus(val label: String) {
    PENDING("Chờ thanh toán"),
    PAID("Đã thanh toán"),
    DEBT("Ghi nợ"),
    CANCELLED("Đã huỷ")
}

/** Cart item — inmemory only, ko persist */
data class CartItem(
    val product: Product,
    val quantity: Double
) {
    val lineTotal: Double get() = product.salePrice * quantity
}

/** Domain model: Order */
data class Order(
    val id: String,
    val warehouseId: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val status: OrderStatus = OrderStatus.PENDING,
    val paidAmount: Double = totalAmount,
    val debtAmount: Double = 0.0,
    /** Đánh dấu đơn này từng ở trạng thái DEBT — giữ lại để query lịch sử kể cả khi đã PAID */
    val wasDebt: Boolean = false,
    val createdBy: String,
    val createdByName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    /** Tên khách hàng — null = khách lẻ vãng lai */
    val customerName: String? = null,
    /** SĐT khách hàng — dùng cho tìm kiếm & lọc lịch sử */
    val customerPhone: String? = null,
    /** Ghi chú đơn hàng (yêu cầu đặc biệt, giao hàng…) */
    val note: String? = null,
    /** Chiết khấu/giảm giá trực tiếp trên đơn (không tính trong items) */
    val discountAmount: Double = 0.0
) {
    val itemCount: Int get() = items.sumOf { it.quantity }.toInt()
    /** Subtotal trước giảm giá */
    val subtotal: Double get() = items.sumOf { it.lineTotal }
}

/** Domain model: OrderItem snapshot (serialized in Firestore)
 *  Đóng băng cả giá bán (unitPrice) lẫn giá vốn (costPrice) tại thời điểm tạo đơn
 *  để báo cáo lãi/lỗ luôn chính xác dù giá sau này thay đổi.
 */
data class OrderItem(
    val productId: String,
    val productName: String,
    val productSku: String,
    val unitPrice: Double,          // Giá bán → snapshot cứng
    val costPrice: Double = 0.0,    // Giá vốn (MAC) → snapshot cứng
    val quantity: Double
) {
    val lineTotal: Double get() = unitPrice * quantity
    val lineCost: Double get() = costPrice * quantity
    val lineProfit: Double get() = lineTotal - lineCost
}

// ═══════════════════════════════════════════════════════════════
// DEBT TRANSACTION (Lịch sử thu nợ)
// ═══════════════════════════════════════════════════════════════

/**
 * Một lần thu nợ — ghi lại từng khoản thanh toán công nợ.
 * Không thay thế Order; chỉ là audit log cho lịch sử.
 */
data class DebtTransaction(
    val id: String,
    val warehouseId: String,
    /** ID đơn hàng được thu nợ (nhiều DebtTransaction có thể cùng orderId khi trả nhiều lần) */
    val orderId: String,
    val customerPhone: String,
    val customerName: String? = null,
    /** Số tiền thực thu trong lần này */
    val amount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String? = null,
    val createdBy: String,
    val createdByName: String,
    val createdAt: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════════════
// WAREHOUSE
// ═══════════════════════════════════════════════════════════════

/** Domain model: Warehouse */
data class Warehouse(
    val id: String,
    val name: String,
    val joinCode: String = "",     // Mã 6 ký tự để nhân viên tham gia (VD: ABC123)
    val address: String? = null,
    val managerId: String,
    val status: WarehouseStatus = WarehouseStatus.ACTIVE,
    val memberCount: Int = 0,
    /** URL ảnh mã QR nhận thanh toán do chủ cửa hàng cung cấp */
    val qrImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class WarehouseStatus(val label: String) {
    ACTIVE("Đang hoạt động"),
    INACTIVE("Tạm ngưng")
}

// ═══════════════════════════════════════════════════════════════
// DASHBOARD / REPORT
// ═══════════════════════════════════════════════════════════════

/** Dashboard KPIs */
data class DashboardStats(
    val totalProducts: Int = 0,
    val totalStockValue: Long = 0L,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val todayOrders: Int = 0,
    val todayStockIn: Double = 0.0,
    val todayStockOut: Double = 0.0,
    val revenueChangePercent: Double = 0.0,  // +12% so với hôm qua
    val weeklyRevenueData: List<Double> = emptyList(),
    val weeklyInData: List<Int> = emptyList(),
    val weeklyOutData: List<Int> = emptyList()
)

/** Report aggregate — Owner only */
data class ReportStats(
    val totalRevenue: Double = 0.0,
    val profit: Double = 0.0,
    val shrinkageRate: Double = 0.0,      // Tỷ lệ hao hụt (%)
    val shrinkageChangePercent: Double = 0.0,
    val revenueByDay: List<Pair<String, Double>> = emptyList(),
    val shrinkageByDay: List<Pair<String, Double>> = emptyList(),
    val topSellingProducts: List<TopProduct> = emptyList(),
    val recentShrinkageEntries: List<StockEntry> = emptyList()
)

data class TopProduct(
    val productId: String,
    val productName: String,
    val productSku: String,
    val productImageUrl: String?,
    val origin: String? = null,
    val quantitySold: Double,
    val unitName: String
)

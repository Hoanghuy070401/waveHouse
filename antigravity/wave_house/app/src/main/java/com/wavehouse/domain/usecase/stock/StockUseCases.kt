package com.wavehouse.domain.usecase.stock

import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.DashboardStats
import com.wavehouse.domain.model.ReportStats
import com.wavehouse.domain.model.ShrinkageReason
import com.wavehouse.domain.model.StockEntry
import com.wavehouse.domain.model.StockItem
import com.wavehouse.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStockItemsUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    operator fun invoke(warehouseId: String): Flow<ApiResult<List<StockItem>>> =
        stockRepository.getStockItems(warehouseId)
}

class GetLowStockAlertsUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    operator fun invoke(warehouseId: String): Flow<ApiResult<List<StockItem>>> =
        stockRepository.getLowStockItems(warehouseId)
}

class GetStockHistoryUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    operator fun invoke(warehouseId: String): Flow<ApiResult<List<StockEntry>>> =
        stockRepository.getStockHistory(warehouseId)
}

class CreateStockInUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    suspend operator fun invoke(
        productId: String,
        warehouseId: String,
        quantity: Double,
        supplierId: String? = null,
        note: String? = null
    ): ApiResult<Unit> {
        if (quantity <= 0.0) return ApiResult.Error("Số lượng nhập phải lớn hơn 0")
        return stockRepository.createStockIn(productId, warehouseId, quantity, supplierId, note)
    }
}

class CreateStockOutUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    suspend operator fun invoke(
        productId: String,
        warehouseId: String,
        quantity: Double,
        currentStock: Double,
        note: String? = null
    ): ApiResult<Unit> {
        if (quantity <= 0.0) return ApiResult.Error("Số lượng xuất phải lớn hơn 0")
        if (quantity > currentStock) return ApiResult.Error(
            "Số tồn trong kho không đủ, hiện tại tồn ${formatQty(currentStock)}"
        )
        return stockRepository.createStockOut(productId, warehouseId, quantity, note)
    }
}

class CreateShrinkageUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    suspend operator fun invoke(
        productId: String,
        warehouseId: String,
        quantity: Double,
        currentStock: Double,
        reason: ShrinkageReason,
        note: String? = null
    ): ApiResult<Unit> {
        if (quantity <= 0.0) return ApiResult.Error("Số lượng hao hụt phải lớn hơn 0")
        if (quantity > currentStock) return ApiResult.Error(
            "Số tồn trong kho không đủ, hiện tại tồn ${formatQty(currentStock)}"
        )
        return stockRepository.createShrinkage(productId, warehouseId, quantity, reason, note)
    }
}

class GetDashboardStatsUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    operator fun invoke(warehouseId: String): Flow<ApiResult<DashboardStats>> =
        stockRepository.getTodayStats(warehouseId)
}

class GetReportStatsUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    operator fun invoke(warehouseId: String, days: Int = 7): Flow<ApiResult<ReportStats>> =
        stockRepository.getReportStats(warehouseId, days)
}

/** Format qty for display: show decimal only when needed */
private fun formatQty(qty: Double): String =
    if (qty % 1.0 == 0.0) qty.toLong().toString() else qty.toString()

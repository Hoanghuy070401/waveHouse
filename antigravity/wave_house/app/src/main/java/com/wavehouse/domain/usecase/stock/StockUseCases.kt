package com.wavehouse.domain.usecase.stock

import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.DashboardStats
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
        quantity: Int,
        supplierId: String? = null,
        note: String? = null
    ): ApiResult<Unit> {
        if (quantity <= 0) return ApiResult.Error("Số lượng nhập phải lớn hơn 0")
        return stockRepository.createStockIn(productId, warehouseId, quantity, supplierId, note)
    }
}

class CreateStockOutUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    suspend operator fun invoke(
        productId: String,
        warehouseId: String,
        quantity: Int,
        currentStock: Int,
        note: String? = null
    ): ApiResult<Unit> {
        if (quantity <= 0) return ApiResult.Error("Số lượng xuất phải lớn hơn 0")
        if (quantity > currentStock) return ApiResult.Error(
            "Số tồn trong kho không đủ, hiện tại tồn $currentStock"
        )
        return stockRepository.createStockOut(productId, warehouseId, quantity, note)
    }
}

class GetDashboardStatsUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    operator fun invoke(warehouseId: String): Flow<ApiResult<DashboardStats>> =
        stockRepository.getTodayStats(warehouseId)
}

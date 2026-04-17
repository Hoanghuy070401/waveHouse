package com.wavehouse.domain.usecase.product

import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(warehouseId: String): Flow<ApiResult<List<Product>>> =
        productRepository.getProducts(warehouseId)
}

class SearchProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(warehouseId: String, query: String): Flow<ApiResult<List<Product>>> =
        productRepository.searchProducts(warehouseId, query)
}

class GetProductByBarcodeUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(barcode: String): ApiResult<Product> =
        productRepository.getProductByBarcode(barcode)
}

class GetProductByIdUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(id: String): ApiResult<Product> =
        productRepository.getProductById(id)
}

class CreateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(product: Product): ApiResult<String> {
        if (product.name.isBlank()) return ApiResult.Error("Tên sản phẩm không được để trống")
        if (product.sku.isBlank()) return ApiResult.Error("Mã SKU không được để trống")
        return productRepository.createProduct(product)
    }
}

class UpdateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(product: Product): ApiResult<Unit> {
        if (product.name.isBlank()) return ApiResult.Error("Tên sản phẩm không được để trống")
        return productRepository.updateProduct(product)
    }
}

class DeleteProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(id: String): ApiResult<Unit> =
        productRepository.deleteProduct(id)
}

class UpdateProductPriceUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(
        productId: String,
        costPrice: Double,
        salePrice: Double,
        updatedBy: String,
        updatedByName: String?,
        reason: String?
    ): ApiResult<Unit> {
        if (costPrice < 0 || salePrice < 0) return ApiResult.Error("Giá trị không hợp lệ")
        return productRepository.updateProductPrice(
            productId, costPrice, salePrice, updatedBy, updatedByName, reason
        )
    }
}

class GetProductPriceHistoryUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(productId: String) = productRepository.getProductPriceHistory(productId)
}

# Phase 1: Domain & Data Layer

## Objective
Establish the foundational models and database operations for Price History tracking.

## Tasks

### 1. Domain Models
- Create `data class PriceRecord`:
  ```kotlin
  data class PriceRecord(
      val id: String,
      val costPrice: Double,
      val salePrice: Double,
      val timestamp: Long,
      val updatedBy: String,
      val reason: String? = null
  )
  ```

### 2. ProductRepositoryImpl Updates
- Add function `suspend fun updateProductPrice(productId: String, costPrice: Double, salePrice: Double, userId: String)`
- The implementation must use Firebase's multi-path update to guarantee atomicity:
  ```kotlin
  val updates = hashMapOf<String, Any>(
      "/products/$productId/costPrice" to costPrice,
      "/products/$productId/salePrice" to salePrice,
      "/products/$productId/updatedAt" to timestamp,
      "/product_price_history/$productId/$pushId" to priceRecord
  )
  database.reference.updateChildren(updates)
  ```

### 3. Fetching History
- Add function `fun getProductPriceHistory(productId: String): Flow<List<PriceRecord>>`
- Sort records descending by timestamp so the UI shows the latest history first.

package com.wavehouse.core.ui.navigation

/**
 * Type-safe navigation routes for WaveHouse.
 */
sealed class Routes(val route: String) {

    // ── Auth ────────────────────────────────────────────────────
    data object Splash : Routes("splash")
    data object Login : Routes("login")
    data object ForgotPassword : Routes("forgot_password")

    // ── Main tabs ───────────────────────────────────────────────
    data object Dashboard : Routes("dashboard")

    // Products
    data object ProductList : Routes("product_list")
    data object ProductDetail : Routes("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    data object AddProduct : Routes("add_product")
    data object EditProduct : Routes("edit_product/{productId}") {
        fun createRoute(productId: String) = "edit_product/$productId"
    }
    data object BarcodeScanner : Routes("barcode_scanner")

    // Stock
    data object StockOverview : Routes("stock_overview")
    data object StockIn : Routes("stock_in")
    data object StockOut : Routes("stock_out")
    data object StockHistory : Routes("stock_history")
    data object LowStockAlert : Routes("low_stock_alert")

    // Suppliers
    data object SupplierList : Routes("supplier_list")
    data object AddEditSupplier : Routes("add_edit_supplier?supplierId={supplierId}") {
        fun createRoute(supplierId: String? = null) =
            if (supplierId != null) "add_edit_supplier?supplierId=$supplierId"
            else "add_edit_supplier"
    }

    // Reports & Settings
    data object Report : Routes("report")
    data object Settings : Routes("settings")
    data object ChangePassword : Routes("change_password")
    data object Profile : Routes("profile")
}

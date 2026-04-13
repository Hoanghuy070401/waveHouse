package com.wavehouse.core.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wavehouse.R

/**
 * Type-safe navigation routes for WaveHouse.
 * Uses sealed class hierarchy for compile-time safety.
 */
sealed class Routes(val route: String) {

    // ── Auth Graph ──────────────────────────────────────────
    data object Auth : Routes("auth_graph")
    data object Splash : Routes("splash")
    data object Login : Routes("login")
    data object ForgotPassword : Routes("forgot_password")

    // ── Main Graph ──────────────────────────────────────────
    data object Main : Routes("main_graph")

    // Dashboard
    data object Dashboard : Routes("dashboard")

    // Products
    data object ProductGraph : Routes("product_graph")
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
    data object StockGraph : Routes("stock_graph")
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

    // Reports
    data object Report : Routes("report")

    // Settings
    data object Settings : Routes("settings")
    data object ChangePassword : Routes("change_password")
    data object Profile : Routes("profile")
}

/**
 * Bottom navigation items
 */
enum class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val iconSelectedRes: Int
) {
    DASHBOARD(
        route = Routes.Dashboard.route,
        labelRes = R.string.nav_dashboard,
        iconRes = R.drawable.ic_nav_dashboard,
        iconSelectedRes = R.drawable.ic_nav_dashboard_filled
    ),
    PRODUCTS(
        route = Routes.ProductList.route,
        labelRes = R.string.nav_products,
        iconRes = R.drawable.ic_nav_products,
        iconSelectedRes = R.drawable.ic_nav_products_filled
    ),
    STOCK(
        route = Routes.StockOverview.route,
        labelRes = R.string.nav_stock,
        iconRes = R.drawable.ic_nav_stock,
        iconSelectedRes = R.drawable.ic_nav_stock_filled
    ),
    REPORTS(
        route = Routes.Report.route,
        labelRes = R.string.nav_reports,
        iconRes = R.drawable.ic_nav_reports,
        iconSelectedRes = R.drawable.ic_nav_reports_filled
    ),
    SETTINGS(
        route = Routes.Settings.route,
        labelRes = R.string.nav_settings,
        iconRes = R.drawable.ic_nav_settings,
        iconSelectedRes = R.drawable.ic_nav_settings_filled
    )
}

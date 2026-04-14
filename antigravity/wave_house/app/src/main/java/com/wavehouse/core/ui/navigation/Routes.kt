package com.wavehouse.core.ui.navigation

/**
 * Type-safe navigation routes for WaveHouse / FreshStock.
 */
sealed class Routes(val route: String) {

    // ── Auth ────────────────────────────────────────────────────
    data object Splash : Routes("splash")
    data object Login : Routes("login")
    data object Register : Routes("register")
    data object ForgotPassword : Routes("forgot_password")
    data object ForgotPasswordSuccess : Routes("forgot_password_success?email={email}") {
        fun createRoute(email: String) = "forgot_password_success?email=${android.net.Uri.encode(email)}"
    }
    data object EmailVerification : Routes("email_verification?email={email}") {
        fun createRoute(email: String) = "email_verification?email=${android.net.Uri.encode(email)}"
    }

    // ── Main tabs (5 tabs theo PRD) ─────────────────────────────
    data object Dashboard : Routes("dashboard")          // 🏠 Trang chủ
    data object ProductList : Routes("product_list")      // 📦 Kho hàng (= Inventory tab)
    data object Pos : Routes("pos")                       // 🛒 Bán hàng (POS)
    data object Report : Routes("report")                 // 📊 Báo cáo (Owner only)
    data object Account : Routes("account")               // 👤 Tài khoản

    // ── Product detail / edit ───────────────────────────────────
    data object ProductDetail : Routes("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    data object AddProduct : Routes("add_product")
    data object EditProduct : Routes("edit_product/{productId}") {
        fun createRoute(productId: String) = "edit_product/$productId"
    }
    data object BarcodeScanner : Routes("barcode_scanner")

    // ── Stock ───────────────────────────────────────────────────
    data object StockOverview : Routes("stock_overview")
    data object StockIn : Routes("stock_in")
    data object StockOut : Routes("stock_out")
    data object Shrinkage : Routes("shrinkage")           // Hao hụt
    data object StockHistory : Routes("stock_history")
    data object LowStockAlert : Routes("low_stock_alert")

    // ── POS sub-screens ─────────────────────────────────────────
    data object PosCheckout : Routes("pos_checkout")
    data object PosPaymentPending : Routes("pos_payment_pending/{orderId}") {
        fun createRoute(orderId: String) = "pos_payment_pending/$orderId"
    }

    // ── Suppliers ───────────────────────────────────────────────
    data object SupplierList : Routes("supplier_list")
    data object AddEditSupplier : Routes("add_edit_supplier?supplierId={supplierId}") {
        fun createRoute(supplierId: String? = null) =
            if (supplierId != null) "add_edit_supplier?supplierId=$supplierId"
            else "add_edit_supplier"
    }

    // ── Settings / Account sub-screens ──────────────────────────
    data object Settings : Routes("settings")
    data object ChangePassword : Routes("change_password")
    data object Profile : Routes("profile")
    data object ManageStaff : Routes("manage_staff")
    data object PaymentConfig : Routes("payment_config")
}

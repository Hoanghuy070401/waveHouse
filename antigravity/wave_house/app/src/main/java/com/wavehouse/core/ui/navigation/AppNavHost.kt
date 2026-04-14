package com.wavehouse.core.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wavehouse.domain.model.UserRole
import com.wavehouse.presentation.auth.login.LoginScreen
import com.wavehouse.presentation.auth.register.RegisterScreen
import com.wavehouse.presentation.auth.splash.SplashScreen
import com.wavehouse.presentation.auth.emailverification.EmailVerificationScreen
import com.wavehouse.presentation.auth.forgotpassword.ForgotPasswordScreen
import com.wavehouse.presentation.auth.forgotpassword.ForgotPasswordSuccessScreen
import com.wavehouse.presentation.auth.forgotpassword.NewPasswordScreen
import com.wavehouse.presentation.dashboard.DashboardScreen
import com.wavehouse.presentation.product.addedit.AddEditProductScreen
import com.wavehouse.presentation.product.detail.ProductDetailScreen
import com.wavehouse.presentation.product.list.ProductListScreen
import com.wavehouse.presentation.product.scanner.BarcodeScanScreen
import com.wavehouse.presentation.pos.PosScreen
import com.wavehouse.presentation.report.ReportScreen
import com.wavehouse.presentation.settings.SettingsScreen
import com.wavehouse.presentation.stock.history.StockHistoryScreen
import com.wavehouse.presentation.stock.lowstock.LowStockAlertScreen
import com.wavehouse.presentation.stock.overview.StockOverviewScreen
import com.wavehouse.presentation.stock.shrinkage.ShrinkageScreen
import com.wavehouse.presentation.stock.stockin.StockInScreen
import com.wavehouse.presentation.stock.stockout.StockOutScreen
import com.wavehouse.presentation.supplier.SupplierListScreen
import com.wavehouse.presentation.supplier.addedit.AddEditSupplierScreen

private const val NAV_ANIM_DURATION = 300

/** Bottom nav destination — mapped to Material Icons */
data class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val requiresRole: Set<UserRole>? = null  // null = all roles
)

/** 5 tabs theo PRD FreshStock
 *  - Báo cáo chỉ hiển thị cho ADMIN và ACCOUNTANT */
private val allBottomNavDestinations = listOf(
    BottomNavDestination(
        Routes.Dashboard.route, "Trang chủ",
        Icons.Outlined.Home, Icons.Filled.Home
    ),
    BottomNavDestination(
        Routes.ProductList.route, "Kho hàng",
        Icons.Outlined.Inventory2, Icons.Filled.Inventory2
    ),
    BottomNavDestination(
        Routes.Pos.route, "Bán hàng",
        Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart
    ),
    BottomNavDestination(
        Routes.Report.route, "Báo cáo",
        Icons.Outlined.BarChart, Icons.Filled.BarChart,
        requiresRole = setOf(UserRole.ADMIN, UserRole.ACCOUNTANT)
    ),
    BottomNavDestination(
        Routes.Account.route, "Tài khoản",
        Icons.Outlined.Person, Icons.Filled.Person
    ),
)

private val routesWithoutBottomBar = setOf(
    Routes.Splash.route,
    Routes.Login.route,
    Routes.Register.route,
    Routes.EmailVerification.route,
    Routes.ForgotPassword.route,
    Routes.ForgotPasswordSuccess.route,
    Routes.NewPassword.route,
    Routes.ProductDetail.route,
    Routes.AddProduct.route,
    Routes.EditProduct.route,
    Routes.BarcodeScanner.route,
    Routes.StockIn.route,
    Routes.StockOut.route,
    Routes.Shrinkage.route,
    Routes.StockHistory.route,
    Routes.LowStockAlert.route,
    Routes.ChangePassword.route,
    Routes.PosCheckout.route,
    Routes.PosPaymentPending.route,
    Routes.ManageStaff.route,
    Routes.PaymentConfig.route,
)

@Composable
fun AppNavHost(
    currentUserRole: UserRole = UserRole.ADMIN
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = routesWithoutBottomBar.none { pattern ->
        currentRoute?.startsWith(pattern.substringBefore("{")) == true
    }

    // Filter tabs by user role
    val visibleDestinations = allBottomNavDestinations.filter { dest ->
        dest.requiresRole == null || currentUserRole in dest.requiresRole
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FreshStockBottomBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    destinations = visibleDestinations
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash.route
        ) {
            // ── Auth ────────────────────────────────────────────────────────
            composable(
                route = Routes.Splash.route,
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) }
            ) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Routes.Dashboard.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToEmailVerification = { email ->
                        navController.navigate(Routes.EmailVerification.createRoute(email)) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.Login.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) }
            ) {
                LoginScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Routes.Dashboard.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Routes.Register.route)
                    },
                    onNavigateToEmailVerification = { email ->
                        navController.navigate(Routes.EmailVerification.createRoute(email)) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Routes.ForgotPassword.route)
                    }
                )
            }

            composable(
                route = Routes.Register.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(NAV_ANIM_DURATION)) }
            ) {
                RegisterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(Routes.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToEmailVerification = { email ->
                        navController.navigate(Routes.EmailVerification.createRoute(email)) {
                            popUpTo(Routes.Register.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Email Verification ──────────────────────────────────────────
            composable(
                route = Routes.EmailVerification.route,
                arguments = listOf(navArgument("email") {
                    defaultValue = ""
                }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) }
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                EmailVerificationScreen(
                    email = email,
                    onNavigateToDashboard = {
                        navController.navigate(Routes.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Forgot Password ─────────────────────────────────────────────
            composable(
                route = Routes.ForgotPassword.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(NAV_ANIM_DURATION)) }
            ) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(Routes.ForgotPassword.route) { inclusive = true }
                        }
                    },
                    onNavigateToSuccess = { email ->
                        navController.navigate(Routes.ForgotPasswordSuccess.createRoute(email)) {
                            popUpTo(Routes.ForgotPassword.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Forgot Password: Email sent success ─────────────────────────
            composable(
                route = Routes.ForgotPasswordSuccess.route,
                arguments = listOf(navArgument("email") { defaultValue = "" }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(NAV_ANIM_DURATION)) }
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                ForgotPasswordSuccessScreen(
                    email = email,
                    onNavigateToLogin = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── New Password (Deep Link from email) ────────────────────────
            composable(
                route = Routes.NewPassword.route,
                arguments = listOf(navArgument("oobCode") { defaultValue = "" }),
                deepLinks = listOf(
                    androidx.navigation.navDeepLink {
                        uriPattern = "${Routes.NewPassword.deepLinkPattern}?oobCode={oobCode}"
                    }
                ),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(NAV_ANIM_DURATION)) }
            ) { backStackEntry ->
                val oobCode = backStackEntry.arguments?.getString("oobCode") ?: ""
                NewPasswordScreen(
                    oobCode = oobCode,
                    onNavigateToLogin = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── Dashboard (Trang chủ) ──────────────────────────────────────
            composable(route = Routes.Dashboard.route) {
                DashboardScreen(navController = navController)
            }

            // ── Inventory (Kho hàng) ───────────────────────────────────────
            composable(route = Routes.ProductList.route) {
                ProductListScreen(
                    onNavigateToDetail = { navController.navigate(Routes.ProductDetail.createRoute(it)) },
                    onNavigateToAdd = { navController.navigate(Routes.AddProduct.route) },
                    onNavigateToScanner = { navController.navigate(Routes.BarcodeScanner.route) }
                )
            }

            composable(
                route = Routes.ProductDetail.route,
                arguments = listOf(navArgument("productId") { }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(NAV_ANIM_DURATION)) }
            ) {
                ProductDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigate(Routes.EditProduct.createRoute(it)) }
                )
            }

            composable(
                route = Routes.AddProduct.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(NAV_ANIM_DURATION)) }
            ) {
                AddEditProductScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanner = { navController.navigate(Routes.BarcodeScanner.route) }
                )
            }

            composable(
                route = Routes.EditProduct.route,
                arguments = listOf(navArgument("productId") { }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(NAV_ANIM_DURATION)) }
            ) {
                AddEditProductScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanner = { navController.navigate(Routes.BarcodeScanner.route) }
                )
            }

            composable(route = Routes.BarcodeScanner.route) {
                BarcodeScanScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onProductFound = { productId ->
                        navController.navigate(Routes.ProductDetail.createRoute(productId)) {
                            popUpTo(Routes.BarcodeScanner.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── POS (Bán hàng) ─────────────────────────────────────────────
            composable(route = Routes.Pos.route) {
                PosScreen(navController = navController)
            }

            // ── Stock sub-screens ──────────────────────────────────────────
            composable(route = Routes.StockOverview.route) {
                StockOverviewScreen(navController = navController)
            }

            composable(
                route = Routes.StockIn.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(NAV_ANIM_DURATION)) }
            ) {
                StockInScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanner = { navController.navigate(Routes.BarcodeScanner.route) }
                )
            }

            composable(
                route = Routes.StockOut.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(NAV_ANIM_DURATION)) }
            ) {
                StockOutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanner = { navController.navigate(Routes.BarcodeScanner.route) }
                )
            }

            composable(
                route = Routes.Shrinkage.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(NAV_ANIM_DURATION)) }
            ) {
                ShrinkageScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.StockHistory.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(NAV_ANIM_DURATION)) }
            ) {
                StockHistoryScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.LowStockAlert.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(NAV_ANIM_DURATION)) }
            ) {
                LowStockAlertScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToStockIn = { navController.navigate(Routes.StockIn.route) }
                )
            }

            // ── Supplier ────────────────────────────────────────────────────
            composable(route = Routes.SupplierList.route) {
                SupplierListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAdd = { navController.navigate(Routes.AddEditSupplier.createRoute()) },
                    onNavigateToEdit = { navController.navigate(Routes.AddEditSupplier.createRoute(it)) }
                )
            }

            composable(
                route = Routes.AddEditSupplier.route,
                arguments = listOf(navArgument("supplierId") { nullable = true }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(NAV_ANIM_DURATION)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(NAV_ANIM_DURATION)) }
            ) {
                AddEditSupplierScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Report ──────────────────────────────────────────────────────
            composable(route = Routes.Report.route) {
                ReportScreen()
            }

            // ── Account / Settings ──────────────────────────────────────────
            composable(route = Routes.Account.route) {
                SettingsScreen(navController = navController)
            }

            composable(route = Routes.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}

@Composable
private fun FreshStockBottomBar(
    navController: NavController,
    currentRoute: String?,
    destinations: List<BottomNavDestination>
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        destinations.forEach { destination ->
            val isSelected = currentRoute == destination.route
            val isPosTab = destination.route == Routes.Pos.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon else destination.icon,
                        contentDescription = destination.label,
                        modifier = if (isPosTab) Modifier.size(28.dp) else Modifier.size(24.dp)
                    )
                },
                label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) },
                colors = if (isPosTab) {
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                } else {
                    NavigationBarItemDefaults.colors()
                }
            )
        }
    }
}

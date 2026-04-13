package com.wavehouse.core.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wavehouse.presentation.auth.login.LoginScreen
import com.wavehouse.presentation.auth.splash.SplashScreen
import com.wavehouse.presentation.dashboard.DashboardScreen
import com.wavehouse.presentation.product.addedit.AddEditProductScreen
import com.wavehouse.presentation.product.detail.ProductDetailScreen
import com.wavehouse.presentation.product.list.ProductListScreen
import com.wavehouse.presentation.product.scanner.BarcodeScanScreen
import com.wavehouse.presentation.report.ReportScreen
import com.wavehouse.presentation.settings.SettingsScreen
import com.wavehouse.presentation.stock.history.StockHistoryScreen
import com.wavehouse.presentation.stock.lowstock.LowStockAlertScreen
import com.wavehouse.presentation.stock.overview.StockOverviewScreen
import com.wavehouse.presentation.stock.stockin.StockInScreen
import com.wavehouse.presentation.stock.stockout.StockOutScreen

private const val NAV_ANIM_DURATION = 300

/** Bottom nav destinations — mapped to Material Icons (no drawable resources needed) */
data class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val bottomNavDestinations = listOf(
    BottomNavDestination(Routes.Dashboard.route, "Tổng quan", Icons.Outlined.Home, Icons.Filled.Home),
    BottomNavDestination(Routes.ProductList.route, "Sản phẩm", Icons.Outlined.Inventory2, Icons.Filled.Inventory2),
    BottomNavDestination(Routes.StockOverview.route, "Kho hàng", Icons.Outlined.Warehouse, Icons.Filled.Warehouse),
    BottomNavDestination(Routes.Report.route, "Báo cáo", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    BottomNavDestination(Routes.Settings.route, "Cài đặt", Icons.Outlined.Settings, Icons.Filled.Settings),
)

private val routesWithoutBottomBar = setOf(
    Routes.Splash.route,
    Routes.Login.route,
    Routes.ForgotPassword.route,
    Routes.ProductDetail.route,
    Routes.AddProduct.route,
    Routes.EditProduct.route,
    Routes.BarcodeScanner.route,
    Routes.StockIn.route,
    Routes.StockOut.route,
    Routes.StockHistory.route,
    Routes.LowStockAlert.route,
    Routes.ChangePassword.route,
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = routesWithoutBottomBar.none { pattern ->
        currentRoute?.startsWith(pattern.substringBefore("{")) == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                WaveHouseBottomBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    destinations = bottomNavDestinations
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
                    }
                )
            }

            // ── Dashboard ───────────────────────────────────────────────────
            composable(route = Routes.Dashboard.route) {
                DashboardScreen(navController = navController)
            }

            // ── Products ────────────────────────────────────────────────────
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

            // ── Stock ───────────────────────────────────────────────────────
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

            // ── Report ──────────────────────────────────────────────────────
            composable(route = Routes.Report.route) {
                ReportScreen()
            }

            // ── Settings ────────────────────────────────────────────────────
            composable(route = Routes.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}

@Composable
private fun WaveHouseBottomBar(
    navController: NavController,
    currentRoute: String?,
    destinations: List<BottomNavDestination>
) {
    NavigationBar {
        destinations.forEach { destination ->
            val isSelected = currentRoute == destination.route
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
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

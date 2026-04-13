package com.wavehouse.core.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.wavehouse.presentation.auth.login.LoginScreen
import com.wavehouse.presentation.auth.splash.SplashScreen
import com.wavehouse.presentation.dashboard.DashboardScreen
import com.wavehouse.presentation.product.list.ProductListScreen
import com.wavehouse.presentation.report.ReportScreen
import com.wavehouse.presentation.settings.SettingsScreen
import com.wavehouse.presentation.stock.overview.StockOverviewScreen

private const val NAV_ANIM_DURATION = 300

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Routes.Dashboard.route,
        Routes.ProductList.route,
        Routes.StockOverview.route,
        Routes.Report.route,
        Routes.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                WaveHouseBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            }
        ) {
            // Splash
            composable(Routes.Splash.route) {
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

            // Auth
            composable(
                route = Routes.Login.route,
                enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
                exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
            ) {
                LoginScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Routes.Dashboard.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // Main tabs
            composable(Routes.Dashboard.route) {
                DashboardScreen(navController = navController)
            }

            composable(Routes.ProductList.route) {
                ProductListScreen(
                    onNavigateToDetail = { productId ->
                        navController.navigate(Routes.ProductDetail.createRoute(productId))
                    },
                    onNavigateToAdd = {
                        navController.navigate(Routes.AddProduct.route)
                    },
                    onNavigateToScanner = {
                        navController.navigate(Routes.BarcodeScanner.route)
                    }
                )
            }

            composable(Routes.StockOverview.route) {
                StockOverviewScreen(navController = navController)
            }

            composable(Routes.Report.route) {
                ReportScreen()
            }

            composable(Routes.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}

package com.driverai.b2c.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.driverai.b2c.data.auth.FirebaseAuthRepository
import com.driverai.b2c.data.scan.ScanSessionWithOccurrences
import com.driverai.b2c.ui.auth.LoginScreen
import com.driverai.b2c.ui.history.HistoryScreen
import com.driverai.b2c.ui.history.ScanDetailScreen
import com.driverai.b2c.ui.profile.ProfileScreen
import com.driverai.b2c.ui.scan.ScannerScreen
import com.driverai.b2c.ui.upgrade.UpgradeScreen
import com.driverai.b2c.ui.vehicle.VehicleScreen

private const val ROUTE_LOGIN           = "login"
private const val ROUTE_SCANNER         = "scanner"
private const val ROUTE_VEHICLES        = "vehicles"
private const val ROUTE_HISTORY         = "history"
private const val ROUTE_PROFILE         = "profile"
private const val ROUTE_DETAIL          = "scan_detail"
private const val ROUTE_UPGRADE         = "upgrade"
private const val ROUTE_PAYMENT_SUCCESS = "payment_success"

@Composable
fun AppNavHost(
    authRepository: FirebaseAuthRepository,
    deepLinkUri: Uri? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val rootNavController = rememberNavController()
    val currentUser by authRepository.currentUserFlow.collectAsStateWithLifecycle(
        initialValue = authRepository.currentUser
    )
    val startDestination = if (currentUser != null) ROUTE_SCANNER else ROUTE_LOGIN

    NavHost(navController = rootNavController, startDestination = startDestination) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    rootNavController.navigate(ROUTE_SCANNER) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_SCANNER) {
            MainShell(
                deepLinkUri = deepLinkUri,
                onDeepLinkConsumed = onDeepLinkConsumed,
                onSignOut = {
                    authRepository.signOut()
                    rootNavController.navigate(ROUTE_LOGIN) {
                        popUpTo(ROUTE_SCANNER) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
private fun MainShell(
    deepLinkUri: Uri?,
    onDeepLinkConsumed: () -> Unit,
    onSignOut: () -> Unit,
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var selectedSession by remember { mutableStateOf<ScanSessionWithOccurrences?>(null) }

    // Handle Stripe payment return: driverai://payment/success
    LaunchedEffect(deepLinkUri) {
        val uri = deepLinkUri ?: return@LaunchedEffect
        if (uri.scheme == "driverai" && uri.host == "payment" && uri.pathSegments.firstOrNull() == "success") {
            bottomNavController.navigate(ROUTE_PAYMENT_SUCCESS) {
                launchSingleTop = true
            }
        }
        onDeepLinkConsumed()
    }

    val showBottomBar = currentDestination?.route?.let {
        it in listOf(ROUTE_SCANNER, ROUTE_VEHICLES, ROUTE_HISTORY, ROUTE_PROFILE)
    } ?: true

    val onUpgradeClick = { bottomNavController.navigate(ROUTE_UPGRADE) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_SCANNER } == true,
                        onClick = {
                            bottomNavController.navigate(ROUTE_SCANNER) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
                        label = { Text("Scanner") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_VEHICLES } == true,
                        onClick = {
                            bottomNavController.navigate(ROUTE_VEHICLES) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                        label = { Text("My Cars") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_HISTORY } == true,
                        onClick = {
                            bottomNavController.navigate(ROUTE_HISTORY) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text("History") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_PROFILE } == true,
                        onClick = {
                            bottomNavController.navigate(ROUTE_PROFILE) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Profile") },
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = bottomNavController, startDestination = ROUTE_SCANNER) {
                composable(ROUTE_SCANNER) {
                    ScannerScreen(onUpgradeClick = onUpgradeClick)
                }
                composable(ROUTE_VEHICLES) {
                    VehicleScreen()
                }
                composable(ROUTE_HISTORY) {
                    HistoryScreen(
                        onSessionClick = { session ->
                            selectedSession = session
                            bottomNavController.navigate(ROUTE_DETAIL)
                        }
                    )
                }
                composable(ROUTE_DETAIL) {
                    selectedSession?.let { session ->
                        ScanDetailScreen(
                            item = session,
                            onBack = { bottomNavController.popBackStack() },
                        )
                    }
                }
                composable(ROUTE_PROFILE) {
                    ProfileScreen(
                        onSignOut = onSignOut,
                        onUpgradeClick = onUpgradeClick,
                    )
                }
                composable(ROUTE_UPGRADE) {
                    UpgradeScreen(onBack = { bottomNavController.popBackStack() })
                }
                composable(ROUTE_PAYMENT_SUCCESS) {
                    PaymentSuccessScreen(
                        onContinue = {
                            bottomNavController.navigate(ROUTE_SCANNER) {
                                popUpTo(ROUTE_PAYMENT_SUCCESS) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

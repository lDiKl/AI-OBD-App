package com.driverai.b2c.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.driverai.b2c.ui.scan.ScannerScreen
import com.driverai.b2c.ui.vehicle.VehicleScreen

private const val ROUTE_LOGIN    = "login"
private const val ROUTE_SCANNER  = "scanner"
private const val ROUTE_VEHICLES = "vehicles"
private const val ROUTE_HISTORY  = "history"
private const val ROUTE_DETAIL   = "scan_detail"

@Composable
fun AppNavHost(authRepository: FirebaseAuthRepository) {
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
private fun MainShell(onSignOut: () -> Unit) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Pass the selected session object via in-memory state to avoid Parcelable requirement
    var selectedSession by remember { mutableStateOf<ScanSessionWithOccurrences?>(null) }

    val showBottomBar = currentDestination?.route?.let {
        it in listOf(ROUTE_SCANNER, ROUTE_VEHICLES, ROUTE_HISTORY)
    } ?: true

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
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = bottomNavController, startDestination = ROUTE_SCANNER) {
                composable(ROUTE_SCANNER) {
                    ScannerScreen(onSignOut = onSignOut)
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
            }
        }
    }
}

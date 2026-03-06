package com.avyrox.service.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.avyrox.service.data.auth.FirebaseAuthRepository
import com.avyrox.service.data.local.AppDatabase
import com.avyrox.service.data.shop.ShopRepository
import com.avyrox.service.ui.auth.LoginScreen
import com.avyrox.service.ui.cases.CaseDetailScreen
import com.avyrox.service.ui.cases.CasesScreen
import com.avyrox.service.ui.leads.LeadDetailScreen
import com.avyrox.service.ui.leads.LeadsScreen
import com.avyrox.service.ui.profile.ProfileScreen
import com.avyrox.service.ui.scan.ScannerScreen
import com.avyrox.service.ui.setup.ShopSetupScreen
import com.avyrox.service.viewmodel.LeadsViewModel
import kotlinx.coroutines.launch

private const val ROUTE_LOGIN   = "login"
private const val ROUTE_SETUP   = "shop_setup"
private const val ROUTE_MAIN    = "main"
private const val ROUTE_SCANNER = "scanner"
private const val ROUTE_CASES   = "cases"
private const val ROUTE_DETAIL  = "case_detail"
private const val ROUTE_LEADS   = "leads"
private const val ROUTE_LEAD_DETAIL = "lead_detail"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_PAYMENT_SUCCESS = "payment_success"

@Composable
fun AppNavHost(
    authRepository: FirebaseAuthRepository,
    shopRepository: ShopRepository,
    appDatabase: AppDatabase,
    deepLinkUri: Uri? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val rootNav = rememberNavController()
    val scope = rememberCoroutineScope()
    val currentUser by authRepository.currentUserFlow.collectAsStateWithLifecycle(
        initialValue = authRepository.currentUser
    )
    val startDestination = if (currentUser != null) ROUTE_MAIN else ROUTE_LOGIN

    // When app resumes with an existing Firebase session, verify the shop is registered.
    // LoginScreen handles this check for fresh sign-ins; we cover the cold-start case here.
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val isRegistered = shopRepository.isShopRegistered()
            if (!isRegistered) {
                rootNav.navigate(ROUTE_SETUP) {
                    popUpTo(ROUTE_MAIN) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = rootNav, startDestination = startDestination) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    rootNav.navigate(ROUTE_MAIN) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
                },
                onNeedsSetup = {
                    rootNav.navigate(ROUTE_SETUP) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
                },
            )
        }
        composable(ROUTE_SETUP) {
            ShopSetupScreen(
                onSetupComplete = {
                    rootNav.navigate(ROUTE_MAIN) { popUpTo(ROUTE_SETUP) { inclusive = true } }
                }
            )
        }
        composable(ROUTE_MAIN) {
            MainShell(
                deepLinkUri = deepLinkUri,
                onDeepLinkConsumed = onDeepLinkConsumed,
                onSignOut = {
                    scope.launch {
                        appDatabase.clearAllData()
                        authRepository.signOut()
                    }
                    rootNav.navigate(ROUTE_LOGIN) { popUpTo(ROUTE_MAIN) { inclusive = true } }
                },
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
    val bottomNav = rememberNavController()
    val navBackStackEntry by bottomNav.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Handle Stripe return: shopai://payment/success
    LaunchedEffect(deepLinkUri) {
        val uri = deepLinkUri ?: return@LaunchedEffect
        if (uri.scheme == "shopai" && uri.host == "payment" && uri.pathSegments.firstOrNull() == "success") {
            bottomNav.navigate(ROUTE_PAYMENT_SUCCESS) { launchSingleTop = true }
        }
        onDeepLinkConsumed()
    }

    val showBottomBar = currentDestination?.route in listOf(ROUTE_SCANNER, ROUTE_CASES, ROUTE_LEADS, ROUTE_PROFILE)

    val onUpgradeClick = { bottomNav.navigate(ROUTE_PROFILE) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_SCANNER } == true,
                        onClick = {
                            bottomNav.navigate(ROUTE_SCANNER) {
                                popUpTo(bottomNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Bluetooth, null) },
                        label = { Text("Scanner") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_CASES } == true,
                        onClick = {
                            bottomNav.navigate(ROUTE_CASES) {
                                popUpTo(bottomNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Assignment, null) },
                        label = { Text("Cases") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_LEADS } == true,
                        onClick = {
                            bottomNav.navigate(ROUTE_LEADS) {
                                popUpTo(bottomNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Inbox, null) },
                        label = { Text("Leads") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == ROUTE_PROFILE } == true,
                        onClick = {
                            bottomNav.navigate(ROUTE_PROFILE) {
                                popUpTo(bottomNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("Profile") },
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = bottomNav, startDestination = ROUTE_SCANNER) {
                composable(ROUTE_SCANNER) {
                    ScannerScreen(onUpgradeClick = onUpgradeClick)
                }
                composable(ROUTE_CASES) {
                    CasesScreen(
                        onCaseClick = { caseId ->
                            bottomNav.navigate("$ROUTE_DETAIL/$caseId")
                        }
                    )
                }
                composable("$ROUTE_DETAIL/{caseId}") { backStackEntry ->
                    val caseId = backStackEntry.arguments?.getString("caseId") ?: return@composable
                    CaseDetailScreen(
                        caseId = caseId,
                        onBack = { bottomNav.popBackStack() },
                    )
                }
                composable(ROUTE_LEADS) {
                    val vm: LeadsViewModel = hiltViewModel()
                    LeadsScreen(
                        repository = vm.repository,
                        onLeadClick = { leadId ->
                            bottomNav.navigate("$ROUTE_LEAD_DETAIL/$leadId")
                        },
                    )
                }
                composable("$ROUTE_LEAD_DETAIL/{leadId}") { backStackEntry ->
                    val leadId = backStackEntry.arguments?.getString("leadId") ?: return@composable
                    val vm: LeadsViewModel = hiltViewModel()
                    LeadDetailScreen(
                        leadId = leadId,
                        repository = vm.repository,
                        onBack = { bottomNav.popBackStack() },
                    )
                }
                composable(ROUTE_PROFILE) {
                    ProfileScreen(
                        onSignOut = onSignOut,
                        onUpgradeClick = onUpgradeClick,
                    )
                }
                composable(ROUTE_PAYMENT_SUCCESS) {
                    PaymentSuccessScreen(
                        onContinue = {
                            bottomNav.navigate(ROUTE_SCANNER) {
                                popUpTo(ROUTE_PAYMENT_SUCCESS) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

package com.driverai.b2c.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.driverai.b2c.data.auth.FirebaseAuthRepository
import com.driverai.b2c.ui.auth.LoginScreen
import com.driverai.b2c.ui.scan.ScannerScreen

private const val ROUTE_LOGIN = "login"
private const val ROUTE_SCANNER = "scanner"

@Composable
fun AppNavHost(authRepository: FirebaseAuthRepository) {
    val navController = rememberNavController()
    val currentUser by authRepository.currentUserFlow.collectAsStateWithLifecycle(initialValue = authRepository.currentUser)

    val startDestination = if (currentUser != null) ROUTE_SCANNER else ROUTE_LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(ROUTE_SCANNER) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_SCANNER) {
            ScannerScreen(
                onSignOut = {
                    authRepository.signOut()
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(ROUTE_SCANNER) { inclusive = true }
                    }
                }
            )
        }
    }
}

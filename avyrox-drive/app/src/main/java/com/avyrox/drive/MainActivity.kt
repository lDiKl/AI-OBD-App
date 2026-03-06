package com.avyrox.drive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.avyrox.drive.data.auth.FirebaseAuthRepository
import com.avyrox.drive.ui.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: FirebaseAuthRepository

    // Holds the latest deep-link URI — AppNavHost reacts to it via recomposition
    private var deepLinkUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkUri = intent.data
        setContent {
            MaterialTheme {
                AppNavHost(
                    authRepository = authRepository,
                    deepLinkUri = deepLinkUri,
                    onDeepLinkConsumed = { deepLinkUri = null },
                )
            }
        }
    }

    // Called when app is already running (singleTop) and a new intent arrives (Stripe redirect)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUri = intent.data
    }
}

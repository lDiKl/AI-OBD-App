package com.shopai.b2b

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shopai.b2b.data.auth.FirebaseAuthRepository
import com.shopai.b2b.data.local.AppDatabase
import com.shopai.b2b.data.shop.ShopRepository
import com.shopai.b2b.ui.AppNavHost
import com.shopai.b2b.ui.theme.ShopAITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: FirebaseAuthRepository

    @Inject
    lateinit var shopRepository: ShopRepository

    @Inject
    lateinit var appDatabase: AppDatabase

    private var deepLinkUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkUri = intent?.data
        enableEdgeToEdge()
        setContent {
            ShopAITheme {
                AppNavHost(
                    authRepository = authRepository,
                    shopRepository = shopRepository,
                    appDatabase = appDatabase,
                    deepLinkUri = deepLinkUri,
                    onDeepLinkConsumed = { deepLinkUri = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkUri = intent.data
    }
}

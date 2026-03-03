package com.driverai.b2c.ui.upgrade

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.driverai.b2c.viewmodel.UpgradeViewModel

private val FREE_FEATURES = listOf(
    "OBD Bluetooth scanning",
    "Fault code severity",
    "Can-drive indicator",
    "Scan history (local)",
)

private val PREMIUM_FEATURES = listOf(
    "Everything in Free",
    "AI plain-language explanation",
    "Likely causes with probability",
    "Recommended action",
    "Regional repair cost estimate",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
    viewModel: UpgradeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Open Stripe Checkout when URL is ready
    LaunchedEffect(state) {
        if (state is UpgradeViewModel.State.CheckoutReady) {
            val url = (state as UpgradeViewModel.State.CheckoutReady).url
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, Uri.parse(url))
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade to Premium") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Unlock the full power of AI diagnostics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            // Free plan card
            PlanCard(
                name = "Free",
                price = "€0",
                features = FREE_FEATURES,
                isCurrent = true,
                highlight = false,
            )

            // Premium plan card
            PlanCard(
                name = "Premium",
                price = "€9.99 / month",
                features = PREMIUM_FEATURES,
                isCurrent = false,
                highlight = true,
            )

            Spacer(Modifier.height(8.dp))

            if (state is UpgradeViewModel.State.Error) {
                Text(
                    (state as UpgradeViewModel.State.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = { viewModel.startCheckout() },
                enabled = state !is UpgradeViewModel.State.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state is UpgradeViewModel.State.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Upgrade — €9.99 / month")
            }

            Text(
                "Payments are processed by Stripe. Cancel anytime.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanCard(
    name: String,
    price: String,
    features: List<String>,
    isCurrent: Boolean,
    highlight: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isCurrent) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(price, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (highlight) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(feature, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

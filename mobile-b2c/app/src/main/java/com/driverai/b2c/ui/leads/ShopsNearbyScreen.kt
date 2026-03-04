package com.driverai.b2c.ui.leads

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driverai.b2c.data.leads.LeadsRepository
import com.driverai.b2c.data.network.ShopDto
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: Context): Pair<Double, Double> =
    suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) cont.resume(location.latitude to location.longitude)
                else cont.resumeWithException(Exception("Location unavailable — make sure GPS is enabled"))
            }
            .addOnFailureListener { cont.resumeWithException(it) }
        cont.invokeOnCancellation { cts.cancel() }
    }

private sealed class ShopsUiState {
    object Idle : ShopsUiState()
    object Loading : ShopsUiState()
    data class Loaded(val shops: List<ShopDto>) : ShopsUiState()
    data class Error(val message: String) : ShopsUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopsNearbyScreen(
    sessionId: String?,
    dtcCodes: List<String>,
    vehicleInfo: Map<String, String>,
    leadsRepository: LeadsRepository,
    onBack: () -> Unit,
    onLeadSent: () -> Unit,
) {
    val context = LocalContext.current
    // rememberCoroutineScope() is tied to the composition — cancels only when
    // the composable leaves, NOT on recomposition. Safe for long-running calls.
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf<ShopsUiState>(ShopsUiState.Idle) }
    var selectedShop by remember { mutableStateOf<ShopDto?>(null) }
    var sending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var successDialog by remember { mutableStateOf(false) }

    // Single function that runs the full location → shops flow in one coroutine.
    // Using scope.launch (not LaunchedEffect) so recomposition doesn't cancel it.
    fun loadShops() {
        scope.launch {
            uiState = ShopsUiState.Loading
            try {
                val (lat, lng) = getCurrentLocation(context)
                leadsRepository.getNearbyShops(lat, lng).fold(
                    onSuccess = { uiState = ShopsUiState.Loaded(it) },
                    onFailure = { uiState = ShopsUiState.Error(it.message ?: "Failed to load shops") },
                )
            } catch (e: CancellationException) {
                throw e  // never swallow CancellationException
            } catch (e: Exception) {
                uiState = ShopsUiState.Error(e.message ?: "Could not get location")
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) loadShops()
        else uiState = ShopsUiState.Error("Location permission is required to find nearby shops")
    }

    // Request permission exactly once when the screen first appears
    LaunchedEffect(Unit) {
        permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (successDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Diagnostic Sent!") },
            text = { Text("The shop will review your diagnostic and send you a quote. Check \"My Leads\" for updates.") },
            confirmButton = {
                TextButton(onClick = {
                    successDialog = false
                    onLeadSent()
                }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Shops") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = uiState) {
                is ShopsUiState.Idle ->
                    CenterLoading("Requesting location…")
                is ShopsUiState.Loading ->
                    CenterLoading("Finding nearby shops…")
                is ShopsUiState.Error ->
                    CenterError(s.message, onRetry = { loadShops() })
                is ShopsUiState.Loaded -> {
                    if (s.shops.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("No shops found nearby", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No registered shops within 50 km.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "${s.shops.size} shop(s) found within 50 km",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(s.shops) { shop ->
                                    ShopCard(
                                        shop = shop,
                                        selected = shop.id == selectedShop?.id,
                                        onClick = { selectedShop = shop },
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (sendError != null) {
                                    Text(
                                        sendError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                Button(
                                    onClick = {
                                        val shop = selectedShop ?: return@Button
                                        scope.launch {
                                            sending = true
                                            sendError = null
                                            leadsRepository.sendLead(
                                                shopId = shop.id,
                                                sessionId = sessionId,
                                                dtcCodes = dtcCodes,
                                                vehicleInfo = vehicleInfo,
                                            ).fold(
                                                onSuccess = { successDialog = true },
                                                onFailure = { sendError = "Failed to send: ${it.message}" },
                                            )
                                            sending = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    enabled = selectedShop != null && !sending,
                                ) {
                                    if (sending) {
                                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                                    } else {
                                        Text(
                                            if (selectedShop != null) "Send Diagnostic to ${selectedShop!!.name}"
                                            else "Select a shop first"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopCard(shop: ShopDto, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = if (selected) androidx.compose.foundation.BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary
        ) else null,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(shop.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star, null,
                        modifier = Modifier.height(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text("%.1f".format(shop.rating), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
            Text(shop.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(shop.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("%.1f km".format(shop.distanceKm), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CenterLoading(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CenterError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

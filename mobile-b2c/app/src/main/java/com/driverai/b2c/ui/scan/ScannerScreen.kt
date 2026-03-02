package com.driverai.b2c.ui.scan

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.driverai.b2c.data.obd.models.DtcCode
import com.driverai.b2c.data.obd.models.FreezeFrameData
import com.driverai.b2c.data.obd.models.ObdScanResult
import com.driverai.b2c.viewmodel.ScanState
import com.driverai.b2c.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    // Permission launcher for BLUETOOTH_CONNECT (required on API 31+)
    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onStartScan()
        else viewModel.onPermissionDenied()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("OBD Scanner") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val s = state) {
                is ScanState.Idle -> IdleContent(
                    onScanClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            viewModel.onStartScan()
                        }
                    }
                )
                is ScanState.RequestingPermission -> LoadingContent("Requesting permission…")
                is ScanState.SelectingDevice -> DeviceListContent(
                    devices = s.pairedDevices,
                    onDeviceClick = { viewModel.onDeviceSelected(it.address) },
                    onCancel = { viewModel.onCancelSelection() }
                )
                is ScanState.Connecting -> LoadingContent("Connecting to adapter…")
                is ScanState.Scanning -> LoadingContent("Reading OBD data…")
                is ScanState.Success -> ResultContent(result = s.result)
                is ScanState.Error -> ErrorContent(
                    message = s.message,
                    onRetry = { viewModel.onRetry() }
                )
            }
        }
    }
}

// --- Sub-composables ---

@Composable
private fun IdleContent(onScanClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text("Connect to your car's OBD adapter", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Make sure your ELM327 adapter is paired\nin Android Bluetooth settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onScanClick) {
            Text("Start OBD Scan")
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceListContent(
    devices: List<BluetoothDevice>,
    onDeviceClick: (BluetoothDevice) -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Select your ELM327 adapter",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(devices) { device ->
                ListItem(
                    headlineContent = { Text(device.name ?: "Unknown device") },
                    supportingContent = { Text(device.address, fontFamily = FontFamily.Monospace) },
                    leadingContent = {
                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onDeviceClick(device) }
                )
                Divider()
            }
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun ResultContent(result: ObdScanResult) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (result.dtcCodes.isEmpty()) {
            item { NoDtcCard() }
        } else {
            item {
                Text(
                    "${result.dtcCodes.size} fault code(s) found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(result.dtcCodes) { code -> DtcCodeCard(code) }
        }

        result.freezeFrame?.let { ff ->
            item { FreezeFrameCard(ff) }
        }
    }
}

@Composable
private fun NoDtcCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text("No fault codes found", fontWeight = FontWeight.Bold)
                Text("Your vehicle's OBD system reports no stored errors.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DtcCodeCard(code: DtcCode) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    code.raw,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(code.description, style = MaterialTheme.typography.bodySmall)
                Text(
                    code.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FreezeFrameCard(ff: FreezeFrameData) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Freeze Frame Data", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            ff.engineRpm?.let { FreezeFrameRow("Engine RPM", "$it rpm") }
            ff.vehicleSpeed?.let { FreezeFrameRow("Vehicle Speed", "$it km/h") }
            ff.coolantTemp?.let { FreezeFrameRow("Coolant Temp", "$it °C") }
            ff.engineLoad?.let { FreezeFrameRow("Engine Load", "$it %") }
        }
    }
}

@Composable
private fun FreezeFrameRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}

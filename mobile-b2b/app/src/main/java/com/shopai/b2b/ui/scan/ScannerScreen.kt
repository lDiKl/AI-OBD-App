package com.shopai.b2b.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shopai.b2b.data.obd.models.ObdScanResult
import com.shopai.b2b.viewmodel.ScannerState
import com.shopai.b2b.viewmodel.ScannerViewModel

private const val TCP_PORT = 35000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onUpgradeClick: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    // Default: 10.0.2.2 for Android emulator, change to PC LAN IP for physical device
    var tcpHost by remember { mutableStateOf("10.0.2.2") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("OBD Scanner") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val s = state) {
                is ScannerState.Idle -> IdleContent(
                    isConnected = isConnected,
                    tcpHost = tcpHost,
                    onTcpHostChange = { tcpHost = it },
                    onConnectTcp = { viewModel.connectTcp(tcpHost, TCP_PORT) },
                    onScan = { viewModel.startScan() },
                    onDisconnect = { viewModel.disconnect() },
                )
                is ScannerState.Connecting -> LoadingContent("Connecting…", onCancel = { viewModel.disconnect() })
                is ScannerState.Scanning -> LoadingContent("Scanning vehicle…", onCancel = { viewModel.disconnect() })
                is ScannerState.ScanComplete -> ScanCompleteContent(
                    scanResult = s.scanResult,
                    onCreateCase = { viewModel.proceedToCreateCase(s.scanResult) },
                    onRescan = { viewModel.startScan() },
                    onDisconnect = { viewModel.disconnect() },
                )
                is ScannerState.NeedsVehicleInfo -> VehicleInfoForm(
                    scanResult = s.scanResult,
                    onSubmit = { make, model, year, plate, symptoms ->
                        viewModel.submitVehicleInfo(s.scanResult, make, model, year, plate, symptoms)
                    },
                    onBack = { viewModel.backToScanComplete(s.scanResult) },
                )
                is ScannerState.Analyzing -> LoadingContent("AI is analyzing… this may take a moment")
                is ScannerState.Done -> DoneContent(
                    caseId = s.case.localId,
                    serverId = s.case.serverId,
                    codes = s.case.inputCodes,
                    onReset = { viewModel.reset() },
                )
                is ScannerState.Error -> ErrorContent(
                    message = s.message,
                    onRetry = { viewModel.reset() },
                )
            }
        }
    }
}

@Composable
private fun IdleContent(
    isConnected: Boolean,
    tcpHost: String,
    onTcpHostChange: (String) -> Unit,
    onConnectTcp: () -> Unit,
    onScan: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Adapter Status", fontWeight = FontWeight.Bold)
            Text(
                if (isConnected) "Connected" else "Not connected",
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            if (!isConnected) {
                OutlinedTextField(
                    value = tcpHost,
                    onValueChange = onTcpHostChange,
                    label = { Text("PC IP address") },
                    placeholder = { Text("10.0.2.2 or 192.168.x.x") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedButton(onClick = onConnectTcp, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect via TCP (emulator)")
                }
            }
        }
    }

    Button(
        onClick = onScan,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = isConnected,
    ) {
        Text("Start OBD Scan")
    }

    if (isConnected) {
        OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
            Text("Disconnect")
        }
    } else {
        Text(
            "Connect an ELM327 adapter via Bluetooth or use the TCP emulator for testing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScanCompleteContent(
    scanResult: ObdScanResult,
    onCreateCase: () -> Unit,
    onRescan: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val hasCodes = scanResult.dtcCodes.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasCodes) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (hasCodes) "DTC Codes Found" else "No Errors Found",
                fontWeight = FontWeight.Bold,
                color = if (hasCodes) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (hasCodes) {
                HorizontalDivider()
                scanResult.dtcCodes.forEach { code ->
                    Text(
                        "• ${code.raw}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            } else {
                Text(
                    "The vehicle OBD system reports no active fault codes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }

    Button(
        onClick = onCreateCase,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (hasCodes) "Create Diagnostic Case" else "Create Case Anyway")
    }

    OutlinedButton(
        onClick = onRescan,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Rescan")
    }

    OutlinedButton(
        onClick = onDisconnect,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Disconnect")
    }

    if (!hasCodes) {
        Text(
            "You can still create a case for deeper mechanical inspection or issues not captured by OBD.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VehicleInfoForm(
    scanResult: ObdScanResult,
    onSubmit: (make: String, model: String, year: String, plate: String, symptoms: String) -> Unit,
    onBack: () -> Unit,
) {
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }

    Text("Enter Vehicle Details", style = MaterialTheme.typography.titleMedium)

    OutlinedTextField(make, { make = it }, label = { Text("Make *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(model, { model = it }, label = { Text("Model *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(year, { year = it }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(plate, { plate = it }, label = { Text("License Plate") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(
        symptoms, { symptoms = it },
        label = { Text("Customer Symptoms (optional)") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )

    Spacer(Modifier.height(4.dp))

    Button(
        onClick = { onSubmit(make, model, year, plate, symptoms) },
        modifier = Modifier.fillMaxWidth(),
        enabled = make.isNotBlank() && model.isNotBlank(),
    ) {
        Text("Analyze with AI")
    }

    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text("Back to Scan Results")
    }
}

@Composable
private fun DoneContent(caseId: String, serverId: String?, codes: String, onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Case Created", fontWeight = FontWeight.Bold)
            if (serverId != null) {
                Text("Synced to server #${serverId.take(8)}", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Saved offline — will sync when connected", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Codes: $codes", style = MaterialTheme.typography.bodySmall)
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onReset, modifier = Modifier.weight(1f)) { Text("New Scan Disconnect") }
    }
}

@Composable
private fun LoadingContent(label: String, onCancel: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (onCancel != null) {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            OutlinedButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

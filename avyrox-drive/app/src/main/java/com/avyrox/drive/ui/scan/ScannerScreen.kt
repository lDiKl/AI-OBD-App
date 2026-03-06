package com.avyrox.drive.ui.scan

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.avyrox.drive.BuildConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.avyrox.drive.data.network.CodeResultDto
import com.avyrox.drive.data.network.ScanAnalyzeResponse
import com.avyrox.drive.data.obd.models.DtcCode
import com.avyrox.drive.data.obd.models.FreezeFrameData
import com.avyrox.drive.data.obd.models.ObdScanResult
import com.avyrox.drive.viewmodel.ScanState
import com.avyrox.drive.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onUpgradeClick: () -> Unit = {},
    onFindService: ((ScanAnalyzeResponse) -> Unit)? = null,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onStartScan()
        else viewModel.onPermissionDenied()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("OBD Scanner") }) }
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
                    },
                    onEmulatorClick = if (BuildConfig.DEBUG) {
                        { viewModel.onConnectToEmulator() }
                    } else null,
                )
                is ScanState.RequestingPermission -> LoadingContent("Requesting permission…")
                is ScanState.SelectingDevice -> DeviceListContent(
                    devices = s.pairedDevices,
                    onDeviceClick = { viewModel.onDeviceSelected(it.address) },
                    onCancel = { viewModel.onCancelSelection() }
                )
                is ScanState.Connecting -> LoadingContent("Connecting to adapter…", onCancel = { viewModel.onCancel() })
                is ScanState.Scanning -> LoadingContent("Reading OBD data…", onCancel = { viewModel.onCancel() })
                is ScanState.Success -> OBDResultContent(
                    result = s.result,
                    onAnalyze = { viewModel.onAnalyzeWithAI() },
                    onScanAgain = { viewModel.onScanAgain() },
                    onNewScan = { viewModel.onRetry() },
                )
                is ScanState.Analyzing -> LoadingContent("Analyzing with AI…")
                is ScanState.AnalysisReady -> AnalysisResultContent(
                    analysis = s.analysis,
                    isPremium = s.analysis.isPremium,
                    onScanAgain = { viewModel.onScanAgain() },
                    onNewScan = { viewModel.onRetry() },
                    onUpgradeClick = onUpgradeClick,
                    onFindService = if (onFindService != null) { { onFindService(s.analysis) } } else null,
                )
                is ScanState.Error -> ErrorContent(
                    message = s.message,
                    onRetry = { viewModel.onRetry() }
                )
            }
        }
    }
}

// --- OBD raw result (after BT scan, before AI analysis) ---

@Composable
private fun OBDResultContent(
    result: ObdScanResult,
    onAnalyze: () -> Unit,
    onScanAgain: () -> Unit,
    onNewScan: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (result.dtcCodes.isEmpty()) {
            item { NoDtcCard() }
        } else {
            item {
                Text(
                    "${result.dtcCodes.size} fault code(s) found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(result.dtcCodes) { code -> RawDtcCard(code) }
        }

        result.freezeFrame?.let { ff ->
            item { FreezeFrameCard(ff) }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.dtcCodes.isNotEmpty()) {
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Analyze with AI")
                    }
                }
                OutlinedButton(
                    onClick = onScanAgain,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Scan Again")
                }
                OutlinedButton(
                    onClick = onNewScan,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

// --- AI Analysis result ---

@Composable
private fun AnalysisResultContent(
    analysis: ScanAnalyzeResponse,
    isPremium: Boolean,
    onScanAgain: () -> Unit,
    onNewScan: () -> Unit,
    onUpgradeClick: () -> Unit = {},
    onFindService: (() -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { OverallRiskCard(analysis) }
        items(analysis.codes) { code -> AnalysisCodeCard(code, isPremium = isPremium, onUpgradeClick = onUpgradeClick) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onFindService != null) {
                    Button(
                        onClick = onFindService,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Find Nearby Service")
                    }
                }
                OutlinedButton(
                    onClick = onScanAgain,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Scan Again")
                }
                OutlinedButton(
                    onClick = onNewScan,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun OverallRiskCard(analysis: ScanAnalyzeResponse) {
    val (bgColor, label) = when (analysis.overallRisk) {
        "critical" -> MaterialTheme.colorScheme.errorContainer to "CRITICAL — Do NOT drive"
        "high"     -> Color(0xFFFF8C00).copy(alpha = 0.15f) to "HIGH RISK — Drive with caution"
        "medium"   -> Color(0xFFFFC107).copy(alpha = 0.15f) to "MEDIUM — Schedule service soon"
        else       -> MaterialTheme.colorScheme.primaryContainer to "LOW — Vehicle OK"
    }
    Card(colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                if (analysis.safeToDrive) "Safe to drive" else "Not safe to drive",
                style = MaterialTheme.typography.bodyMedium,
                color = if (analysis.safeToDrive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AnalysisCodeCard(code: CodeResultDto, isPremium: Boolean = false, onUpgradeClick: () -> Unit = {}) {
    val severityColor = when (code.free.severity) {
        "critical" -> MaterialTheme.colorScheme.error
        "high"     -> Color(0xFFE65100)
        "medium"   -> Color(0xFFF57F17)
        else       -> Color(0xFF2E7D32)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    code.code,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = severityColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        code.free.severity.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(code.free.description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Can drive: ${code.free.canDrive.replace('_', ' ')}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Premium AI content
            code.premium?.let { ai ->
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                Text("AI Analysis", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(ai.simpleExplanation, style = MaterialTheme.typography.bodySmall)
                if (ai.mainCauses.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Likely causes:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    ai.mainCauses.forEachIndexed { i, cause ->
                        val pct = ai.causesProbability.getOrNull(i)
                        Text(
                            "• $cause${if (pct != null) " ($pct%)" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Recommended: ${ai.recommendedAction}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Show upsell or AI-unavailable notice depending on subscription
            if (code.premium == null) {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                if (isPremium) {
                    // Premium user but AI call failed (e.g. API key not configured)
                    Text(
                        "AI analysis unavailable. Check back later or scan again.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Upgrade to Premium for AI explanation, likely causes and repair advice",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onUpgradeClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Upgrade to Premium", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// --- Sub-composables (shared / OBD raw) ---

@Composable
private fun IdleContent(
    onScanClick: () -> Unit,
    onEmulatorClick: (() -> Unit)? = null,
) {
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
        Button(onClick = onScanClick) { Text("Start OBD Scan") }
        if (onEmulatorClick != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onEmulatorClick) { Text("Connect to Emulator (debug)") }
        }
    }
}

@Composable
private fun LoadingContent(message: String, onCancel: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (onCancel != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
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
                    leadingContent = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
                    modifier = Modifier.clickable { onDeviceClick(device) }
                )
                Divider()
            }
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) { Text("Cancel") }
    }
}

@Composable
private fun NoDtcCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.size(12.dp))
            Column {
                Text("No fault codes found", fontWeight = FontWeight.Bold)
                Text("Your vehicle's OBD system reports no stored errors.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RawDtcCard(code: DtcCode) {
    Card {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(12.dp))
            Column {
                Text(code.raw, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleSmall)
                Text(code.description, style = MaterialTheme.typography.bodySmall)
                Text(code.type.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}

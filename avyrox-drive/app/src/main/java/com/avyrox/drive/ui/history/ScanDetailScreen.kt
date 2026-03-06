package com.avyrox.drive.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avyrox.drive.data.scan.ErrorOccurrenceEntity
import com.avyrox.drive.data.scan.ScanSessionWithOccurrences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(
    item: ScanSessionWithOccurrences,
    onBack: () -> Unit,
) {
    val session = item.session
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        .format(Date(session.scannedAt))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${session.vehicleYear} ${session.vehicleMake} ${session.vehicleModel}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SessionSummaryCard(session.overallRisk, session.safeToDrive, session.mileage, dateStr)
            }
            if (item.occurrences.isEmpty()) {
                item {
                    Text(
                        "No fault codes in this session",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(item.occurrences) { occurrence ->
                    OccurrenceCard(occurrence)
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(
    overallRisk: String,
    safeToDrive: Boolean,
    mileage: Int,
    dateStr: String,
) {
    val riskColor = when (overallRisk) {
        "critical" -> MaterialTheme.colorScheme.error
        "high"     -> Color(0xFFE65100)
        "medium"   -> Color(0xFFF57F17)
        else       -> Color(0xFF2E7D32)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = riskColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        "Risk: ${overallRisk.uppercase()}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = riskColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (safeToDrive) "✓ Safe to drive" else "✗ Not safe to drive",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (safeToDrive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Mileage: $mileage km", style = MaterialTheme.typography.bodySmall)
            Text("Scanned: $dateStr", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OccurrenceCard(occurrence: ErrorOccurrenceEntity) {
    val gson = remember { Gson() }
    val severityColor = when (occurrence.severity) {
        "critical" -> MaterialTheme.colorScheme.error
        "high"     -> Color(0xFFE65100)
        "medium"   -> Color(0xFFF57F17)
        else       -> Color(0xFF2E7D32)
    }
    val mainCauses: List<String> = remember(occurrence.mainCausesJson) {
        occurrence.mainCausesJson?.let {
            gson.fromJson(it, object : TypeToken<List<String>>() {}.type)
        } ?: emptyList()
    }
    val probabilities: List<Int> = remember(occurrence.causesProbabilityJson) {
        occurrence.causesProbabilityJson?.let {
            gson.fromJson(it, object : TypeToken<List<Int>>() {}.type)
        } ?: emptyList()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    occurrence.code,
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
                        occurrence.severity.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(occurrence.description, style = MaterialTheme.typography.bodySmall)
            Text(
                "Can drive: ${occurrence.canDrive.replace('_', ' ')}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            occurrence.simpleExplanation?.let { explanation ->
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                Text("AI Analysis", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(explanation, style = MaterialTheme.typography.bodySmall)
                if (mainCauses.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Likely causes:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    mainCauses.forEachIndexed { i, cause ->
                        val pct = probabilities.getOrNull(i)
                        Text(
                            "• $cause${if (pct != null) " ($pct%)" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                occurrence.recommendedAction?.let { action ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Recommended: $action",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

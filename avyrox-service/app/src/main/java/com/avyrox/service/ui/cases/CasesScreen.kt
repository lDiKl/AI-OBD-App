package com.avyrox.service.ui.cases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avyrox.service.data.local.DiagnosticCaseEntity
import com.avyrox.service.viewmodel.CasesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(
    onCaseClick: (String) -> Unit,
    viewModel: CasesViewModel = hiltViewModel(),
) {
    val cases by viewModel.cases.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Diagnostic Cases") }) }
    ) { padding ->
        if (cases.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No cases yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Scan a vehicle to create your first diagnostic case.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { /* top padding */ }
                items(cases) { case ->
                    CaseCard(case = case, onClick = { onCaseClick(case.localId) })
                }
                item { /* bottom padding */ }
            }
        }
    }
}

@Composable
private fun CaseCard(case: DiagnosticCaseEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${case.vehicleMake} ${case.vehicleModel} ${case.vehicleYear}".trim(),
                    fontWeight = FontWeight.Bold,
                )
                StatusChip(case.status)
            }
            if (case.vehiclePlate.isNotBlank()) {
                Text(case.vehiclePlate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Codes: ${case.inputCodes.trim('[', ']').replace("\"", "")}",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(case.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (case.pendingSync) {
                    Text("Pending sync", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "open" -> MaterialTheme.colorScheme.primary
        "in_progress" -> MaterialTheme.colorScheme.secondary
        "closed" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        status.replace("_", " ").replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

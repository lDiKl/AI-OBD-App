package com.avyrox.service.ui.cases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.avyrox.service.data.local.DiagnosticCaseEntity
import com.avyrox.service.data.network.DiagnosticAnalyzeResponse
import com.avyrox.service.viewmodel.CaseDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    caseId: String,
    onBack: () -> Unit,
    viewModel: CaseDetailViewModel = hiltViewModel(),
) {
    val case by viewModel.case.collectAsStateWithLifecycle()

    LaunchedEffect(caseId) { viewModel.load(caseId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Case Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            )
        }
    ) { padding ->
        val c = case
        if (c == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
        } else {
            CaseDetailContent(c, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun CaseDetailContent(case: DiagnosticCaseEntity, modifier: Modifier = Modifier) {
    val aiResult: DiagnosticAnalyzeResponse? = try {
        if (case.aiResult.isNotBlank()) Gson().fromJson(case.aiResult, DiagnosticAnalyzeResponse::class.java) else null
    } catch (_: Exception) { null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Vehicle header
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${case.vehicleMake} ${case.vehicleModel} ${case.vehicleYear}".trim(), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                if (case.vehiclePlate.isNotBlank()) Text(case.vehiclePlate)
                Text("Status: ${case.status}", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Codes
        InfoCard(title = "DTC Codes", body = case.inputCodes.trim('[', ']').replace("\"", ""))

        // Symptoms
        if (case.symptomsText.isNotBlank()) {
            InfoCard(title = "Customer Symptoms", body = case.symptomsText)
        }

        // AI Analysis
        if (aiResult != null) {
            Text("AI Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (aiResult.probableCauses.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Probable Causes", fontWeight = FontWeight.Bold)
                        aiResult.probableCauses.forEach { cause ->
                            Text("• ${cause.cause} (${cause.probability}%)")
                            Text("  ${cause.explanation}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (aiResult.diagnosticSequence.isNotEmpty()) {
                InfoCard(
                    title = "Diagnostic Steps",
                    body = aiResult.diagnosticSequence.mapIndexed { i, step -> "${i + 1}. $step" }.joinToString("\n"),
                )
            }

            if (aiResult.partsLikelyNeeded.isNotEmpty()) {
                InfoCard(title = "Parts Likely Needed", body = aiResult.partsLikelyNeeded.joinToString(", "))
            }

            InfoCard(title = "Urgency", body = aiResult.urgency.replace("_", " ").replaceFirstChar { it.uppercase() })

            if (aiResult.estimatedLaborHours > 0) {
                InfoCard(title = "Estimated Labor", body = "%.1f hours".format(aiResult.estimatedLaborHours))
            }

            if (aiResult.additionalNotes.isNotBlank()) {
                InfoCard(title = "Additional Notes", body = aiResult.additionalNotes)
            }
        } else if (case.pendingSync) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI analysis pending — case will sync when connected to the internet.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

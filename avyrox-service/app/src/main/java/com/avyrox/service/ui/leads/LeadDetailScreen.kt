package com.avyrox.service.ui.leads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avyrox.service.data.leads.B2BLeadsRepository
import com.avyrox.service.data.network.B2BLeadDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadDetailScreen(
    leadId: String,
    repository: B2BLeadsRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var lead by remember { mutableStateOf<B2BLeadDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var acting by remember { mutableStateOf(false) }
    var actError by remember { mutableStateOf<String?>(null) }

    // Quote form state
    var costMin by remember { mutableStateOf("") }
    var costMax by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(leadId) {
        loading = true
        repository.getLead(leadId).fold(
            onSuccess = {
                lead = it
                // Pre-fill form if quote already exists
                it.quote?.let { q ->
                    costMin = q.costMin.toInt().toString()
                    costMax = q.costMax.toInt().toString()
                    days = q.estimatedDays.toString()
                    notes = q.notes ?: ""
                }
                error = null
            },
            onFailure = { error = it.message },
        )
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lead Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        when {
            loading -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
            error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onBack) { Text("Go back") }
            }
            lead != null -> {
                val l = lead!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Vehicle info
                    SectionCard(title = "Vehicle") {
                        l.vehicleInfo.forEach { (k, v) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(k.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(v, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Customer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(l.userEmail, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // DTC codes
                    if (l.dtcCodes.isNotEmpty()) {
                        SectionCard(title = "DTC Codes") {
                            l.dtcCodes.forEach { code ->
                                Text(
                                    "• $code",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    // Freeze frame
                    if (!l.freezeFrame.isNullOrEmpty()) {
                        SectionCard(title = "Freeze Frame") {
                            l.freezeFrame.forEach { (k, v) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(k, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(v.toString(), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // Existing quote display
                    if (l.quote != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Sent Quote", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "€${l.quote.costMin.toInt()} – €${l.quote.costMax.toInt()}  ·  ~${l.quote.estimatedDays} day(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (!l.quote.notes.isNullOrBlank()) {
                                    Text(l.quote.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }

                    // Actions — only shown when not closed
                    if (l.status != "closed") {
                        HorizontalDivider()
                        Text(
                            if (l.quote != null) "Update Quote" else "Send Quote",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = costMin, onValueChange = { costMin = it },
                                label = { Text("Min €") }, modifier = Modifier.weight(1f), singleLine = true,
                            )
                            OutlinedTextField(
                                value = costMax, onValueChange = { costMax = it },
                                label = { Text("Max €") }, modifier = Modifier.weight(1f), singleLine = true,
                            )
                            OutlinedTextField(
                                value = days, onValueChange = { days = it },
                                label = { Text("Days") }, modifier = Modifier.weight(1f), singleLine = true,
                            )
                        }
                        OutlinedTextField(
                            value = notes, onValueChange = { notes = it },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )

                        if (actError != null) {
                            Text(actError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val min = costMin.toFloatOrNull()
                                    val max = costMax.toFloatOrNull()
                                    val d = days.toIntOrNull()
                                    if (min == null || max == null || d == null) {
                                        actError = "Please fill in Min, Max, and Days with valid numbers."
                                        return@Button
                                    }
                                    scope.launch {
                                        acting = true
                                        actError = null
                                        repository.sendQuote(leadId, min, max, d, notes.ifBlank { null }).fold(
                                            onSuccess = { lead = it },
                                            onFailure = { actError = it.message },
                                        )
                                        acting = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !acting,
                            ) {
                                if (acting) CircularProgressIndicator(modifier = Modifier.height(18.dp))
                                else Text(if (l.quote != null) "Update Quote" else "Send Quote")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        acting = true
                                        actError = null
                                        repository.closeLead(leadId).fold(
                                            onSuccess = { lead = it },
                                            onFailure = { actError = it.message },
                                        )
                                        acting = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !acting,
                            ) {
                                Text("Close Lead")
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Text(
                                "This lead is closed.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            HorizontalDivider()
            content()
        }
    }
}

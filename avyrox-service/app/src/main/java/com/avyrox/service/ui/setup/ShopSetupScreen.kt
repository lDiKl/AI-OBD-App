package com.avyrox.service.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avyrox.service.viewmodel.SetupState
import com.avyrox.service.viewmodel.ShopSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: ShopSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var shopName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is SetupState.Done) onSetupComplete()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Set Up Your Shop") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Almost done! Tell us about your shop.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text("Shop Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (state is SetupState.Error) {
                Text(
                    (state as SetupState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (state is SetupState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.setupShop(shopName, address, phone, "") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = shopName.isNotBlank(),
                ) {
                    Text("Create Shop")
                }
            }
        }
    }
}

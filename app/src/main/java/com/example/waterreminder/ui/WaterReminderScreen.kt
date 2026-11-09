package com.example.waterreminder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WaterReminderScreen(
    modifier: Modifier = Modifier,
    viewModel: WaterReminderViewModel = viewModel()
) {
    val context = LocalContext.current

    // ViewModel (DataStore)
    val reminderCount by viewModel.reminderCount.collectAsStateWithLifecycle()
    val isEnabled by viewModel.isReminderEnabled.collectAsStateWithLifecycle()
    val savedInterval by viewModel.savedIntervalMinutes.collectAsStateWithLifecycle()

    var intervalInput by remember(savedInterval) { mutableStateOf(savedInterval.toString()) }

    // лаунчер для запиту дозволу POST_NOTIFICATIONS на Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val interval = intervalInput.toLongOrNull() ?: 15L
            viewModel.startReminder(interval)
        } else {
            Toast.makeText(
                context,
                "дозвіл на сповіщення обов'язковий для роботи нагадувань",
                Toast.LENGTH_LONG
            ).show()
            viewModel.stopReminder()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💧 трекер гідратації",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = intervalInput,
            onValueChange = { newValue ->
                // лише цифри
                if (newValue.all { it.isDigit() }) {
                    intervalInput = newValue
                }
            },
            label = { Text("інтервал нагадувань (хв)") },
            supportingText = { Text("мін системний інтервал 15 хвилин") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // нагадування
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "увімкнути нагадування",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            val interval = intervalInput.toLongOrNull() ?: 15L
                            if (interval < 15L) {
                                intervalInput = "15"
                                Toast.makeText(
                                    context,
                                    "встановлено мін системний інтервал: 15 хв",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            // 13+
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.startReminder(interval)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.startReminder(interval)
                            }
                        } else {
                            viewModel.stopReminder()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "нагадувань відправлено: $reminderCount разів",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                viewModel.clearAll()
                intervalInput = "15"
                Toast.makeText(context, "усі дані та задачі очищено (clearAll)", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("скинути налаштування та лічильник (clearAll)")
        }
    }
}
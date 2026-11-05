package com.gameboost.pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gameboost.pro.ui.AppViewModel

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        
        SettingsCard("Gaming Mode") {
            ToggleSetting("Auto-Boost on Game Launch", "Automatically optimize when launching games", state.autoBoost, { viewModel.setAutoBoost(it) })
            ToggleSetting("Persistent Monitor", "Keep optimization running in background", state.persistentMonitor, { viewModel.setPersistentMonitor(it) })
            ToggleSetting("Thermal Alerts", "Warn when device gets too hot", state.thermalAlerts, { viewModel.setThermalAlerts(it) })
            ToggleSetting("Haptic Feedback", "Vibrate on boost completion", state.hapticFeedback, { viewModel.setHapticFeedback(it) })
        }
        
        Spacer(Modifier.height(16.dp))
        
        SettingsCard("Permissions") {
            SettingButton("Notifications", if (viewModel.areNotificationsEnabled(context)) "Enabled" else "Disabled") { viewModel.openNotificationSettings(context) }
            SettingButton("Usage Access", "Required for app detection") { viewModel.openUsageAccessSettings(context) }
            SettingButton("Battery Optimization", "Prevent background restrictions") { viewModel.openBatterySettings(context) }
        }
        
        Spacer(Modifier.height(16.dp))
        
        SettingsCard("About") {
            SettingButton("Version", "Game Booster v3.0") { }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ToggleSetting(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingButton(title: String, subtitle: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

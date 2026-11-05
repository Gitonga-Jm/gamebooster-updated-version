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
import com.gameboost.pro.engine.DeviceCompatibilityEngine
import com.gameboost.pro.ui.AppViewModel

@Composable
fun StatsScreen(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    val sessions by viewModel.sessionTracker.allSessions.collectAsState()
    val weeklyStats by viewModel.sessionTracker.weeklyStats.collectAsState()
    val context = LocalContext.current
    val deviceInfo = remember { DeviceCompatibilityEngine().getDeviceInfo(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("📊 Performance Stats",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        // Weekly Summary
        Card(Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("📅 This Week", fontWeight = FontWeight.Bold)
                Text(weeklyStats, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Device Compatibility
        Card(Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    deviceInfo.compatibilityScore >= 90 -> MaterialTheme.colorScheme.primaryContainer
                    deviceInfo.compatibilityScore >= 70 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                })
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("📱 Device: ${deviceInfo.compatibilityScore}/100", fontWeight = FontWeight.Bold)
                Text("${deviceInfo.manufacturer} ${deviceInfo.model} | Android ${deviceInfo.androidVersion}")
                Text("GPU: ${deviceInfo.gpuRenderer}")
                if (deviceInfo.manufacturerOptimizations.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("✅ Tips:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    deviceInfo.manufacturerOptimizations.take(2).forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Overall Stats
        Card(Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text("🏆 Overall", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                StatRow("Sessions", "${viewModel.sessionTracker.getSessionCount()}")
                StatRow("Play Time", "${viewModel.sessionTracker.getTotalPlayTime() / 60000}min")
                StatRow("Avg FPS", "${viewModel.sessionTracker.getAverageFps()}")
                StatRow("Avg Ping", "${viewModel.sessionTracker.getAveragePing()}ms")
            }
        }
        Spacer(Modifier.height(8.dp))

        // System Resources
        Card(Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text("💻 System", fontWeight = FontWeight.Bold)
                StatRow("RAM", "${state.ramPercent}% (${state.ramUsedMB}MB / ${state.ramTotalMB}MB)")
                StatRow("Ping", "${state.networkPing}ms")
                StatRow("CPU Temp", "${String.format("%.1f", state.cpuTemp)}°C")
                StatRow("Network", state.networkType)
            }
        }

        // Refresh button (works now because it calls viewModel.refreshMetrics())
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.refreshMetrics() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Stats")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

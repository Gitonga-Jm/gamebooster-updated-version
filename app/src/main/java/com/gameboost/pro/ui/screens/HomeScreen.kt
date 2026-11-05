package com.gameboost.pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gameboost.pro.engine.BatteryPredictor
import com.gameboost.pro.engine.GamingHeatmap
import com.gameboost.pro.ui.AppViewModel
import com.gameboost.pro.ui.components.BoostFab
import com.gameboost.pro.ui.components.GamingCard

@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    val sessions by viewModel.sessionTracker.allSessions.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("🎮 Game Booster", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Score: ${state.boostScore} | Boosts: ${state.totalBoosts}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(12.dp))
            
            // Row 1
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GamingCard("RAM", "${state.ramPercent}%", "${state.ramUsedMB}MB / ${state.ramTotalMB}MB", Modifier.weight(1f))
                GamingCard("Ping", "${state.networkPing}ms", state.networkType, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GamingCard("CPU", "${String.format("%.1f", state.cpuTemp)}°C", if (state.cpuTemp > 45) "⚠ Hot" else "✅ Normal", Modifier.weight(1f))
                GamingCard("Battery", "${state.batteryLevel}%", "${state.runningAppsCount} apps", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GamingCard("DNS", state.fastestDns.ifEmpty { "Auto" }, "${state.networkPing}ms", Modifier.weight(1f))
                GamingCard("Turbo", if (state.gamingModeActive) "🟢 ON" else "⚫ OFF", "Gaming Mode", Modifier.weight(1f))
            }
            
            // Battery Prediction Card
            if (state.batteryLevel > 0 && state.lastPreLaunchResult != null) {
                Spacer(Modifier.height(12.dp))
                val predictor = BatteryPredictor()
                val prediction = predictor.predict(state.batteryLevel, state.lastPreLaunchResult?.dnsUsed ?: "game")
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("🔋 Battery Prediction", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("~${predictor.getTimeString(prediction.estimatedMinutes)} remaining", color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            // Weekly Stats Card
            Spacer(Modifier.height(12.dp))
            val heatmap = GamingHeatmap(viewModel.sessionTracker)
            val weekSessions = sessions.filter { it.startTime > System.currentTimeMillis() - 7 * 86400000L }
            if (weekSessions.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("📊 This Week", fontWeight = FontWeight.Bold)
                        Text("${weekSessions.size} sessions | ${weekSessions.sumOf { it.durationMs } / 60000}min played", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            // Last Session
            state.lastPreLaunchResult?.let { result ->
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("📊 Last Session", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("RAM Freed: ${result.ramFreedMB}MB | Ping: ${result.pingAfter}ms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("DNS: ${result.dnsUsed} | Ping: ${result.pingAfter}ms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            
            Spacer(Modifier.height(80.dp))
        }
        
        BoostFab(onClick = { viewModel.triggerBoost() }, enabled = !state.boostInProgress, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
        
        if (state.boostInProgress) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(state.boostMessage ?: "Optimizing...", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.8f), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("${(state.boostProgress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

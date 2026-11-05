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
import com.gameboost.pro.engine.CacheCleaner
import com.gameboost.pro.engine.GameReadinessScore
import com.gameboost.pro.engine.NetworkDiagnostics
import com.gameboost.pro.ui.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    val tempStatus by viewModel.thermalCooler.tempStatus.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var cacheCleared by remember { mutableStateOf(0L) }
    var isClearing by remember { mutableStateOf(false) }
    var networkReport by remember { mutableStateOf<com.gameboost.pro.engine.NetworkReport?>(null) }
    var isRunningDiagnostics by remember { mutableStateOf(false) }
    var readinessReport by remember { mutableStateOf<com.gameboost.pro.engine.ReadinessReport?>(null) }
    var isCalculatingReadiness by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { viewModel.refreshMetrics(); viewModel.thermalCooler.startMonitoring() }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Optimization Tools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        
        // GAME READINESS SCORE
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🎯 Game Readiness", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(8.dp))
                
                if (readinessReport != null) {
                    val r = readinessReport ?: return@Column
                    Text("${r.emoji} ${r.score}/100", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(r.rating, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (r.problems.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        r.problems.forEach { problem ->
                            Text("⚠️ $problem", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                } else {
                    Text("Tap to analyze your device", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    isCalculatingReadiness = true
                    scope.launch {
                        readinessReport = GameReadinessScore(context).calculate()
                        isCalculatingReadiness = false
                    }
                }, enabled = !isCalculatingReadiness, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isCalculatingReadiness) "Analyzing..." else "🎯 Check Game Readiness")
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // NETWORK DIAGNOSTICS
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🌐 Network Diagnostics", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                if (networkReport != null) {
                    val n = networkReport ?: return@Column
                    Text("📡 Ping: ${n.ping}ms | Jitter: ${n.jitter}ms")
                    Text("📶 WiFi: ${n.wifiQuality} (${n.wifiStrength}dBm)")
                    Text("🌐 DNS: ${n.dnsLatency}ms | Packet Loss: ${String.format("%.1f", n.packetLoss)}%")
                    Text("📊 Score: ${n.score}/100 | ${n.stability}", fontWeight = FontWeight.Bold, color = when {
                        n.score >= 80 -> MaterialTheme.colorScheme.primary
                        n.score >= 50 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    })
                    if (n.recommendations.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        n.recommendations.take(2).forEach { rec ->
                            Text("💡 $rec", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Text("Tap to run full network analysis")
                }
                
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    isRunningDiagnostics = true
                    scope.launch {
                        networkReport = NetworkDiagnostics(context).runFullDiagnostics()
                        isRunningDiagnostics = false
                    }
                }, enabled = !isRunningDiagnostics, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isRunningDiagnostics) "Testing..." else "🌐 Run Network Diagnostics")
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // RAM Boost
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🧹 Memory Optimizer", fontWeight = FontWeight.Bold)
                Text("Safe garbage collection", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                val ramInfo = if (state.lastBoostResult != null) "Freed ${state.lastBoostResult?.ramFreedMB ?: 0}MB" else "Current: ${state.ramPercent}% used"
                Text(ramInfo, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.triggerBoost() }, enabled = !state.boostInProgress, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.boostInProgress) "Optimizing..." else "🧹 Optimize Memory")
                }
            }
        }
        
        // Thermal Monitor
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = when(tempStatus.status) {
                "CRITICAL" -> MaterialTheme.colorScheme.errorContainer; "WARNING" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant })) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🌡️ Thermal Monitor", fontWeight = FontWeight.Bold)
                Text("CPU: ${String.format("%.1f", tempStatus.cpuTemp)}°C | Battery: ${String.format("%.1f", tempStatus.batteryTemp)}°C")
                if (tempStatus.coolingActive) Text("❄️ Cooling active...", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.thermalCooler.manualCool() }, enabled = !tempStatus.coolingActive,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (tempStatus.coolingActive) "Cooling..." else "❄️ Cool Device")
                }
            }
        }
        
        // Cache Cleaner
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("💾 Cache Cleaner", fontWeight = FontWeight.Bold)
                if (cacheCleared > 0) {
                    val mb = cacheCleared / (1024 * 1024)
                    Text("✅ Cleared ${if (mb > 0) "${mb}MB" else "${cacheCleared / 1024}KB"}!", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    isClearing = true
                    scope.launch { cacheCleared = CacheCleaner(context).clearAllCaches(); isClearing = false }
                }, enabled = !isClearing, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isClearing) "Clearing..." else "💾 Clear Caches")
                }
            }
        }
    }
}

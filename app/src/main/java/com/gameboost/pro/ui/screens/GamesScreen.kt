@file:OptIn(ExperimentalMaterial3Api::class)

package com.gameboost.pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gameboost.pro.ui.AppViewModel

@Composable
fun GamesScreen(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showGamesOnly by remember { mutableStateOf(true) }
    var showBoostedOnly by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { viewModel.loadAllApps() }
    
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Network Stabilizer Card
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = if (state.networkStabilizerActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column {
                    Text("🌐 Network Stabilizer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Ping: ${state.avgPing}ms | ${state.networkType}", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.networkStabilizerActive, onCheckedChange = { viewModel.toggleNetworkStabilizer() })
            }
        }
        Spacer(Modifier.height(6.dp))
        
        // Turbo indicator
        if (state.gamingModeActive) {
            FilterChip(selected = true, onClick = { viewModel.stopGamingMode() }, label = { Text("🎮 Turbo ON") })
            Spacer(Modifier.height(4.dp))
        }
        
        // Filter Chips
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = showGamesOnly, onClick = { showGamesOnly = true; showBoostedOnly = false }, label = { Text("🎮 Games") }, modifier = Modifier.weight(1f))
            FilterChip(selected = showBoostedOnly, onClick = { showBoostedOnly = true; showGamesOnly = false }, label = { Text("⭐ Saved") }, modifier = Modifier.weight(1f))
            FilterChip(selected = !showGamesOnly && !showBoostedOnly, onClick = { showGamesOnly = false; showBoostedOnly = false }, label = { Text("All") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        
        val filteredApps = remember(state.allApps, showGamesOnly, showBoostedOnly) {
            when { showGamesOnly -> state.allApps.filter { it.isGame || it.isUserAdded }; showBoostedOnly -> state.allApps.filter { it.isUserAdded }; else -> state.allApps }
        }
        
        Text("${filteredApps.size} apps", style = MaterialTheme.typography.bodySmall)
        
        if (state.gamesLoading) CircularProgressIndicator(modifier = Modifier.padding(32.dp))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(filteredApps) { app ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(app.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    if (app.isGame) Text(" 🎮")
                                    if (app.isUserAdded) Text(" ⭐")
                                }
                            }
                            IconButton(onClick = { if (app.isUserAdded) viewModel.removeAppFromBoost(app.packageName) else viewModel.addAppToBoost(app.packageName) }) {
                                Text(if (app.isUserAdded) "⭐" else "☆")
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick = { viewModel.launchGame(app.packageName) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text("▶ Play", style = MaterialTheme.typography.bodySmall) }
                            Button(onClick = { viewModel.launchGameWithBoost(app.packageName) }, modifier = Modifier.weight(2f), enabled = !state.boostInProgress, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                Text(if (state.boostInProgress && state.launchingGame == app.packageName) "..." else "⚡ Boost & Play", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

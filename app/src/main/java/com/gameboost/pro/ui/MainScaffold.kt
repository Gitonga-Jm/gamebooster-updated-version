package com.gameboost.pro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.gameboost.pro.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            if (!state.boostInProgress) {
                TopAppBar(
                    title = { },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Text("🏠") }, label = { Text("Home") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Text("🎮") }, label = { Text("Games") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { Text("⚡") }, label = { Text("Tools") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 },
                    icon = { Text("📊") }, label = { Text("Stats") })
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 },
                    icon = { Text("⚙️") }, label = { Text("Settings") })
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel = viewModel)
                1 -> GamesScreen(viewModel = viewModel)
                2 -> ToolsScreen(viewModel = viewModel)
                3 -> StatsScreen(viewModel = viewModel)
                4 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
    
    state.snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSnackbar()
        }
    }
}

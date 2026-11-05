package com.gameboost.pro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gameboost.pro.ui.screens.SplashScreen
import com.gameboost.pro.ui.theme.GameBoostProTheme

@Composable
fun GameBoostApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    // Auto-complete onboarding on first launch
    LaunchedEffect(Unit) {
        viewModel.completeOnboarding()
    }

    GameBoostProTheme {
        when {
            state.showSplash -> {
                SplashScreen(onFinished = viewModel::onSplashFinished)
            }
            else -> {
                MainScaffold(viewModel = viewModel)
            }
        }
    }
}

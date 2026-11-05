package com.gameboost.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gameboost.pro.ui.AppViewModel
import com.gameboost.pro.ui.theme.BackgroundDark
import com.gameboost.pro.ui.theme.NeonGreen

@Composable
fun OnboardingScreen(viewModel: AppViewModel, onComplete: () -> Unit) {
    // Auto-complete immediately
    LaunchedEffect(Unit) { viewModel.completeOnboarding(); onComplete() }
}

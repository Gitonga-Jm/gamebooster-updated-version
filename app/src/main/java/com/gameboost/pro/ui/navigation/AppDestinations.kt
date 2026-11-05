package com.gameboost.pro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class RootDestination(val route: String) {
    object Onboarding : RootDestination("onboarding")
    object Main : RootDestination("main")
}

enum class AppTab(val route: String, val label: String) {
    HOME("home", "Home"),
    GAMES("games", "Games"),
    TOOLS("tools", "Tools"),
    STATS("stats", "Stats"),
    SETTINGS("settings", "Settings")
}

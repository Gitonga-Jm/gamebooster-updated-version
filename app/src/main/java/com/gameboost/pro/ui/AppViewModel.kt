package com.gameboost.pro.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gameboost.pro.GameBoostApplication
import com.gameboost.pro.data.AppCatalogRepository
import com.gameboost.pro.data.BoostableApp
import com.gameboost.pro.data.GameCatalogRepository
import com.gameboost.pro.data.InstalledGame
import com.gameboost.pro.domain.BoostEngine
import com.gameboost.pro.domain.BoostResult
import com.gameboost.pro.domain.PreLaunchResult
import com.gameboost.pro.engine.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AppUiState(
    val onboardingComplete: Boolean = false,
    val showSplash: Boolean = true,
    val boostScore: Int = 72,
    val totalBoosts: Int = 0,
    val ramFreedMb: Int = 0,
    val lastBoostTime: Long = 0L,
    val autoBoost: Boolean = true,
    val persistentMonitor: Boolean = true,
    val thermalAlerts: Boolean = true,
    val hapticFeedback: Boolean = true,
    val ramUsedMB: Long = 0,
    val ramTotalMB: Long = 0,
    val ramPercent: Int = 0,
    val networkPing: Int = 0,
    val cpuTemp: Float = 0f,
    val networkType: String = "Unknown",
    val fastestDns: String = "",
    val batteryLevel: Int = 0,
    val runningAppsCount: Int = 0,
    val games: List<InstalledGame> = emptyList(),
    val allApps: List<BoostableApp> = emptyList(),
    val gamesLoading: Boolean = false,
    val boostInProgress: Boolean = false,
    val boostProgress: Float = 0f,
    val boostMessage: String? = null,
    val snackbarMessage: String? = null,
    val lastBoostResult: BoostResult? = null,
    val lastPreLaunchResult: PreLaunchResult? = null,
    val launchingGame: String? = null,
    val gamingModeActive: Boolean = false,
    val networkStabilizerActive: Boolean = false,
    val avgPing: Int = 0
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as GameBoostApplication).preferencesRepository
    private val gameRepo = GameCatalogRepository(application)
    private val appRepo = AppCatalogRepository(application)
    private val boostEngine = BoostEngine(application)
    val gamingEnforcer = GamingModeEnforcer(application)
    val networkStabilizer = NetworkStabilizer(application)
    val thermalCooler = ThermalCooler(application)
    val sessionTracker = GameSessionTracker(application)
    private val app = application

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var metricsJob: Job? = null
    private var boostJob: Job? = null
    private var pingMonitorJob: Job? = null

    init {
        startMetricsPolling()
        startPingMonitoring()
    }

    private fun startPingMonitoring() {
        pingMonitorJob = viewModelScope.launch {
            while (isActive) {
                try {
                    _uiState.update { it.copy(avgPing = boostEngine.getPerformanceEngine().measurePingSimple()) }
                } catch (_: Exception) {}
                delay(4000)
            }
        }
    }

    fun onSplashFinished() { _uiState.update { it.copy(showSplash = false) } }
    fun completeOnboarding() { viewModelScope.launch { try { prefs.setOnboardingComplete(true) } catch (_: Exception) {} } }

    fun loadGames() {
        viewModelScope.launch {
            _uiState.update { it.copy(gamesLoading = true) }
            try { _uiState.update { it.copy(games = gameRepo.loadInstalledGames(), gamesLoading = false) } }
            catch (e: Exception) { _uiState.update { it.copy(gamesLoading = false) } }
        }
    }

    fun loadAllApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(gamesLoading = true) }
            try { _uiState.update { it.copy(allApps = appRepo.loadAllApps(), gamesLoading = false) } }
            catch (e: Exception) { _uiState.update { it.copy(gamesLoading = false) } }
        }
    }

    fun addAppToBoost(p: String) { appRepo.addAppToBoostList(p); loadAllApps() }
    fun removeAppFromBoost(p: String) { appRepo.removeAppFromBoostList(p); loadAllApps() }

    fun toggleNetworkStabilizer() {
        try {
            if (_uiState.value.networkStabilizerActive) {
                networkStabilizer.stopStabilizing()
                _uiState.update { it.copy(networkStabilizerActive = false) }
            } else {
                networkStabilizer.startStabilizing()
                _uiState.update { it.copy(networkStabilizerActive = true) }
            }
        } catch (_: Exception) {}
    }

    fun launchGame(p: String) {
        try {
            app.packageManager.getLaunchIntentForPackage(p)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(this)
            }
        } catch (_: Exception) {}
    }

    fun launchGameWithBoost(p: String) {
        if (_uiState.value.boostInProgress) return
        boostJob = viewModelScope.launch {
            _uiState.update { it.copy(boostInProgress = true, boostProgress = 0f, boostMessage = "Boosting...") }
            try {
                val result = boostEngine.prepareForGame(p); delay(50)
                val (used, total) = boostEngine.getPerformanceEngine().getRamInfo()
                _uiState.update { it.copy(boostProgress = 0.5f, ramUsedMB = used, ramTotalMB = total) }
                if (!_uiState.value.networkStabilizerActive) {
                    networkStabilizer.startStabilizing()
                    _uiState.update { it.copy(networkStabilizerActive = true) }
                }
                gamingEnforcer.startEnforcing(p)
                _uiState.update { it.copy(boostProgress = 0.9f, gamingModeActive = true) }
                try { prefs.recordBoost(10, result.ramFreedMB.toInt()) } catch (_: Exception) {}
                _uiState.update {
                    it.copy(
                        boostProgress = 1f, boostInProgress = false,
                        lastPreLaunchResult = result, snackbarMessage = "${result.ramFreedMB}MB freed"
                    )
                }
                launchGame(p)
            } catch (e: Exception) {
                _uiState.update { it.copy(boostInProgress = false) }
                launchGame(p)
            }
        }
    }

    fun triggerBoost() {
        if (_uiState.value.boostInProgress) return
        boostJob = viewModelScope.launch {
            _uiState.update { it.copy(boostInProgress = true, boostProgress = 0f) }
            try {
                val result = boostEngine.performDeepBoost()
                val (used, total) = boostEngine.getPerformanceEngine().getRamInfo()
                gamingEnforcer.startEnforcing("system")
                if (!_uiState.value.networkStabilizerActive) {
                    networkStabilizer.startStabilizing()
                    _uiState.update { it.copy(networkStabilizerActive = true) }
                }
                _uiState.update {
                    it.copy(
                        boostProgress = 1f, boostInProgress = false,
                        lastBoostResult = result, fastestDns = result.fastestDns,
                        ramUsedMB = used, ramTotalMB = total,
                        networkPing = result.pingAfter, gamingModeActive = true
                    )
                }
                try { prefs.recordBoost(10, result.ramFreedMB.toInt()) } catch (_: Exception) {}
            } catch (e: Exception) { _uiState.update { it.copy(boostInProgress = false) } }
        }
    }

    fun stopGamingMode() { gamingEnforcer.stopEnforcing(); _uiState.update { it.copy(gamingModeActive = false) } }

    fun clearSnackbar() { _uiState.update { it.copy(snackbarMessage = null) } }
    fun setAutoBoost(e: Boolean) { viewModelScope.launch { try { prefs.setAutoBoost(e) } catch (_: Exception) {} } }
    fun setPersistentMonitor(e: Boolean) { viewModelScope.launch { try { prefs.setPersistentMonitor(e) } catch (_: Exception) {} } }
    fun setThermalAlerts(e: Boolean) { viewModelScope.launch { try { prefs.setThermalAlerts(e) } catch (_: Exception) {} } }
    fun setHapticFeedback(e: Boolean) { viewModelScope.launch { try { prefs.setHapticFeedback(e) } catch (_: Exception) {} } }

    fun refreshMetrics() {
        viewModelScope.launch {
            try {
                val e = boostEngine.getPerformanceEngine()
                val (u, t) = e.getRamInfo()
                _uiState.update {
                    it.copy(
                        ramUsedMB = u, ramTotalMB = t,
                        ramPercent = if (t > 0) ((u.toFloat() / t) * 100).toInt() else 0,
                        networkPing = e.measurePingSimple(),
                        cpuTemp = e.getCpuTemperature(),
                        networkType = e.getNetworkType(),
                        batteryLevel = e.getBatteryLevel(),
                        runningAppsCount = e.getRunningAppsCount()
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun startMetricsPolling() {
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch { while (isActive) { refreshMetrics(); delay(3000) } }
    }

    fun openNotificationSettings(c: Context) {
        try {
            c.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, c.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {}
    }

    fun openUsageAccessSettings(c: Context) {
        try { c.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
    }

    fun openBatterySettings(c: Context) {
        try {
            c.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${c.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {}
    }

    fun areNotificationsEnabled(c: Context): Boolean = NotificationManagerCompat.from(c).areNotificationsEnabled()

    override fun onCleared() {
        try { gamingEnforcer.stopEnforcing() } catch (_: Exception) {}
        try { networkStabilizer.stopStabilizing() } catch (_: Exception) {}
        try { thermalCooler.stopMonitoring() } catch (_: Exception) {}
        metricsJob?.cancel(); boostJob?.cancel(); pingMonitorJob?.cancel()
        super.onCleared()
    }
}

data class SettingsSnapshot(
    val autoBoost: Boolean = true,
    val persistentMonitor: Boolean = true,
    val thermalAlerts: Boolean = true,
    val haptic: Boolean = true
)

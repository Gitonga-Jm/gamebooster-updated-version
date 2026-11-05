package com.gameboost.pro.engine

import android.app.*
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class GamingModeEnforcer(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var enforcementJob: Job? = null
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val performanceEngine = PerformanceEngine(context)
    private val callLagPreventer = CallLagPreventer(context)
    private val audioOptimizer = AudioOptimizer(context)
    
    var isEnforcing = false
    private set
    var currentPing = 0; var currentTemp = 0f; var currentRam = 0
    
    companion object {
        const val NOTIFICATION_ID = 2001; const val CHANNEL_ID = "gaming_mode_stats"
    }
    
    fun startEnforcing(gamePackage: String) {
        if (isEnforcing) return
        isEnforcing = true
        try {
            createNotificationChannel()
            callLagPreventer.enableGamingFocus()
            audioOptimizer.optimizeForGaming()
            lockWiFiForGaming()
            lockCpuPerformance()
            updateNotification()
            
            enforcementJob = scope.launch {
                while (isActive && isEnforcing) {
                    try {
                        updateStats()
                        updateNotification()
                        keepNetworkAlive()
                        delay(3000)
                    } catch (_: Exception) { }
                }
            }
        } catch (e: Exception) { isEnforcing = false }
    }
    
    fun stopEnforcing() {
        isEnforcing = false
        enforcementJob?.cancel()
        callLagPreventer.disableGamingFocus()
        audioOptimizer.restoreAudio()
        unlockNetworks()
        try { notificationManager.cancel(NOTIFICATION_ID) } catch (_: Exception) { }
    }
    
    private fun updateStats() {
        try {
            currentPing = performanceEngine.measurePingSimple()
            currentTemp = performanceEngine.getCpuTemperature()
            val (used, total) = performanceEngine.getRamInfo()
            currentRam = if (total > 0) ((used.toFloat() / total) * 100).toInt() else 0
        } catch (_: Exception) { }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Gaming", NotificationManager.IMPORTANCE_LOW)) } catch (_: Exception) { }
        }
    }
    
    private fun updateNotification() {
        try {
            notificationManager.notify(NOTIFICATION_ID,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("🎮 Gaming Mode").setContentText("Ping: ${currentPing}ms | RAM: ${currentRam}%")
                    .setSmallIcon(android.R.drawable.ic_menu_manage).setOngoing(true).build())
        } catch (_: Exception) { }
    }
    
    private fun lockCpuPerformance() {
        try {
            for (p in listOf("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor")) {
                try { val f = java.io.File(p); if (f.exists()) f.writeText("performance") } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }
    
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null
    
    private fun lockWiFiForGaming() {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            wifiLock = wm?.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "GB:WiFi")
            wifiLock?.acquire()
        } catch (_: Exception) { }
    }
    
    private fun keepNetworkAlive() {
        try { connectivityManager.activeNetwork?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) connectivityManager.bindProcessToNetwork(it) } } catch (_: Exception) { }
    }
    
    private fun unlockNetworks() {
        try { wifiLock?.release() } catch (_: Exception) { }
        try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) connectivityManager.bindProcessToNetwork(null) } catch (_: Exception) { }
    }
}

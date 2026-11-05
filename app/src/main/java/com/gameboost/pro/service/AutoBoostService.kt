package com.gameboost.pro.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gameboost.pro.engine.GamingModeEnforcer
import com.gameboost.pro.engine.NetworkStabilizer
import com.gameboost.pro.engine.PerformanceEngine
import kotlinx.coroutines.*

class AutoBoostService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null
    
    companion object {
        var isRunning = false
        fun start(ctx: Context) { if (!isRunning) ctx.startForegroundService(Intent(ctx, AutoBoostService::class.java)) }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, AutoBoostService::class.java)) }
    }
    
    override fun onBind(i: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("auto_boost", "Auto Boost", NotificationManager.IMPORTANCE_LOW))
        }
        startForeground(7001, NotificationCompat.Builder(this, "auto_boost")
            .setContentTitle("🎮 Auto Boost Ready").setSmallIcon(android.R.drawable.ic_menu_manage).setOngoing(true).build())
        startMonitoring()
    }
    
    private fun startMonitoring() {
        val engine = PerformanceEngine(this)
        val gamingEnforcer = GamingModeEnforcer(this)
        val networkStabilizer = NetworkStabilizer(this)
        var lastGamePackage: String? = null
        
        monitorJob = scope.launch {
            while (isActive) {
                try {
                    val topPkg = getForegroundPackage()
                    if (topPkg != null && topPkg != packageName && topPkg != lastGamePackage) {
                        val gameKeywords = listOf("game", "play", "pubg", "cod", "fortnite", "minecraft",
                            "roblox", "efootball", "pes", "fifa", "ml", "freefire", "genshin")
                        if (gameKeywords.any { topPkg.contains(it) }) {
                            lastGamePackage = topPkg
                            engine.safeMemoryOptimize()
                            networkStabilizer.startStabilizing()
                            gamingEnforcer.startEnforcing(topPkg)
                        }
                    }
                } catch (_: Exception) { }
                delay(3000)
            }
        }
    }
    
    private fun getForegroundPackage(): String? {
        return try {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            am.runningAppProcesses?.firstOrNull { it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }?.processName
        } catch (_: Exception) { null }
    }
    
    override fun onDestroy() { isRunning = false; monitorJob?.cancel(); super.onDestroy() }
}

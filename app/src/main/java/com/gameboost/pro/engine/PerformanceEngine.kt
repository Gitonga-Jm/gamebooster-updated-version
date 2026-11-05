package com.gameboost.pro.engine

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class SystemStats(
    val ramUsedMB: Long = 0, val ramTotalMB: Long = 0, val ramPercent: Int = 0,
    val cpuTemp: Float = 0f, val networkPing: Int = 0,
    val activeNetwork: String = "Unknown", val batteryLevel: Int = 0,
    val batteryTemp: Float = 0f, val runningAppsCount: Int = 0
)

class PerformanceEngine(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    fun getRamInfo(): Pair<Long, Long> {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return Pair((memInfo.totalMem - memInfo.availMem) / 1048576, memInfo.totalMem / 1048576)
    }
    
    fun getBatteryLevel(): Int {
        return try {
            val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100) / scale else -1
        } catch (_: Exception) { -1 }
    }
    
    fun getBatteryTemperature(): Float {
        return try {
            val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            (intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1).let { if (it > 0) it / 10f else 0f }
        } catch (_: Exception) { 0f }
    }
    
    fun getRunningAppsCount() = try { activityManager.runningAppProcesses?.size ?: 0 } catch (_: Exception) { 0 }
    
    /**
     * SAFE memory optimization - only suggests GC, never kills processes
     */
    fun safeMemoryOptimize(): Long {
        val before = getRamInfo().first
        try {
            // Only suggest garbage collection - NEVER kill processes
            System.gc()
            Runtime.getRuntime().gc()
            System.runFinalization()
        } catch (_: Exception) { }
        val after = getRamInfo().first
        return maxOf(0, before - after)
    }
    
    fun getCpuTemperature(): Float {
        return try {
            for (file in listOf("/sys/class/thermal/thermal_zone0/temp", "/sys/class/thermal/thermal_zone1/temp")) {
                val f = File(file)
                if (f.exists()) { val t = (f.readText().trim().toFloatOrNull()?:0f)/1000f; if (t in 20f..100f) return t }
            }
            getBatteryTemperature()
        } catch (_: Exception) { getBatteryTemperature() }
    }
    
    fun measurePingSimple(host: String = "8.8.8.8"): Int {
        return try {
            val process = Runtime.getRuntime().exec("ping -c 1 -W 1 $host")
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            var ping = -1
            reader.forEachLine { line ->
                if (line.contains("time=")) { val idx = line.indexOf("time=")+5; val end = line.indexOf(" ms", idx); if (end > idx) ping = line.substring(idx, end).trim().toFloatOrNull()?.toInt() ?: -1 }
            }
            process.waitFor(); reader.close(); ping
        } catch (_: Exception) { -1 }
    }
    
    fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "Unknown"
        return when { caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"; caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"; else -> "Connected" }
    }
}

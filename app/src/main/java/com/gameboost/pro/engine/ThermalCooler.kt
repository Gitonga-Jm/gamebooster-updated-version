package com.gameboost.pro.engine

import android.app.*
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class ThermalCooler(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var coolingJob: Job? = null
    private val performanceEngine = PerformanceEngine(context)
    
    private val _tempStatus = MutableStateFlow(TempStatus())
    val tempStatus: StateFlow<TempStatus> = _tempStatus
    
    var isCooling = false
    private set
    
    companion object {
        const val CHANNEL_ID = "thermal_alerts"
        const val NOTIFICATION_ID = 6001
    }
    
    data class TempStatus(
        val cpuTemp: Float = 0f,
        val batteryTemp: Float = 0f,
        val status: String = "Normal",
        val coolingActive: Boolean = false,
        val appsKilled: Int = 0,
        val tempDrop: Float = 0f
    )
    
    fun startMonitoring() {
        createNotificationChannel()
        
        coolingJob = scope.launch {
            var lastTemp = 0f
            
            while (isActive) {
                try {
                    val cpuTemp = performanceEngine.getCpuTemperature()
                    val batteryTemp = performanceEngine.getBatteryTemperature()
                    
                    val status = when {
                        cpuTemp > 48f || batteryTemp > 42f -> "CRITICAL"
                        cpuTemp > 43f || batteryTemp > 38f -> "WARNING"
                        cpuTemp > 38f -> "WARM"
                        else -> "NORMAL"
                    }
                    
                    if (status == "CRITICAL" || status == "WARNING") {
                        if (!isCooling) startEmergencyCooling()
                    }
                    
                    val tempDrop = if (isCooling && lastTemp > 0) lastTemp - cpuTemp else 0f
                    
                    _tempStatus.value = TempStatus(cpuTemp, batteryTemp, status, isCooling, 0, tempDrop)
                    
                    if (status == "CRITICAL") sendTempAlert(cpuTemp, batteryTemp)
                    
                    lastTemp = cpuTemp
                    delay(5000)
                } catch (_: Exception) { }
            }
        }
    }
    
    fun startEmergencyCooling() {
        if (isCooling) return
        isCooling = true
        
        scope.launch {
            try {
                // Safe cooling: lower CPU/GPU to powersave
                try {
                    for (p in listOf("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "/sys/devices/system/cpu/cpufreq/policy0/scaling_governor")) {
                        try { File(p).writeText("powersave") } catch (_: Exception) { }
                    }
                } catch (_: Exception) { }
                
                try {
                    for (p in listOf("/sys/class/kgsl/kgsl-3d0/devfreq/governor")) {
                        try { File(p).writeText("powersave") } catch (_: Exception) { }
                    }
                } catch (_: Exception) { }
                
                // Reduce brightness
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && android.provider.Settings.System.canWrite(context)) {
                        android.provider.Settings.System.putInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, 50)
                    }
                } catch (_: Exception) { }
                
                System.gc()
                
                _tempStatus.value = _tempStatus.value.copy(coolingActive = true)
                delay(30000)
                
                // Restore performance
                for (p in listOf("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")) {
                    try { File(p).writeText("performance") } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
            isCooling = false
        }
    }
    
    fun manualCool(): TempStatus {
        startEmergencyCooling()
        return _tempStatus.value
    }
    
    private fun sendTempAlert(cpuTemp: Float, batteryTemp: Float) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("🌡️ Device Hot!")
                    .setContentText("CPU: ${String.format("%.1f", cpuTemp)}°C - Cooling activated")
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build())
        } catch (_: Exception) { }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Thermal Alerts", NotificationManager.IMPORTANCE_HIGH))
        }
    }
    
    fun stopMonitoring() { coolingJob?.cancel(); isCooling = false }
}

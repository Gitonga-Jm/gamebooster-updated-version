package com.gameboost.pro.domain

import android.content.Context
import android.content.Intent
import com.gameboost.pro.engine.PerformanceEngine
import com.gameboost.pro.network.DnsOptimizer
import kotlinx.coroutines.*

data class BoostResult(
    val ramFreedMB: Long = 0, val fastestDns: String = "",
    val dnsLatency: Int = 0, val pingBefore: Int = 0,
    val pingAfter: Int = 0, val cpuTemp: Float = 0f,
    val batteryLevel: Int = 0
)

data class PreLaunchResult(
    val ramFreedMB: Long = 0,
    val pingBefore: Int = 0, val pingAfter: Int = 0,
    val dnsUsed: String = "", val cpuTemp: Float = 0f,
    val batteryLevel: Int = 0
)

class BoostEngine(private val context: Context) {
    
    private val performanceEngine = PerformanceEngine(context)
    private val dnsOptimizer = DnsOptimizer(context)
    
    suspend fun performDeepBoost(): BoostResult = withContext(Dispatchers.IO) {
        val pingBefore = performanceEngine.measurePingSimple()
        val ramFreed = performanceEngine.safeMemoryOptimize()
        val fastestDns = dnsOptimizer.quickDnsTest()
        val pingAfter = performanceEngine.measurePingSimple()
        
        BoostResult(
            ramFreedMB = ramFreed, fastestDns = fastestDns.name,
            dnsLatency = fastestDns.latency, pingBefore = pingBefore,
            pingAfter = pingAfter, cpuTemp = performanceEngine.getCpuTemperature(),
            batteryLevel = performanceEngine.getBatteryLevel()
        )
    }
    
    suspend fun prepareForGame(packageName: String): PreLaunchResult = withContext(Dispatchers.IO) {
        val pingBefore = performanceEngine.measurePingSimple()
        val ramFreed = performanceEngine.safeMemoryOptimize()
        val dns = dnsOptimizer.quickDnsTest()
        val pingAfter = performanceEngine.measurePingSimple()
        
        PreLaunchResult(
            ramFreedMB = ramFreed, pingBefore = pingBefore, pingAfter = pingAfter,
            dnsUsed = dns.name, cpuTemp = performanceEngine.getCpuTemperature(),
            batteryLevel = performanceEngine.getBatteryLevel()
        )
    }
    
    fun launchGame(packageName: String): Intent? {
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        } catch (_: Exception) { null }
    }
    
    fun getPerformanceEngine(): PerformanceEngine = performanceEngine
    fun getDnsOptimizer(): DnsOptimizer = dnsOptimizer
}

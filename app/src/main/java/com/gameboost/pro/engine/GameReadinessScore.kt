package com.gameboost.pro.engine

import android.content.Context
import kotlinx.coroutines.*
import kotlin.math.roundToInt

data class ReadinessReport(
    val score: Int = 0,
    val rating: String = "Unknown",
    val emoji: String = "❓",
    val pingScore: Int = 0,
    val jitterScore: Int = 0,
    val packetLossScore: Int = 0,
    val tempScore: Int = 0,
    val batteryScore: Int = 0,
    val cpuScore: Int = 0,
    val ramScore: Int = 0,
    val problems: List<String> = emptyList(),
    val readyForCompetitive: Boolean = false
)

class GameReadinessScore(private val context: Context) {
    
    private val networkDiagnostics = NetworkDiagnostics(context)
    private val performanceEngine = PerformanceEngine(context)
    
    suspend fun calculate(): ReadinessReport = withContext(Dispatchers.IO) {
        val problems = mutableListOf<String>()
        
        // 1. Network diagnostics (40 points max)
        val network = networkDiagnostics.runFullDiagnostics()
        val pingScore = when { network.ping < 30 -> 10; network.ping < 60 -> 7; network.ping < 100 -> 4; network.ping < 150 -> 1; else -> 0 }
        val jitterScore = when { network.jitter < 10 -> 10; network.jitter < 20 -> 6; network.jitter < 40 -> 3; else -> 0 }
        val packetLossScore = when { network.packetLoss < 1 -> 10; network.packetLoss < 5 -> 5; network.packetLoss < 10 -> 2; else -> 0 }
        val dnsScore = when { network.dnsLatency < 30 -> 10; network.dnsLatency < 80 -> 6; network.dnsLatency < 150 -> 3; else -> 0 }
        
        if (network.ping > 100) problems.add("High ping: ${network.ping}ms")
        if (network.packetLoss > 5) problems.add("Packet loss: ${String.format("%.1f", network.packetLoss)}%")
        
        // 2. Temperature (20 points max)
        val cpuTemp = performanceEngine.getCpuTemperature()
        val batteryTemp = performanceEngine.getBatteryTemperature()
        val tempScore = when {
            cpuTemp < 40 -> 20; cpuTemp < 45 -> 15; cpuTemp < 50 -> 8; cpuTemp < 55 -> 3; else -> 0
        }
        if (cpuTemp > 48) problems.add("Device overheating: ${String.format("%.1f", cpuTemp)}°C")
        
        // 3. Battery (15 points max)
        val battery = performanceEngine.getBatteryLevel()
        val batteryScore = when {
            battery > 50 -> 15; battery > 30 -> 10; battery > 15 -> 5; battery > 5 -> 2; else -> 0
        }
        if (battery < 15) problems.add("Battery very low: ${battery}%")
        
        // 4. CPU/Memory (25 points max)
        val (usedRam, totalRam) = performanceEngine.getRamInfo()
        val ramPercent = if (totalRam > 0) ((usedRam.toFloat() / totalRam) * 100).toInt() else 50
        val ramScore = when { ramPercent < 50 -> 15; ramPercent < 70 -> 10; ramPercent < 85 -> 5; else -> 2 }
        val cpuScore = 10 // Default score, CPU is hard to measure without root
        if (ramPercent > 85) problems.add("Memory almost full: ${ramPercent}%")
        
        // Total score
        val totalScore = (pingScore + jitterScore + packetLossScore + dnsScore + 
                         tempScore + batteryScore + ramScore + cpuScore).coerceIn(0, 100)
        
        // Rating
        val (rating, emoji, ready) = when {
            totalScore >= 90 -> Triple("Ready for Competitive Gaming", "🏆", true)
            totalScore >= 75 -> Triple("Good for Gaming", "✅", true)
            totalScore >= 60 -> Triple("Playable - Minor Issues", "⚠️", false)
            totalScore >= 40 -> Triple("High Risk of Lag", "🔴", false)
            else -> Triple("Not Recommended for Gaming", "⛔", false)
        }
        
        ReadinessReport(
            score = totalScore, rating = rating, emoji = emoji,
            pingScore = pingScore, jitterScore = jitterScore,
            packetLossScore = packetLossScore, tempScore = tempScore,
            batteryScore = batteryScore, cpuScore = cpuScore, ramScore = ramScore,
            problems = problems, readyForCompetitive = ready
        )
    }
}

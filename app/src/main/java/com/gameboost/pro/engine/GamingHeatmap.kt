package com.gameboost.pro.engine

data class HeatmapData(
    val gameName: String,
    val fpsLow: Int = 0, val fpsAvg: Int = 0, val fpsHigh: Int = 0,
    val pingLow: Int = 0, val pingAvg: Int = 0, val pingHigh: Int = 0,
    val tempLow: Float = 0f, val tempAvg: Float = 0f, val tempHigh: Float = 0f,
    val sessionsCount: Int = 0
)

class GamingHeatmap(private val sessionTracker: GameSessionTracker) {
    
    fun getHeatmapForGame(packageName: String): HeatmapData {
        val sessions = sessionTracker.allSessions.value.filter { it.packageName == packageName }
        if (sessions.isEmpty()) return HeatmapData(packageName)
        
        val fpsValues = sessions.map { it.avgFps }.filter { it > 0 }
        val pingValues = sessions.map { it.avgPing }.filter { it > 0 }
        val tempValues = sessions.map { it.maxTemp }.filter { it > 0 }
        
        return HeatmapData(
            gameName = sessions.firstOrNull()?.gameName ?: packageName,
            fpsLow = fpsValues.minOrNull() ?: 0,
            fpsAvg = if (fpsValues.isNotEmpty()) fpsValues.average().toInt() else 0,
            fpsHigh = fpsValues.maxOrNull() ?: 0,
            pingLow = pingValues.minOrNull() ?: 0,
            pingAvg = if (pingValues.isNotEmpty()) pingValues.average().toInt() else 0,
            pingHigh = pingValues.maxOrNull() ?: 0,
            tempLow = tempValues.minOrNull() ?: 0f,
            tempAvg = if (tempValues.isNotEmpty()) tempValues.average().toFloat() else 0f,
            tempHigh = tempValues.maxOrNull() ?: 0f,
            sessionsCount = sessions.size
        )
    }
    
    fun getPerformanceRating(heatmap: HeatmapData): String {
        val fpsScore = when { heatmap.fpsAvg >= 55 -> 3; heatmap.fpsAvg >= 30 -> 2; heatmap.fpsAvg > 0 -> 1; else -> 0 }
        val pingScore = when { heatmap.pingAvg in 1..40 -> 3; heatmap.pingAvg in 41..80 -> 2; heatmap.pingAvg > 80 -> 1; else -> 0 }
        val tempScore = when { heatmap.tempHigh < 40 -> 3; heatmap.tempHigh < 48 -> 2; heatmap.tempHigh > 48 -> 1; else -> 0 }
        val total = fpsScore + pingScore + tempScore
        return when { total >= 7 -> "⭐⭐⭐ Excellent"; total >= 5 -> "⭐⭐ Good"; total >= 3 -> "⭐ Fair"; else -> "Poor" }
    }
}

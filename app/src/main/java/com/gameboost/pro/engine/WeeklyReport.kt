package com.gameboost.pro.engine

import android.content.Context
import android.content.Intent

class WeeklyReport(private val sessionTracker: GameSessionTracker, private val context: Context) {
    
    fun generateReport(): String {
        val sessions = sessionTracker.allSessions.value
        if (sessions.isEmpty()) return "No gaming data yet!"
        
        val weekAgo = System.currentTimeMillis() - 7 * 86400000L
        val weekSessions = sessions.filter { it.startTime > weekAgo }
        
        if (weekSessions.isEmpty()) return "No sessions this week. Play some games!"
        
        val totalTime = weekSessions.sumOf { it.durationMs } / 60000
        val avgFps = weekSessions.map { it.avgFps }.filter { it > 0 }.average().toInt()
        val avgPing = weekSessions.map { it.avgPing }.filter { it > 0 }.average().toInt()
        val totalRamFreed = weekSessions.sumOf { it.ramFreed }
        val topGame = weekSessions.groupBy { it.gameName }.maxByOrNull { it.value.size }?.key ?: "Unknown"
        val maxTemp = weekSessions.maxOfOrNull { it.maxTemp } ?: 0f
        
        return buildString {
            appendLine("📊 GAME BOOSTER WEEKLY REPORT")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("🎮 Games played: ${weekSessions.size}")
            appendLine("⏱ Total play time: ${totalTime}min")
            appendLine("📈 Avg FPS: $avgFps")
            appendLine("📡 Avg Ping: ${avgPing}ms")
            appendLine("🧹 RAM freed: ${totalRamFreed}MB")
            appendLine("🎯 Top game: $topGame")
            appendLine("🌡 Max temp: ${String.format("%.1f", maxTemp)}°C")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("Boosted by Game Booster 🚀")
        }
    }
    
    fun shareReport() {
        val report = generateReport()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, report)
            putExtra(Intent.EXTRA_SUBJECT, "My Gaming Stats - Game Booster")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Gaming Stats"))
    }
}

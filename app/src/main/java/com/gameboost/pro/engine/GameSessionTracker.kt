package com.gameboost.pro.engine

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GameSession(
    val gameName: String,
    val packageName: String,
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long = 0,
    var avgFps: Int = 0,
    var avgPing: Int = 0,
    var maxTemp: Float = 0f,
    var batteryStart: Int = 0,
    var batteryEnd: Int = 0,
    var ramFreed: Long = 0,
    var appsKilled: Int = 0
) {
    val durationMs get() = if (endTime > 0) endTime - startTime else System.currentTimeMillis() - startTime
    val durationMin get() = durationMs / 60000
    val batteryDrain get() = batteryStart - batteryEnd
}

class GameSessionTracker(context: Context) {
    private val prefs = context.getSharedPreferences("game_sessions_v2", Context.MODE_PRIVATE)
    
    private val _currentSession = MutableStateFlow<GameSession?>(null)
    val currentSession: StateFlow<GameSession?> = _currentSession
    
    private val _allSessions = MutableStateFlow<List<GameSession>>(emptyList())
    val allSessions: StateFlow<List<GameSession>> = _allSessions
    
    private val _weeklyStats = MutableStateFlow("No sessions yet")
    val weeklyStats: StateFlow<String> = _weeklyStats
    
    init { loadSessions() }
    
    fun startSession(name: String, pkg: String, battery: Int, ram: Long, appsKilled: Int) {
        _currentSession.value = GameSession(
            gameName = name,
            packageName = pkg,
            startTime = System.currentTimeMillis(),
            batteryStart = battery,
            ramFreed = ram,
            appsKilled = appsKilled
        )
    }
    
    fun updateStats(fps: Int, ping: Int, temp: Float) {
        _currentSession.value?.let { session ->
            _currentSession.value = session.copy(
                avgFps = if (session.avgFps == 0) fps else (session.avgFps + fps) / 2,
                avgPing = if (session.avgPing == 0) ping else (session.avgPing + ping) / 2,
                maxTemp = maxOf(session.maxTemp, temp)
            )
        }
    }
    
    fun endSession(batteryEnd: Int): GameSession? {
        val session = _currentSession.value ?: return null
        val final = session.copy(
            endTime = System.currentTimeMillis(),
            batteryEnd = batteryEnd
        )
        
        val list = _allSessions.value.toMutableList()
        list.add(0, final)
        if (list.size > 100) list.removeAt(list.lastIndex)
        _allSessions.value = list
        
        saveSessions(list)
        updateWeeklyStats()
        _currentSession.value = null
        
        return final
    }
    
    fun getTotalPlayTime(): Long = _allSessions.value.sumOf { it.durationMs }
    
    fun getSessionCount(): Int = _allSessions.value.size
    
    fun getAverageFps(): Int {
        val fps = _allSessions.value.map { it.avgFps }.filter { it > 0 }
        return if (fps.isNotEmpty()) fps.average().toInt() else 0
    }
    
    fun getAveragePing(): Int {
        val pings = _allSessions.value.map { it.avgPing }.filter { it > 0 }
        return if (pings.isNotEmpty()) pings.average().toInt() else 0
    }
    
    fun getTotalRamFreed(): Long = _allSessions.value.sumOf { it.ramFreed }
    
    fun getTotalAppsKilled(): Int = _allSessions.value.sumOf { it.appsKilled }
    
    fun getTopGames(): List<Pair<String, Long>> {
        return _allSessions.value
            .groupBy { it.gameName }
            .mapValues { it.value.sumOf { s -> s.durationMs } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }
    }
    
    private fun updateWeeklyStats() {
        val weekAgo = System.currentTimeMillis() - 7 * 86400000L
        val sessions = _allSessions.value.filter { it.startTime > weekAgo }
        
        if (sessions.isEmpty()) {
            _weeklyStats.value = "No sessions this week"
            return
        }
        
        val totalMin = sessions.sumOf { it.durationMs } / 60000
        val count = sessions.size
        val avgFps = sessions.map { it.avgFps }.filter { it > 0 }.average().toInt()
        val avgPing = sessions.map { it.avgPing }.filter { it > 0 }.average().toInt()
        
        _weeklyStats.value = "$count sessions | ${totalMin}min | Avg FPS: $avgFps | Ping: ${avgPing}ms"
    }
    
    private fun saveSessions(list: List<GameSession>) {
        try {
            prefs.edit().putString("sessions", list.take(50).joinToString("||") { s ->
                "${s.gameName},${s.packageName},${s.startTime},${s.endTime},${s.avgFps},${s.avgPing},${s.maxTemp},${s.batteryStart},${s.batteryEnd},${s.ramFreed},${s.appsKilled}"
            }).apply()
        } catch (_: Exception) { }
    }
    
    private fun loadSessions() {
        try {
            val data = prefs.getString("sessions", "") ?: ""
            if (data.isEmpty()) return
            _allSessions.value = data.split("||").mapNotNull { line ->
                val p = line.split(",")
                if (p.size >= 11) GameSession(p[0], p[1], p[2].toLongOrNull()?:0, p[3].toLongOrNull()?:0,
                    p[4].toIntOrNull()?:0, p[5].toIntOrNull()?:0, p[6].toFloatOrNull()?:0f,
                    p[7].toIntOrNull()?:0, p[8].toIntOrNull()?:0, p[9].toLongOrNull()?:0, p[10].toIntOrNull()?:0)
                else null
            }
            updateWeeklyStats()
        } catch (_: Exception) { }
    }
}

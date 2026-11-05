package com.gameboost.pro.engine

import android.content.Context

class AiBoostPredictor(context: Context) {
    private val prefs = context.getSharedPreferences("ai_boost", Context.MODE_PRIVATE)
    
    fun recordGameLaunch(packageName: String) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        val key = "play_${packageName}_${day}_${hour}"
        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count).apply()
    }
    
    fun predictNextGame(): String? {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        val allKeys = prefs.all.keys.filter { it.startsWith("play_") }
        var bestMatch: Pair<String, Int>? = null
        
        for (key in allKeys) {
            if (key.contains("_${day}_${hour}") || key.contains("_${day}_${hour-1}") || key.contains("_${day}_${hour+1}")) {
                val count = prefs.getInt(key, 0)
                val pkg = key.removePrefix("play_").replace("_${day}_${hour}", "").replace("_${day}_${hour-1}", "").replace("_${day}_${hour+1}", "")
                if (bestMatch == null || count > bestMatch.second) {
                    bestMatch = Pair(pkg, count)
                }
            }
        }
        return if (bestMatch != null && bestMatch.second >= 3) bestMatch.first else null
    }
    
    fun shouldPreBoost(): Boolean = predictNextGame() != null
}

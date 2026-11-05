package com.gameboost.pro.engine

class BatteryPredictor {
    
    data class BatteryPrediction(
        val currentLevel: Int,
        val estimatedMinutes: Int,
        val gameName: String = "",
        val drainRatePerHour: Float = 0f
    )
    
    // Average battery drain per hour for common games
    private val gameDrainRates = mapOf(
        "efootball" to 18f,
        "pubg" to 25f,
        "cod" to 22f,
        "freefire" to 15f,
        "mobilelegends" to 12f,
        "genshin" to 30f,
        "minecraft" to 10f,
        "roblox" to 8f
    )
    
    fun predict(currentBattery: Int, gamePackage: String): BatteryPrediction {
        val drainRate = gameDrainRates.entries
            .find { gamePackage.contains(it.key) }?.value ?: 15f
        
        val minutesLeft = ((currentBattery.toFloat() / drainRate) * 60).toInt()
        
        return BatteryPrediction(
            currentLevel = currentBattery,
            estimatedMinutes = minutesLeft.coerceAtLeast(0),
            gameName = gamePackage,
            drainRatePerHour = drainRate
        )
    }
    
    fun getTimeString(minutes: Int): String {
        return when {
            minutes >= 120 -> "${minutes / 60}h ${minutes % 60}m"
            minutes >= 60 -> "1h ${minutes - 60}m"
            minutes > 0 -> "${minutes} min"
            else -> "Battery low!"
        }
    }
}

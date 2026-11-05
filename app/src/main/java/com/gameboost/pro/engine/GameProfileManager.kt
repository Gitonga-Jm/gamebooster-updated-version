package com.gameboost.pro.engine

import android.content.Context

data class GameProfile(
    val packageName: String,
    val gameName: String,
    val cpuBoost: Boolean = true,
    val gpuBoost: Boolean = true,
    val networkPriority: Boolean = true,
    val killApps: Boolean = true,
    val preferredDns: String = "auto",
    val brightness: Int = 80,
    val volumeBoost: Boolean = false,
    val voiceChatPriority: Boolean = false
)

class GameProfileManager(context: Context) {
    private val prefs = context.getSharedPreferences("game_profiles_v2", Context.MODE_PRIVATE)
    
    fun saveProfile(profile: GameProfile) {
        prefs.edit().putString("profile_${profile.packageName}", 
            "${profile.gameName}|${profile.cpuBoost}|${profile.gpuBoost}|${profile.networkPriority}|${profile.killApps}|${profile.preferredDns}|${profile.brightness}|${profile.volumeBoost}|${profile.voiceChatPriority}"
        ).apply()
    }
    
    fun getProfile(packageName: String): GameProfile? {
        val data = prefs.getString("profile_$packageName", null) ?: return null
        val parts = data.split("|")
        if (parts.size < 9) return null
        return GameProfile(packageName, parts[0], parts[1].toBoolean(), parts[2].toBoolean(),
            parts[3].toBoolean(), parts[4].toBoolean(), parts[5], parts[6].toIntOrNull()?:80,
            parts[7].toBoolean(), parts[8].toBoolean())
    }
    
    fun getDefaultProfiles(): List<GameProfile> = listOf(
        GameProfile("com.konami.pesam", "eFootball", true, true, true, true, "cloudflare", 85, false, false),
        GameProfile("com.tencent.ig", "PUBG Mobile", true, true, true, true, "google", 90, true, true),
        GameProfile("com.activision.callofduty.warzone", "Call of Duty", true, true, true, true, "cloudflare", 85, true, true),
        GameProfile("com.mobile.legends", "Mobile Legends", true, true, true, true, "cloudflare", 80, false, false),
        GameProfile("com.dts.freefireth", "Free Fire", true, false, true, true, "google", 75, false, true)
    )
}

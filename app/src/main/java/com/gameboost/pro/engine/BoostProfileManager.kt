package com.gameboost.pro.engine

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BoostProfile(
    val packageName: String,
    val cpuLock: Boolean = true,
    val gpuLock: Boolean = true,
    val networkBoost: Boolean = true,
    val killApps: Boolean = true,
    val preferredDns: String = "auto",
    val brightness: Int = 80,
    val priority: String = "balanced" // performance, balanced, battery
)

class BoostProfileManager(context: Context) {
    private val prefs = context.getSharedPreferences("boost_profiles", Context.MODE_PRIVATE)
    
    private val _profiles = MutableStateFlow<Map<String, BoostProfile>>(emptyMap())
    val profiles: StateFlow<Map<String, BoostProfile>> = _profiles
    
    init { loadProfiles() }
    
    fun getProfile(packageName: String): BoostProfile {
        return _profiles.value[packageName] ?: BoostProfile(packageName)
    }
    
    fun saveProfile(profile: BoostProfile) {
        val map = _profiles.value.toMutableMap()
        map[profile.packageName] = profile
        _profiles.value = map
        
        prefs.edit().putString("profile_${profile.packageName}", 
            "${profile.cpuLock},${profile.gpuLock},${profile.networkBoost},${profile.killApps},${profile.preferredDns},${profile.brightness},${profile.priority}"
        ).apply()
    }
    
    private fun loadProfiles() {
        val map = mutableMapOf<String, BoostProfile>()
        prefs.all.keys.filter { it.startsWith("profile_") }.forEach { key ->
            val pkg = key.removePrefix("profile_")
            prefs.getString(key, "")?.split(",")?.let { parts ->
                if (parts.size >= 7) {
                    map[pkg] = BoostProfile(pkg, parts[0].toBoolean(), parts[1].toBoolean(), 
                        parts[2].toBoolean(), parts[3].toBoolean(), parts[4], parts[5].toIntOrNull()?:80, parts[6])
                }
            }
        }
        _profiles.value = map
    }
}

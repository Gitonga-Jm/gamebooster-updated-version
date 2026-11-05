package com.gameboost.pro.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BoostableApp(
    val packageName: String,
    val label: String,
    val isGame: Boolean = false,
    val isUserAdded: Boolean = false
)

class AppCatalogRepository(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("boosted_apps", Context.MODE_PRIVATE)
    
    /**
     * Get all installed apps that can be boosted
     */
    suspend fun loadAllApps(): List<BoostableApp> = withContext(Dispatchers.IO) {
        val apps = mutableListOf<BoostableApp>()
        val pm = context.packageManager
        
        // Get user-added apps
        val userAdded = prefs.getStringSet("user_added_apps", emptySet()) ?: emptySet()
        
        // Get all installed apps
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        // Game keywords
        val gameKeywords = listOf("game", "play", "pubg", "cod", "fortnite", "minecraft", "roblox",
            "efootball", "pes", "fifa", "ml", "mobilelegends", "freefire", "genshin", "impact")
        
        for (app in installedApps) {
            // Skip system apps
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
            if (app.packageName == context.packageName) continue
            
            val label = pm.getApplicationLabel(app).toString()
            val packageLower = app.packageName.lowercase()
            val labelLower = label.lowercase()
            
            // Check if it's a game
            val isGame = gameKeywords.any { packageLower.contains(it) || labelLower.contains(it) }
            
            // Check if user added
            val isUserAdded = userAdded.contains(app.packageName)
            
            // Include if game, user-added, or has launch intent
            val hasLaunchIntent = pm.getLaunchIntentForPackage(app.packageName) != null
            
            if (isGame || isUserAdded || hasLaunchIntent) {
                apps.add(BoostableApp(app.packageName, label, isGame, isUserAdded))
            }
        }
        
        apps.sortedByDescending { it.isGame || it.isUserAdded }
    }
    
    /**
     * Add an app to the boost list
     */
    fun addAppToBoostList(packageName: String) {
        val current = prefs.getStringSet("user_added_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(packageName)
        prefs.edit().putStringSet("user_added_apps", current).apply()
    }
    
    /**
     * Remove an app from the boost list
     */
    fun removeAppFromBoostList(packageName: String) {
        val current = prefs.getStringSet("user_added_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(packageName)
        prefs.edit().putStringSet("user_added_apps", current).apply()
    }
    
    /**
     * Get all user-added apps
     */
    fun getUserAddedApps(): Set<String> {
        return prefs.getStringSet("user_added_apps", emptySet()) ?: emptySet()
    }
    
    /**
     * Get launch intent for any app
     */
    fun getLaunchIntent(packageName: String): Intent? {
        return context.packageManager.getLaunchIntentForPackage(packageName)
    }
}

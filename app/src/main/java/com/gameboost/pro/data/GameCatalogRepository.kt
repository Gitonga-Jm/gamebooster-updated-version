package com.gameboost.pro.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledGame(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val lastUsedMs: Long = 0L
)

class GameCatalogRepository(private val context: Context) {

    private val gameKeywords = listOf(
        "game", "play", "pubg", "cod", "fortnite", "minecraft", "roblox",
        "genshin", "valorant", "asphalt", "fifa", "nba", "racing", "shooter",
        "battle", "arena", "legend", "mobile", "clash", "candy", "sonic",
        "mario", "zelda", "pokemon", "chess", "solitaire", "poker"
    )

    suspend fun loadInstalledGames(): List<InstalledGame> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        resolveInfos
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                val appInfo = try {
                    pm.getApplicationInfo(pkg, 0)
                } catch (_: PackageManager.NameNotFoundException) {
                    return@mapNotNull null
                }
                val label = pm.getApplicationLabel(appInfo).toString()
                if (!isLikelyGame(appInfo, label, pkg)) return@mapNotNull null
                InstalledGame(
                    packageName = pkg,
                    label = label,
                    icon = try {
                        pm.getApplicationIcon(pkg)
                    } catch (_: Exception) {
                        null
                    }
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun isLikelyGame(appInfo: ApplicationInfo, label: String, packageName: String): Boolean {
        val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appInfo.category
        } else {
            ApplicationInfo.CATEGORY_UNDEFINED
        }
        if (category == ApplicationInfo.CATEGORY_GAME) return true

        val haystack = "$label $packageName".lowercase()
        return gameKeywords.any { haystack.contains(it) }
    }
}

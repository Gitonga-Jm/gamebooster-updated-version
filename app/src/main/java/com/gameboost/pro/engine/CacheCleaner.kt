package com.gameboost.pro.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class CacheInfo(val packageName: String, val appName: String, val cacheSize: Long)

class CacheCleaner(private val context: Context) {
    
    suspend fun getCacheSizes(): List<CacheInfo> = withContext(Dispatchers.IO) {
        val apps = mutableListOf<CacheInfo>()
        val pm = context.packageManager
        
        try {
            // Scan all app cache directories
            val dataDir = File("/data/data")
            if (dataDir.exists()) {
                dataDir.listFiles()?.forEach { appDir ->
                    if (appDir.isDirectory) {
                        val cacheDir = File(appDir, "cache")
                        if (cacheDir.exists()) {
                            val size = cacheDir.walkTopDown().sumOf { it.length() }
                            if (size > 1024) {
                                val label = try { pm.getApplicationLabel(pm.getApplicationInfo(appDir.name, 0)).toString() } catch (_: Exception) { appDir.name }
                                apps.add(CacheInfo(appDir.name, label, size))
                            }
                        }
                    }
                }
            }
            
            // Also check external cache
            val externalData = File("/storage/emulated/0/Android/data")
            if (externalData.exists()) {
                externalData.listFiles()?.forEach { appDir ->
                    if (appDir.isDirectory) {
                        val cacheDir = File(appDir, "cache")
                        if (cacheDir.exists()) {
                            val size = cacheDir.walkTopDown().sumOf { it.length() }
                            if (size > 1024 && !apps.any { it.packageName == appDir.name }) {
                                apps.add(CacheInfo(appDir.name, appDir.name, size))
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) { }
        
        apps.sortedByDescending { it.cacheSize }
    }
    
    suspend fun clearAllCaches(): Long = withContext(Dispatchers.IO) {
        var totalCleared = 0L
        
        try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            
            for (app in installedApps) {
                // Skip system apps and our app
                if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
                if (app.packageName == context.packageName) continue
                
                try {
                    // Method 1: Clear internal cache
                    val internalCache = File("/data/data/${app.packageName}/cache")
                    if (internalCache.exists()) {
                        internalCache.listFiles()?.forEach { file ->
                            totalCleared += file.length()
                            if (file.isDirectory) file.deleteRecursively() else file.delete()
                        }
                    }
                    
                    // Method 2: Clear external cache
                    val externalCache = File("/storage/emulated/0/Android/data/${app.packageName}/cache")
                    if (externalCache.exists()) {
                        externalCache.listFiles()?.forEach { file ->
                            totalCleared += file.length()
                            if (file.isDirectory) file.deleteRecursively() else file.delete()
                        }
                    }
                    
                    // Method 3: Clear code cache
                    val codeCache = File("/data/data/${app.packageName}/code_cache")
                    if (codeCache.exists()) {
                        codeCache.listFiles()?.forEach { file ->
                            totalCleared += file.length()
                            if (file.isDirectory) file.deleteRecursively() else file.delete()
                        }
                    }
                    
                } catch (_: Exception) { }
            }
            
            // Also clear system temp files
            try {
                val tmpDirs = listOf(
                    File("/data/local/tmp"),
                    File("/storage/emulated/0/Android/obb"),
                    File("/storage/emulated/0/Android/media")
                )
                for (dir in tmpDirs) {
                    if (dir.exists()) {
                        dir.listFiles()?.forEach { file ->
                            if (file.name.contains("cache", ignoreCase = true) || file.name.contains("tmp", ignoreCase = true)) {
                                totalCleared += file.length()
                                try { if (file.isDirectory) file.deleteRecursively() else file.delete() } catch (_: Exception) { }
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
            
        } catch (_: Exception) { }
        
        totalCleared
    }
    
    /**
     * Clear cache for a specific app
     */
    suspend fun clearAppCache(packageName: String): Long = withContext(Dispatchers.IO) {
        var cleared = 0L
        try {
            val dirs = listOf(
                File("/data/data/$packageName/cache"),
                File("/storage/emulated/0/Android/data/$packageName/cache"),
                File("/data/data/$packageName/code_cache")
            )
            for (dir in dirs) {
                if (dir.exists()) {
                    dir.listFiles()?.forEach { file ->
                        cleared += file.length()
                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                    }
                }
            }
        } catch (_: Exception) { }
        cleared
    }
}

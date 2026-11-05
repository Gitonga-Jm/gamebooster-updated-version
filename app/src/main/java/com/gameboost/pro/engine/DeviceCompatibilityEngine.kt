package com.gameboost.pro.engine

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader

data class DeviceInfo(
    val manufacturer: String = Build.MANUFACTURER,
    val model: String = Build.MODEL,
    val brand: String = Build.BRAND,
    val androidVersion: String = Build.VERSION.RELEASE,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val cpuAbi: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
    val totalRamMB: Long = 0,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val gpuRenderer: String = "Unknown",
    val gpuVendor: String = "Unknown",
    val manufacturerOptimizations: List<String> = emptyList(),
    val knownIssues: List<String> = emptyList(),
    val compatibilityScore: Int = 100
)

class DeviceCompatibilityEngine {
    
    private data class MfgConfig(val name: String, val opts: List<String>, val issues: List<String>)
    
    private val configs = mapOf(
        "samsung" to MfgConfig("Samsung", listOf("Game Launcher compatible", "Disable Game Optimization Service"), listOf("Game Optimizing Service may interfere")),
        "xiaomi" to MfgConfig("Xiaomi", listOf("MIUI Game Turbo", "Disable MIUI Optimization"), listOf("MIUI may kill background services")),
        "tecno" to MfgConfig("Tecno", listOf("HiOS - lightweight", "Performance Mode recommended"), listOf("HiOS may restrict background apps")),
        "infinix" to MfgConfig("Infinix", listOf("XOS detected", "Enable Performance Mode"), listOf("XOS battery optimization may limit apps")),
        "oppo" to MfgConfig("Oppo", listOf("ColorOS - Game Space compatible"), listOf("ColorOS may kill background apps")),
        "vivo" to MfgConfig("Vivo", listOf("Funtouch OS - Ultra Game Mode"), listOf("Turn off Smart Power Saving")),
        "huawei" to MfgConfig("Huawei", listOf("Performance Mode in Battery settings"), listOf("No Google Play Services")),
        "oneplus" to MfgConfig("OnePlus", listOf("OxygenOS - Fnatic Mode compatible"), listOf("Battery optimization may pause apps")),
        "realme" to MfgConfig("Realme", listOf("Realme UI - GT Mode available"), listOf("Disable Power Saving")),
        "motorola" to MfgConfig("Motorola", listOf("Clean Android - excellent for gaming"), listOf()),
        "google" to MfgConfig("Pixel", listOf("Pixel - Game Dashboard available"), listOf("Adaptive Battery may restrict apps"))
    )
    
    fun getDeviceInfo(context: Context): DeviceInfo {
        val mfg = Build.MANUFACTURER.lowercase()
        val cfg = configs[mfg] ?: MfgConfig(Build.MANUFACTURER, listOf(), listOf())
        
        val ram = try {
            val mi = android.app.ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
            mi.totalMem / 1048576
        } catch (_: Exception) { 0L }
        
        val dm = context.resources.displayMetrics
        
        val (gpu, vendor) = try {
            val p = Runtime.getRuntime().exec("dumpsys SurfaceFlinger | grep GLES")
            val r = BufferedReader(InputStreamReader(p.inputStream))
            val o = r.readText(); r.close(); p.waitFor()
            Pair(o.lines().firstOrNull { it.contains("GLES:") }?.substringAfter("GLES:")?.trim() ?: "Unknown",
                 o.lines().firstOrNull { it.contains("GL_VENDOR") }?.substringAfter("GL_VENDOR:")?.trim() ?: "Unknown")
        } catch (_: Exception) { Pair("Unknown", "Unknown") }
        
        var score = 100
        if (ram < 3000) score -= 10
        if (ram < 2000) score -= 15
        if (Build.VERSION.SDK_INT < 28) score -= 5
        score -= cfg.issues.size * 2
        
        return DeviceInfo(Build.MANUFACTURER, Build.MODEL, Build.BRAND, Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            ram, dm.widthPixels, dm.heightPixels, gpu, vendor, cfg.opts, cfg.issues, score.coerceIn(0, 100))
    }
}

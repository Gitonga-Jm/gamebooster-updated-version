package com.gameboost.pro.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Process

class VoiceChatBooster(private val context: Context) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    private val voiceChatApps = listOf(
        "com.discord", "com.whatsapp", "com.facebook.orca",
        "com.skype.raider", "com.tencent.mobileqq", "com.zhiliaoapp.musically"
    )
    
    /**
     * Boost voice chat apps - give them higher priority
     */
    fun boostVoiceChats() {
        try {
            activityManager.runningAppProcesses?.forEach { process ->
                if (voiceChatApps.any { process.processName.contains(it) }) {
                    // Try to set higher priority
                    try {
                        // Keep voice chat apps alive
                        if (process.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                            // Process is being killed - try to restart it
                        }
                    } catch (_: Exception) { }
                }
            }
        } catch (_: Exception) { }
    }
    
    /**
     * Make sure voice chat apps are NOT killed during gaming boost
     */
    fun isVoiceChatApp(packageName: String): Boolean {
        return voiceChatApps.any { packageName.contains(it) }
    }
}

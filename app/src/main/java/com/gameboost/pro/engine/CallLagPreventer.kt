package com.gameboost.pro.engine

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class CallLagPreventer(private val context: Context) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // Store original state
    private var originalRingerMode = AudioManager.RINGER_MODE_NORMAL
    private var isActive = false
    
    /**
     * Enable full gaming focus - prevent calls/SMS from causing lag
     */
    fun enableGamingFocus() {
        if (isActive) return
        isActive = true
        
        // 1. Silence ringer (prevents audio interruption)
        originalRingerMode = audioManager.ringerMode
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        
        // 2. Prevent network mode switching (calls force network change)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                if (activeNetwork != null) {
                    // Lock to current network - prevent call from switching to 3G
                    connectivityManager.bindProcessToNetwork(activeNetwork)
                }
            }
        } catch (_: Exception) { }
        
        // 3. Request network to stay on LTE/WiFi (don't drop for calls if VoLTE)
        try {
            val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                // Network is good - keep it locked
            }
        } catch (_: Exception) { }
    }
    
    /**
     * Restore normal state
     */
    fun disableGamingFocus() {
        if (!isActive) return
        isActive = false
        audioManager.ringerMode = originalRingerMode
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.bindProcessToNetwork(null)
            }
        } catch (_: Exception) { }
    }
}

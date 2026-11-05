package com.gameboost.pro.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameDetector(private val context: Context) {
    
    private val _newGameDetected = MutableStateFlow<String?>(null)
    val newGameDetected: StateFlow<String?> = _newGameDetected
    
    private var receiver: BroadcastReceiver? = null
    
    /**
     * Start listening for new app installations
     */
    fun startListening() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val packageName = intent.data?.schemeSpecificPart
                        if (packageName != null && packageName != context.packageName) {
                            _newGameDetected.value = packageName
                        }
                    }
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        val packageName = intent.data?.schemeSpecificPart
                        if (packageName != null) {
                            _newGameDetected.value = packageName
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        
        context.registerReceiver(receiver, filter)
    }
    
    fun stopListening() {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }
    
    fun clearDetection() {
        _newGameDetected.value = null
    }
}

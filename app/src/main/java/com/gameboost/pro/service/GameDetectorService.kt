package com.gameboost.pro.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent

class GameDetectorService : AccessibilityService() {
    
    companion object {
        var currentForegroundPackage: String? = null
        var isRunning = false
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null && packageName != packageName) {
                currentForegroundPackage = packageName
            }
        }
    }
    
    override fun onInterrupt() {}
    
    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }
}

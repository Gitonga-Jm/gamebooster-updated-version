package com.gameboost.pro.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat

class WhatsAppBlocker : Service() {
    private var audioManager: AudioManager? = null
    private var originalMode = AudioManager.RINGER_MODE_NORMAL
    
    companion object {
        var isActive = false; var totalBlocked = 0
        fun start(ctx: Context) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !nm.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                Toast.makeText(ctx, "Please enable DND access", Toast.LENGTH_LONG).show()
                return
            }
            try { ctx.startForegroundService(Intent(ctx, WhatsAppBlocker::class.java)) } catch (_: Exception) { }
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, WhatsAppBlocker::class.java)) }
    }
    
    override fun onBind(i: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        try {
            isActive = true
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            originalMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel("wa_blocker", "Call Blocker", NotificationManager.IMPORTANCE_LOW))
            }
            startForeground(4001, NotificationCompat.Builder(this, "wa_blocker")
                .setContentTitle("📵 Gaming DND Active").setContentText("Calls & notifications blocked")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode).setOngoing(true).build())
            
            try {
                val nm = getSystemService(NotificationManager::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                }
            } catch (_: Exception) { }
        } catch (e: Exception) {
            isActive = false
            stopSelf()
        }
    }
    
    override fun onDestroy() {
        isActive = false
        try { audioManager?.ringerMode = originalMode } catch (_: Exception) { }
        try {
            val nm = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        } catch (_: Exception) { }
        super.onDestroy()
    }
}

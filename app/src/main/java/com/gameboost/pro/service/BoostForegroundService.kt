package com.gameboost.pro.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BoostForegroundService : Service() {
    
    companion object {
        fun start(ctx: Context) {
            val intent = Intent(ctx, BoostForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(intent)
            else ctx.startService(intent)
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, BoostForegroundService::class.java)) }
    }
    
    override fun onBind(i: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("boost_service", "Boost Service", NotificationManager.IMPORTANCE_LOW))
        }
        startForeground(1001, NotificationCompat.Builder(this, "boost_service")
            .setContentTitle("Game Booster").setSmallIcon(android.R.drawable.ic_menu_manage).setOngoing(true).build())
    }
    
    override fun onDestroy() { super.onDestroy() }
}

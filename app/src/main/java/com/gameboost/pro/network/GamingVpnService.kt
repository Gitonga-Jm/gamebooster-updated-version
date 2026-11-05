package com.gameboost.pro.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat

class GamingVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        var isRunning = false
        var currentDns = "1.1.1.1"

        fun start(ctx: Context, dns: String = "1.1.1.1") {
            currentDns = dns
            try {
                val intent = Intent(ctx, GamingVpnService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(intent)
                } else {
                    ctx.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("GameBoost", "VPN start failed", e)
                isRunning = false
            }
        }

        fun stop(ctx: Context) {
            try {
                ctx.startService(Intent(ctx, GamingVpnService::class.java).apply { action = "STOP" })
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "gaming_vpn", "Gaming VPN",
                    NotificationManager.IMPORTANCE_LOW
                )
                getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (intent?.action == "STOP") {
                stopVpn()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            if (!isRunning) startVpn()
        } catch (e: Exception) {
            Log.e("GameBoost", "VPN onStartCommand failed", e)
            stopSelf()
        }
        return START_STICKY
    }

    private fun startVpn() {
        try {
            val builder = Builder()
                .setSession("Game Booster VPN")
                .addAddress("10.8.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(currentDns)
                .addDnsServer("1.0.0.1")
                .setMtu(1500)

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isRunning = true
                startForeground(5001, createNotification())
            } else {
                isRunning = false
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e("GameBoost", "VPN establish failed", e)
            isRunning = false
            stopSelf()
        }
    }

    private fun stopVpn() {
        isRunning = false
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "gaming_vpn")
            .setContentTitle("🎮 Gaming VPN")
            .setContentText("DNS: $currentDns")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}

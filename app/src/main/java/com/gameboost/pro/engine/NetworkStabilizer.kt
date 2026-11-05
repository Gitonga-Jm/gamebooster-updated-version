package com.gameboost.pro.engine

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket

class NetworkStabilizer(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var stabilizerJob: Job? = null
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null
    
    var isActive = false
    var currentPing = 0
    
    fun startStabilizing() {
        if (isActive) return
        isActive = true
        
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            wifiLock = wm?.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "GB:Stabilizer")
            wifiLock?.acquire()
        } catch (_: Exception) { }
        
        stabilizerJob = scope.launch {
            while (isActive) {
                try {
                    try {
                        val start = System.currentTimeMillis()
                        val socket = Socket()
                        socket.connect(InetSocketAddress("1.1.1.1", 53), 1000)
                        currentPing = (System.currentTimeMillis() - start).toInt()
                        socket.close()
                    } catch (_: Exception) { currentPing = -1 }
                    
                    if (currentPing > 300 || currentPing < 0) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val net = connectivityManager.activeNetwork
                                if (net != null) {
                                    connectivityManager.bindProcessToNetwork(null)
                                    delay(50)
                                    connectivityManager.bindProcessToNetwork(net)
                                }
                            }
                        } catch (_: Exception) { }
                    }
                    
                    delay(2000)
                } catch (_: Exception) { }
            }
        }
    }
    
    fun stopStabilizing() {
        isActive = false
        stabilizerJob?.cancel()
        try { wifiLock?.release() } catch (_: Exception) { }
        try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) connectivityManager.bindProcessToNetwork(null) } catch (_: Exception) { }
    }
}

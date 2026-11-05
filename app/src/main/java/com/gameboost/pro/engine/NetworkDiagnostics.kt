package com.gameboost.pro.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket

data class NetworkReport(
    val ping: Int = 0,
    val jitter: Int = 0,
    val packetLoss: Float = 0f,
    val dnsLatency: Int = 0,
    val wifiStrength: Int = 0,
    val wifiQuality: String = "Unknown",
    val networkType: String = "Unknown",
    val stability: String = "Unknown",
    val score: Int = 0,
    val problems: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

class NetworkDiagnostics(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    
    suspend fun runFullDiagnostics(): NetworkReport = withContext(Dispatchers.IO) {
        val problems = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // 1. Ping test (multiple samples)
        val pings = mutableListOf<Int>()
        repeat(5) {
            try {
                val start = System.currentTimeMillis()
                val socket = Socket()
                socket.connect(InetSocketAddress("8.8.8.8", 53), 2000)
                pings.add((System.currentTimeMillis() - start).toInt())
                socket.close()
            } catch (_: Exception) { pings.add(999) }
        }
        val validPings = pings.filter { it < 999 }
        val ping = if (validPings.isNotEmpty()) validPings.average().toInt() else 999
        
        // 2. Jitter (variation between pings)
        val jitter = if (validPings.size >= 2) {
            validPings.zipWithNext().map { Math.abs(it.first - it.second) }.average().toInt()
        } else 0
        
        // 3. Packet loss (failed pings / total)
        val packetLoss = ((pings.size - validPings.size).toFloat() / pings.size) * 100f
        
        // 4. DNS latency
        val dnsLatency = try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress("1.1.1.1", 53), 1500)
            val latency = (System.currentTimeMillis() - start).toInt()
            socket.close()
            latency
        } catch (_: Exception) { 999 }
        
        // 5. WiFi strength
        val wifiInfo: WifiInfo? = wifiManager?.connectionInfo
        val wifiStrength = wifiInfo?.rssi ?: -100
        val wifiLevel = WifiManager.calculateSignalLevel(wifiStrength, 5)
        val wifiQuality = when {
            wifiLevel >= 4 -> "Excellent"
            wifiLevel == 3 -> "Good"
            wifiLevel == 2 -> "Fair"
            wifiLevel == 1 -> "Weak"
            else -> "Poor"
        }
        
        // 6. Network type
        val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val networkType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile Data"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Unknown"
        }
        
        // 7. Stability assessment
        val stability = when {
            jitter > 30 -> "Unstable"
            jitter > 15 -> "Moderate"
            jitter > 5 -> "Good"
            else -> "Excellent"
        }
        
        // 8. Detect problems
        if (ping > 100) {
            problems.add("High latency detected (${ping}ms)")
            recommendations.add("Enable Gaming VPN to reduce ping")
            recommendations.add("Move closer to WiFi router")
        }
        if (packetLoss > 5f) {
            problems.add("Packet loss detected (${String.format("%.1f", packetLoss)}%)")
            recommendations.add("Packet loss causes delayed inputs")
            recommendations.add("Check for WiFi interference")
        }
        if (jitter > 20) {
            problems.add("High jitter detected (${jitter}ms)")
            recommendations.add("Jitter causes inconsistent gameplay")
            recommendations.add("Try switching to 5GHz WiFi")
        }
        if (wifiStrength < -70) {
            problems.add("Weak WiFi signal (${wifiStrength}dBm)")
            recommendations.add("Move closer to your router")
            recommendations.add("Consider a WiFi extender")
        }
        if (dnsLatency > 100) {
            problems.add("Slow DNS response (${dnsLatency}ms)")
            recommendations.add("Enable Gaming VPN for faster DNS")
        }
        if (problems.isEmpty()) {
            problems.add("Network looks good!")
            recommendations.add("Ready for gaming")
        }
        
        // 9. Calculate score (0-100)
        val pingScore = when { ping < 30 -> 25; ping < 60 -> 20; ping < 100 -> 10; else -> 0 }
        val jitterScore = when { jitter < 10 -> 25; jitter < 20 -> 15; jitter < 40 -> 5; else -> 0 }
        val packetScore = when { packetLoss < 1 -> 20; packetLoss < 5 -> 10; else -> 0 }
        val wifiScore = when { wifiLevel >= 4 -> 15; wifiLevel == 3 -> 10; wifiLevel == 2 -> 5; else -> 0 }
        val dnsScore = when { dnsLatency < 30 -> 15; dnsLatency < 80 -> 10; dnsLatency < 150 -> 5; else -> 0 }
        val score = (pingScore + jitterScore + packetScore + wifiScore + dnsScore).coerceIn(0, 100)
        
        NetworkReport(
            ping = ping, jitter = jitter, packetLoss = packetLoss,
            dnsLatency = dnsLatency, wifiStrength = wifiStrength,
            wifiQuality = wifiQuality, networkType = networkType,
            stability = stability, score = score,
            problems = problems, recommendations = recommendations
        )
    }
}

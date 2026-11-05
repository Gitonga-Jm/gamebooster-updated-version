package com.gameboost.pro.network

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket

data class DnsConfig(
    val name: String, val primary: String, val secondary: String,
    val latency: Int = 0, val gamingOptimized: Boolean = false
)

class DnsOptimizer(private val context: Context) {
    
    val availableServers = listOf(
        DnsConfig("Cloudflare", "1.1.1.1", "1.0.0.1", gamingOptimized = true),
        DnsConfig("Cloudflare Gaming", "1.1.1.2", "1.0.0.2", gamingOptimized = true),
        DnsConfig("Google", "8.8.8.8", "8.8.4.4", gamingOptimized = true),
        DnsConfig("Quad9", "9.9.9.9", "149.112.112.112"),
        DnsConfig("OpenDNS", "208.67.222.222", "208.67.220.220")
    )
    
    fun getDnsByName(name: String) = availableServers.find { it.name.lowercase().contains(name.lowercase()) } ?: availableServers.first()
    
    fun getCurrentDns(): List<String> {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val props = cm.getLinkProperties(cm.activeNetwork) ?: return emptyList()
            return props.dnsServers.map { it.hostAddress ?: "" }
        } catch (_: Exception) { return emptyList() }
    }
    
    suspend fun measureDnsLatency(dns: String): Int = withContext(Dispatchers.IO) {
        withTimeoutOrNull(1500L) {
            try {
                val start = System.currentTimeMillis()
                val socket = Socket()
                socket.connect(InetSocketAddress(dns, 53), 1000)
                val latency = (System.currentTimeMillis() - start).toInt()
                socket.close()
                latency
            } catch (_: Exception) { Int.MAX_VALUE }
        } ?: Int.MAX_VALUE
    }
    
    suspend fun findFastestDns(gamingOnly: Boolean = true): DnsConfig = withContext(Dispatchers.IO) {
        val servers = if (gamingOnly) availableServers.filter { it.gamingOptimized } else availableServers
        servers.map { it.copy(latency = measureDnsLatency(it.primary)) }
            .filter { it.latency < Int.MAX_VALUE }
            .minByOrNull { it.latency } ?: availableServers.first()
    }
    
    /**
     * Quick DNS test - only tests top 2, returns in under 2 seconds
     */
    suspend fun quickDnsTest(): DnsConfig = withContext(Dispatchers.IO) {
        val top2 = listOf(availableServers[0], availableServers[2]) // Cloudflare + Google
        top2.map { it.copy(latency = measureDnsLatency(it.primary)) }
            .filter { it.latency < Int.MAX_VALUE }
            .minByOrNull { it.latency } ?: availableServers.first()
    }
}

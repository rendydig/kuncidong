package com.rendy.customlockscreen.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Fallback device discovery: probes every host in the local /24 subnet on the
 * remote control port and keeps the ones that answer a `hello`.
 */
class SubnetScanner(
    private val concurrency: Int = 32,
    private val timeoutMs: Int = SCAN_TIMEOUT_MS
) {

    private val client = RemoteControlClient()

    suspend fun scan(port: Int = DEFAULT_PORT, onFound: (KidEndpoint) -> Unit): List<KidEndpoint> =
        coroutineScope {
            val localAddresses = localIpv4Addresses()
            if (localAddresses.isEmpty()) return@coroutineScope emptyList()

            val semaphore = Semaphore(concurrency)
            val found = mutableListOf<KidEndpoint>()
            val resultLock = Any()

            val jobs = localAddresses.flatMap { address ->
                val prefix = address.substringBeforeLast('.')
                (1..254).filter { host -> "$prefix.$host" != address }.map { host ->
                    val candidate = "$prefix.$host"
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val response = client.exchange(
                                candidate,
                                port,
                                RemoteRequest(cmd = Cmd.HELLO),
                                timeoutMs
                            )
                            if (response != null && response.ok) {
                                val endpoint = KidEndpoint(
                                    host = candidate,
                                    port = port,
                                    deviceName = response.device,
                                    paired = response.paired == true,
                                    needsPair = response.needsPair == true,
                                    source = "scan"
                                )
                                synchronized(resultLock) { found.add(endpoint) }
                                onFound(endpoint)
                            }
                        }
                    }
                }
            }
            jobs.awaitAll()
            found.distinctBy { it.host }
        }

    /** Local IPv4 addresses with a /24 prefix, excluding loopback and down links. */
    private suspend fun localIpv4Addresses(): List<String> = withContext(Dispatchers.IO) {
        val addresses = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@withContext emptyList()
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                for (address in networkInterface.inetAddresses) {
                    if (address is Inet4Address && !address.isLoopbackAddress && address.isSiteLocalAddress) {
                        addresses.add(address.hostAddress ?: continue)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enumerate interfaces: ${e.message}")
        }
        addresses.distinct()
    }

    companion object {
        private const val TAG = "SubnetScanner"
        const val SCAN_TIMEOUT_MS = 250
    }
}

package com.rendy.customlockscreen.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.util.ArrayDeque

/** An addressable Kids device reachable on the local network. */
data class KidEndpoint(
    val host: String,
    val port: Int,
    val deviceName: String?,
    val paired: Boolean,
    val needsPair: Boolean,
    val source: String = "nsd"
)

/**
 * Discovers `_kuncidong._tcp` services and resolves each one to a host/port.
 *
 * Resolution is serialized through a queue because Android's NSD manager only
 * resolves one service at a time on many devices.
 */
class NsdDiscovery(context: Context, private val listener: Listener) {

    interface Listener {
        fun onKidFound(endpoint: KidEndpoint)
        fun onDiscoveryFailed(reason: String)
    }

    private val nsdManager =
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    @Volatile
    var isDiscovering: Boolean = false
        private set

    fun start() {
        if (isDiscovering) return
        resolveQueue.clear()
        resolving = false

        val discovery = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "startDiscovery failed: $errorCode")
                listener.onDiscoveryFailed("start_failed_$errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "stopDiscovery failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                isDiscovering = true
                Log.d(TAG, "Discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isDiscovering = false
                Log.d(TAG, "Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceType.startsWith(SERVICE_TYPE)) return
                enqueueResolve(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
            }
        }

        discoveryListener = discovery
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discovery)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery: ${e.message}")
            listener.onDiscoveryFailed(e.message ?: "discovery_error")
        }
    }

    fun stop() {
        isDiscovering = false
        resolveQueue.clear()
        val discovery = discoveryListener ?: return
        discoveryListener = null
        try {
            nsdManager.stopServiceDiscovery(discovery)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop discovery: ${e.message}")
        }
    }

    private fun enqueueResolve(serviceInfo: NsdServiceInfo) {
        resolveQueue.add(serviceInfo)
        maybeResolveNext()
    }

    @Synchronized
    private fun maybeResolveNext() {
        if (resolving) return
        val next = resolveQueue.poll() ?: return
        resolving = true

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${info.serviceName}: $errorCode")
                synchronized(this@NsdDiscovery) {
                    resolving = false
                }
                maybeResolveNext()
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                val host = info.host?.hostAddress
                if (host != null) {
                    listener.onKidFound(
                        KidEndpoint(
                            host = host,
                            port = info.port,
                            deviceName = info.serviceName,
                            paired = false,
                            needsPair = false,
                            source = "nsd"
                        )
                    )
                }
                synchronized(this@NsdDiscovery) {
                    resolving = false
                }
                maybeResolveNext()
            }
        }

        try {
            nsdManager.resolveService(next, resolveListener)
        } catch (e: Exception) {
            Log.w(TAG, "resolveService threw: ${e.message}")
            synchronized(this) {
                resolving = false
            }
            maybeResolveNext()
        }
    }

    companion object {
        private const val TAG = "NsdDiscovery"
    }
}

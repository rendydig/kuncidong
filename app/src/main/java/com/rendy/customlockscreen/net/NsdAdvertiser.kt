package com.rendy.customlockscreen.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * Advertises this Kids device as `_kuncidong._tcp` so Parent devices can find it
 * almost instantly without scanning the subnet.
 */
class NsdAdvertiser(context: Context, private val port: Int = DEFAULT_PORT) {

    private val nsdManager =
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var registrationListener: NsdManager.RegistrationListener? = null

    @Volatile
    var serviceName: String? = null
        private set

    fun start(deviceName: String) {
        stop()
        val shortModel = deviceName.removePrefix("Anak-").take(20)
        val info = NsdServiceInfo().apply {
            this.serviceName = "kuncidong-$shortModel-${RemoteAuthStore.randomId().take(4)}"
            this.serviceType = SERVICE_TYPE
            setPort(port)
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                serviceName = serviceInfo.serviceName
                Log.d(TAG, "Advertising as ${serviceInfo.serviceName}")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD registration failed: $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d(TAG, "NSD unregistered")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD unregistration failed: $errorCode")
            }
        }

        registrationListener = listener
        try {
            nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register NSD service: ${e.message}")
        }
    }

    fun stop() {
        val listener = registrationListener ?: return
        registrationListener = null
        serviceName = null
        try {
            nsdManager.unregisterService(listener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister NSD service: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "NsdAdvertiser"
    }
}

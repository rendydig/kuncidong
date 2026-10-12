package com.rendy.customlockscreen.net

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** A Kids device this Parent has paired with, including its auth token. */
data class KidDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val token: String,
    val lastSeen: Long = System.currentTimeMillis()
) {
    val address: String get() = "$host:$port"
}

/** Persists the Parent's paired Kids devices (name, address and token). */
class ParentDeviceStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun list(): List<KidDevice> {
        val json = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<KidDevice>>() {}.type
            gson.fromJson<List<KidDevice>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun find(id: String): KidDevice? = list().firstOrNull { it.id == id }

    /** Inserts or replaces a device, matching on host:port and on name. */
    fun upsert(device: KidDevice) {
        val devices = list().toMutableList()
        devices.removeAll { it.address == device.address || it.name == device.name }
        devices.add(device)
        save(devices)
    }

    fun touch(id: String) {
        val devices = list().map {
            if (it.id == id) it.copy(lastSeen = System.currentTimeMillis()) else it
        }
        save(devices)
    }

    fun remove(id: String) {
        save(list().filterNot { it.id == id })
    }

    fun clear() = save(emptyList())

    private fun save(devices: List<KidDevice>) {
        prefs.edit().putString(KEY_DEVICES, gson.toJson(devices)).apply()
    }

    companion object {
        private const val PREFS_NAME = "kuncidong_parent_devices"
        private const val KEY_DEVICES = "paired_kids"
        private val gson = Gson()
    }
}

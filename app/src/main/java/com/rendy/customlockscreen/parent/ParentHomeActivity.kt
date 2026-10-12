package com.rendy.customlockscreen.parent

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.rendy.customlockscreen.R
import com.rendy.customlockscreen.RoleSelectionActivity
import com.rendy.customlockscreen.SmartLockApplication
import com.rendy.customlockscreen.data.MessageTemplate
import com.rendy.customlockscreen.data.QuestionDatabase
import com.rendy.customlockscreen.lock.AppLockGate
import com.rendy.customlockscreen.lock.AppPinDialog
import com.rendy.customlockscreen.net.KidDevice
import com.rendy.customlockscreen.net.KidEndpoint
import com.rendy.customlockscreen.net.LockMode
import com.rendy.customlockscreen.net.NsdDiscovery
import com.rendy.customlockscreen.net.ParentDeviceStore
import com.rendy.customlockscreen.net.RemoteAuthStore
import com.rendy.customlockscreen.net.RemoteControlClient
import com.rendy.customlockscreen.net.SubnetScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Parent device home: discovers Kids devices (NSD + subnet scan), pairs with
 * them using the 6 digit code, and sends lock/unlock commands.
 */
class ParentHomeActivity : AppCompatActivity() {

    private lateinit var deviceStore: ParentDeviceStore
    private lateinit var client: RemoteControlClient
    private lateinit var adapter: KidDeviceAdapter
    private lateinit var tvScanStatus: TextView
    private lateinit var tvEmpty: TextView

    private var discovery: NsdDiscovery? = null
    private var scanJob: Job? = null

    /** Endpoints found during the current scan, keyed by `host:port`. */
    private val discovered = LinkedHashMap<String, KidEndpoint>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppLockGate.requireUnlock(this, ParentHomeActivity::class.java)) return
        setContentView(R.layout.activity_parent_home)

        deviceStore = ParentDeviceStore(this)
        client = RemoteControlClient()

        tvScanStatus = findViewById(R.id.tvScanStatus)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = KidDeviceAdapter(
            onItemClick = { device -> onDeviceTapped(device) },
            onLock = { device -> lockDevices(listOf(device)) },
            onUnlock = { device -> unlockDevice(device) },
            onRemove = { device -> confirmRemove(device) },
            onSelectionChanged = { updateSelectionButton() }
        )

        findViewById<RecyclerView>(R.id.rvKids).apply {
            layoutManager = LinearLayoutManager(this@ParentHomeActivity)
            adapter = this@ParentHomeActivity.adapter
        }

        findViewById<MaterialButton>(R.id.btnScan).setOnClickListener { startScan() }
        findViewById<MaterialButton>(R.id.btnTemplates).setOnClickListener {
            startActivity(Intent(this, MessageTemplateActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnParentSettings).setOnClickListener { showSettings() }
        findViewById<MaterialButton>(R.id.btnLockSelected).setOnClickListener {
            val selected = adapter.selectedDevices()
            if (selected.isEmpty()) {
                Toast.makeText(this, "Pilih HP anak dulu", Toast.LENGTH_SHORT).show()
            } else {
                lockDevices(selected)
            }
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (AppLockGate.requireUnlock(this, ParentHomeActivity::class.java)) return
    }

    override fun onDestroy() {
        super.onDestroy()
        discovery?.stop()
        scanJob?.cancel()
    }

    // region discovery

    private fun startScan() {
        discovery?.stop()
        discovered.clear()
        refresh()
        tvScanStatus.text = "Mencari HP anak…"

        val nsd = NsdDiscovery(this, object : NsdDiscovery.Listener {
            override fun onKidFound(endpoint: KidEndpoint) {
                runOnUiThread { addEndpoint(endpoint) }
            }

            override fun onDiscoveryFailed(reason: String) {
                runOnUiThread { tvScanStatus.text = "NSD gagal ($reason), memakai scan IP…" }
            }
        })
        discovery = nsd
        nsd.start()

        scanJob?.cancel()
        scanJob = lifecycleScope.launch {
            val scanner = SubnetScanner()
            scanner.scan { endpoint ->
                runOnUiThread { addEndpoint(endpoint) }
            }
            refresh()
            tvScanStatus.text =
                "Selesai. ${adapter.itemCount} HP anak ditemukan. NSD tetap aktif."
        }
    }

    private fun addEndpoint(endpoint: KidEndpoint) {
        // If a paired device moved, update its address instead of duplicating it.
        val paired = deviceStore.list()
        val knownByName = paired.firstOrNull { it.name == endpoint.deviceName }
        if (knownByName != null && knownByName.address != "${endpoint.host}:${endpoint.port}") {
            deviceStore.upsert(knownByName.copy(host = endpoint.host, port = endpoint.port))
        }
        discovered["${endpoint.host}:${endpoint.port}"] = endpoint
        refresh()
    }

    private fun refresh() {
        val items = currentItems()
        adapter.submit(items)
        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        updateSelectionButton()
    }

    private fun currentItems(): List<KidDevice> {
        val items = LinkedHashMap<String, KidDevice>()
        deviceStore.list().forEach { items[it.id] = it }
        discovered.values.forEach { endpoint ->
            val address = "${endpoint.host}:${endpoint.port}"
            if (items.values.none { it.address == address }) {
                items["scan:$address"] = KidDevice(
                    id = "scan:$address",
                    name = endpoint.deviceName ?: endpoint.host,
                    host = endpoint.host,
                    port = endpoint.port,
                    token = ""
                )
            }
        }
        return items.values.toList()
    }

    // endregion

    // region row actions

    private fun onDeviceTapped(device: KidDevice) {
        if (device.token.isBlank()) {
            showPairDialog(device)
        } else {
            lifecycleScope.launch {
                val response = client.status(device.host, device.port, device.token)
                val message = when {
                    response == null -> "${device.name} tidak bisa dihubungi"
                    response.ok -> {
                        val state = if (response.locked == true) "terkunci" else "terbuka"
                        "${device.name}: $state${response.battery?.let { ", baterai $it%" } ?: ""}"
                    }
                    else -> "${device.name}: ${response.err}"
                }
                Toast.makeText(this@ParentHomeActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmRemove(device: KidDevice) {
        AlertDialog.Builder(this)
            .setTitle("Lepas ${device.name}?")
            .setMessage("HP ini tidak akan menerima perintah dari Anda lagi.")
            .setPositiveButton("Lepas") { _, _ ->
                lifecycleScope.launch {
                    client.unpair(device.host, device.port, device.token)
                    deviceStore.remove(device.id)
                    discovered.remove("${device.host}:${device.port}")
                    refresh()
                    Toast.makeText(this@ParentHomeActivity, "Dilepas", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // endregion

    // region pairing

    private fun showPairDialog(device: KidDevice) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_pair_kid, null)
        val tvDevice = view.findViewById<TextView>(R.id.tvPairDevice)
        val etCode = view.findViewById<EditText>(R.id.etPairCode)
        val etPin = view.findViewById<EditText>(R.id.etPairPin)
        val etName = view.findViewById<EditText>(R.id.etParentName)

        tvDevice.text = "${device.name} (${device.address})"
        etName.setText("Ayah")

        val dialog = AlertDialog.Builder(this)
            .setTitle("Pairing HP Anak")
            .setView(view)
            .setPositiveButton("Pasangkan", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val code = etCode.text?.toString()?.trim().orEmpty()
                val pin = etPin.text?.toString()?.trim().orEmpty()
                val name = etName.text?.toString()?.trim().orEmpty().ifEmpty { "Orang Tua" }

                if (code.length != 6) {
                    Toast.makeText(this, "Kode harus 6 digit", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (pin.length < 4) {
                    Toast.makeText(this, "PIN minimal 4 angka", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                dialog.dismiss()
                pairDevice(device, code, pin, name)
            }
        }

        dialog.show()
    }

    private fun pairDevice(device: KidDevice, code: String, pin: String, parentName: String) {
        lifecycleScope.launch {
            val response = client.pair(device.host, device.port, code, pin, parentName)
            val token = response?.token
            if (response != null && response.ok && !token.isNullOrBlank()) {
                val stored = KidDevice(
                    id = RemoteAuthStore.randomId(),
                    name = response.device ?: device.name,
                    host = device.host,
                    port = device.port,
                    token = token
                )
                deviceStore.upsert(stored)
                discovered.remove("${device.host}:${device.port}")
                refresh()
                Toast.makeText(this@ParentHomeActivity, "✅ ${stored.name} terpasang", Toast.LENGTH_LONG).show()
            } else {
                val reason = response?.err ?: "tidak ada respons"
                Toast.makeText(
                    this@ParentHomeActivity,
                    "Gagal pairing: $reason. Pastikan layar kode di HP anak terbuka.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // endregion

    // region lock / unlock

    private fun lockDevices(devices: List<KidDevice>) {
        val targets = devices.filter { it.token.isNotBlank() }
        if (targets.isEmpty()) {
            Toast.makeText(this, "Pilih HP anak yang sudah dipasangkan", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val templates = QuestionDatabase.getDatabase(this@ParentHomeActivity).remoteDao().getAllTemplates()
            val label = if (targets.size == 1) targets.first().name else "${targets.size} HP anak"
            LockCommandDialog.show(this@ParentHomeActivity, label, templates) { command ->
                sendLock(targets, command)
            }
        }
    }

    private fun sendLock(targets: List<KidDevice>, command: LockCommand) {
        lifecycleScope.launch {
            var success = 0
            for (device in targets) {
                val response = client.lock(
                    host = device.host,
                    port = device.port,
                    token = device.token,
                    mode = command.mode,
                    title = command.title,
                    text = command.text,
                    pack = command.pack
                )
                if (response?.ok == true) success++
            }
            val label = if (command.mode == LockMode.TEXT) "pesan" else "kuis"
            Toast.makeText(
                this@ParentHomeActivity,
                "🔒 Terkunci dengan $label di $success/${targets.size} HP",
                Toast.LENGTH_LONG
            ).show()
            adapter.clearSelection()
        }
    }

    private fun unlockDevice(device: KidDevice) {
        lifecycleScope.launch {
            val response = client.unlock(device.host, device.port, device.token)
            val message = if (response?.ok == true) "🔓 ${device.name} dibuka" else "Gagal membuka ${device.name}"
            Toast.makeText(this@ParentHomeActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectionButton() {
        val count = adapter.selectedCount()
        val button = findViewById<MaterialButton>(R.id.btnLockSelected)
        button.visibility = if (count > 0) View.VISIBLE else View.GONE
        button.text = if (count > 1) "🔒 Kunci $count HP Terpilih" else "🔒 Kunci Terpilih"
    }

    // endregion

    private fun showSettings() {
        AlertDialog.Builder(this)
            .setTitle("Pengaturan Orang Tua")
            .setItems(
                arrayOf("PIN Aplikasi", "Ganti peran HP ini", "Hapus semua HP anak tersimpan")
            ) { _, which ->
                when (which) {
                    0 -> AppPinDialog.show(this)
                    1 -> {
                        SmartLockApplication.instance.setRole(SmartLockApplication.ROLE_UNSET)
                        startActivity(Intent(this, RoleSelectionActivity::class.java))
                        finish()
                    }
                    2 -> {
                        deviceStore.clear()
                        discovered.clear()
                        refresh()
                        Toast.makeText(this, "Daftar HP anak dihapus", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
}

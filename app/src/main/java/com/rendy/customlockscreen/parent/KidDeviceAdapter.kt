package com.rendy.customlockscreen.parent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.rendy.customlockscreen.R
import com.rendy.customlockscreen.net.KidDevice

/**
 * Lists Kids devices (paired and discovered-but-unpaired) with per-row lock,
 * unlock and remove actions, plus a checkbox used for multi-device locking.
 */
class KidDeviceAdapter(
    private val onItemClick: (KidDevice) -> Unit,
    private val onLock: (KidDevice) -> Unit,
    private val onUnlock: (KidDevice) -> Unit,
    private val onRemove: (KidDevice) -> Unit,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<KidDeviceAdapter.ViewHolder>() {

    private val devices = mutableListOf<KidDevice>()
    private val selectedIds = mutableSetOf<String>()

    fun submit(newDevices: List<KidDevice>) {
        devices.clear()
        devices.addAll(newDevices)
        selectedIds.retainAll(devices.map { it.id }.toSet())
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun selectedDevices(): List<KidDevice> = devices.filter { selectedIds.contains(it.id) }

    fun selectedCount(): Int = devices.count { selectedIds.contains(it.id) }

    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kid_device, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = devices.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
        private val tvName: TextView = itemView.findViewById(R.id.tvKidName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvKidAddress)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvKidStatus)
        private val btnLock: MaterialButton = itemView.findViewById(R.id.btnLock)
        private val btnUnlock: MaterialButton = itemView.findViewById(R.id.btnUnlock)
        private val btnRemove: TextView = itemView.findViewById(R.id.btnRemove)

        fun bind(device: KidDevice) {
            val isPaired = device.token.isNotBlank()
            val context = itemView.context

            tvName.text = device.name.ifBlank { device.host }
            tvAddress.text = device.address
            tvStatus.text = if (isPaired) {
                context.getString(R.string.kid_device_status_paired)
            } else {
                context.getString(R.string.kid_device_status_unpaired)
            }
            tvStatus.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isPaired) R.color.success else R.color.warning_orange
                )
            )

            btnLock.visibility = if (isPaired) View.VISIBLE else View.GONE
            btnUnlock.visibility = if (isPaired) View.VISIBLE else View.GONE
            btnRemove.visibility = if (isPaired) View.VISIBLE else View.GONE

            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = selectedIds.contains(device.id)
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedIds.add(device.id) else selectedIds.remove(device.id)
                onSelectionChanged()
            }

            itemView.setOnClickListener { onItemClick(device) }
            btnLock.setOnClickListener { onLock(device) }
            btnUnlock.setOnClickListener { onUnlock(device) }
            btnRemove.setOnClickListener { onRemove(device) }
        }
    }
}

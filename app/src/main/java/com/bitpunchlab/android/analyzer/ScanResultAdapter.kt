package com.bitpunchlab.android.analyzer

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitpunchlab.android.analyzer.databinding.ItemWifiDeviceBinding

class ScanResultAdapter : ListAdapter<ScanResult, ScanResultViewHolder>(ScanResultDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanResultViewHolder {
        return ScanResultViewHolder(
            ItemWifiDeviceBinding
            .inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ScanResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ScanResultViewHolder(val binding: ItemWifiDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: ScanResult) {
        binding.device = item
        binding.executePendingBindings()
    }
}

class ScanResultDiffCallback : DiffUtil.ItemCallback<ScanResult>() {
    override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
        return oldItem.SSID == newItem.SSID
    }

    override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
        return oldItem.frequency == newItem.frequency
    }

}
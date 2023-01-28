package com.bitpunchlab.android.analyzer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitpunchlab.android.analyzer.Process
import com.bitpunchlab.android.analyzer.databinding.ItemProcessBinding

class ProcessAdapter : ListAdapter<Process, ProcessViewHolder>(ProcessDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessViewHolder {
        return ProcessViewHolder(ItemProcessBinding
            .inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ProcessViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ProcessViewHolder(val binding: ItemProcessBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: Process) {
        binding.process = item
        binding.executePendingBindings()
    }
}

class ProcessDiffCallback : DiffUtil.ItemCallback<Process>() {
    override fun areItemsTheSame(oldItem: Process, newItem: Process): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Process, newItem: Process): Boolean {
        return oldItem.name == newItem.name
    }

}


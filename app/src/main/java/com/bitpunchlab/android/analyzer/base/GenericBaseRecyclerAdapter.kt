package com.bitpunchlab.android.analyzer.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class GenericBaseRecyclerAdapter<T : Any>(
    compareItems: (old: T, new: T) -> Boolean,
    compareIContents: (old: T, new: T) -> Boolean,
    private val clickListener: GenericClickListener<T>?,
    private val bindingInterface: GenericRecyclerBindingInterface<T>

) : ListAdapter<T, GenericViewHolder>(GenericDiffCallback(compareItems, compareIContents)){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        val binding = DataBindingUtil.inflate<ViewDataBinding>(
            layoutInflater, getLayoutRes(viewType), parent, false)

        binding.lifecycleOwner = getLifecycleOwner()

        return GenericViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GenericViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, clickListener, bindingInterface)
    }

    @LayoutRes
    abstract fun getLayoutRes(viewType: Int) : Int

    open fun getLifecycleOwner() : LifecycleOwner? {
        return null
    }
}

class GenericViewHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
    fun<T: Any>bind(item: T, clickListener: GenericClickListener<T>?,
        bindingInterface: GenericRecyclerBindingInterface<T>) {
        bindingInterface.bindData(item, binding, clickListener)
    }

}

interface GenericRecyclerBindingInterface<T: Any> {
    fun bindData(item: T, binding: ViewDataBinding, onClickListener: GenericClickListener<T>?) {
    }
}

open abstract class GenericClickListener<T>(open val clickListener: (T) -> Unit) {

}

class GenericDiffCallback<T>(
    private val compareItems: (old: T, new: T) -> Boolean,
    private val compareContents: (old: T, new: T) -> Boolean) : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return compareItems(oldItem, newItem)
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return compareContents(oldItem, newItem)
    }

}
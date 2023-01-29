package com.bitpunchlab.android.analyzer.devices

import android.net.wifi.ScanResult
import androidx.databinding.ViewDataBinding
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.base.GenericBaseRecyclerAdapter
import com.bitpunchlab.android.analyzer.base.GenericClickListener
import com.bitpunchlab.android.analyzer.base.GenericRecyclerBindingInterface
import com.bitpunchlab.android.analyzer.databinding.ItemWifiDeviceBinding

class WifiDeviceAdapter(val clickListener: WifiOnClickListener) : GenericBaseRecyclerAdapter<ScanResult>(
    clickListener = clickListener,
    compareItems = { old, new -> old.BSSID == new.BSSID },
    compareIContents = { old, new -> old.frequency == new.frequency },
    bindingInterface = object : GenericRecyclerBindingInterface<ScanResult> {
        override fun bindData(
            item: ScanResult,
            binding: ViewDataBinding,
            onClickListener: GenericClickListener<ScanResult>?
        ) {
            (binding as ItemWifiDeviceBinding).device = item
            binding
        }
    }
) {
    override fun getLayoutRes(viewType: Int): Int {
        return R.layout.item_wifi_device
    }
}

class WifiOnClickListener(override val clickListener: (ScanResult) -> Unit) :
    GenericClickListener<ScanResult>(clickListener) {
    fun onClick(result: ScanResult) = clickListener(result)
}
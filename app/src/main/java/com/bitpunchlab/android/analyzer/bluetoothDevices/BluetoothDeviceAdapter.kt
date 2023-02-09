package com.bitpunchlab.android.analyzer.devices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.databinding.ViewDataBinding
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.base.GenericBaseRecyclerAdapter
import com.bitpunchlab.android.analyzer.base.GenericClickListener
import com.bitpunchlab.android.analyzer.base.GenericRecyclerBindingInterface
import com.bitpunchlab.android.analyzer.databinding.ItemBluetoothDeviceBinding
import com.bitpunchlab.android.analyzer.models.BluetoothDeviceDetail

@SuppressLint("MissingPermission")
class BluetoothDeviceAdapter(val clickListener: BluetoothClickListener) : GenericBaseRecyclerAdapter<BluetoothDeviceDetail>(
    clickListener = clickListener,
    compareItems = { old, new -> old.device.uuids == new.device.uuids },
    compareIContents = { old, new -> old.device.address == new.device.address },
    bindingInterface = object : GenericRecyclerBindingInterface<BluetoothDeviceDetail> {
        override fun bindData(
            item: BluetoothDeviceDetail,
            binding: ViewDataBinding,
            onClickListener: GenericClickListener<BluetoothDeviceDetail>?
        ) {
            (binding as ItemBluetoothDeviceBinding).bluetooth = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }
    }
) {
    override fun getLayoutRes(viewType: Int): Int {
        return R.layout.item_bluetooth_device
    }
}

class BluetoothClickListener(override val clickListener: (BluetoothDeviceDetail) -> Unit) :
    GenericClickListener<BluetoothDeviceDetail>(clickListener) {

        fun onClick(device: BluetoothDeviceDetail) = clickListener(device)
}

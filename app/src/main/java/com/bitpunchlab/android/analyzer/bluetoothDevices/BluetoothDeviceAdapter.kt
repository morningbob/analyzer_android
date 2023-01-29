package com.bitpunchlab.android.analyzer.devices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.databinding.ViewDataBinding
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.base.GenericBaseRecyclerAdapter
import com.bitpunchlab.android.analyzer.base.GenericClickListener
import com.bitpunchlab.android.analyzer.base.GenericRecyclerBindingInterface
import com.bitpunchlab.android.analyzer.databinding.ItemBluetoothDeviceBinding

@SuppressLint("MissingPermission")
class BluetoothDeviceAdapter(val clickListener: BluetoothClickListener) : GenericBaseRecyclerAdapter<BluetoothDevice>(
    clickListener = clickListener,
    compareItems = { old, new -> old.uuids == new.uuids },
    compareIContents = { old, new -> old.address == new.address },
    bindingInterface = object : GenericRecyclerBindingInterface<BluetoothDevice> {
        override fun bindData(
            item: BluetoothDevice,
            binding: ViewDataBinding,
            onClickListener: GenericClickListener<BluetoothDevice>?
        ) {
            (binding as ItemBluetoothDeviceBinding).device = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }
    }
) {
    override fun getLayoutRes(viewType: Int): Int {
        return R.layout.item_bluetooth_device
    }
}

class BluetoothClickListener(override val clickListener: (BluetoothDevice) -> Unit) :
    GenericClickListener<BluetoothDevice>(clickListener) {

        fun onClick(device: BluetoothDevice) = clickListener(device)
}

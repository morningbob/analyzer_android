package com.bitpunchlab.android.analyzer.devices

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BluetoothDeviceViewModel(application: Application) : AndroidViewModel(application) {

    private var bluetoothManager : BluetoothManager
    private var bluetoothAdapter : BluetoothAdapter
    var bluetoothDevices = MutableLiveData<ArrayList<BluetoothDevice>>()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action
            if (action != null) {
                when (action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        // Discovery has found a device. Get the BluetoothDevice
                        // object and its info from the Intent.
                        val device: BluetoothDevice? =
                            intent!!.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            Log.i("bluetooth vm", "found a device ${it.address}")
                            //val deviceName = it.name
                            //val deviceHardwareAddress = device.address // MAC address
                            addBluetoothDevice(device)
                        }

                    }
                }
            }
        }
    }

    init {
        bluetoothManager = getApplication<Application>().getSystemService<BluetoothManager>() as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter != null) {
            Log.i("bluetooth vm", "got adapter")
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        getApplication<Application>().applicationContext.registerReceiver(bluetoothReceiver, intentFilter)
    }

    fun addBluetoothDevice(device: BluetoothDevice) {
        var currentList = ArrayList<BluetoothDevice>()
        var isExistDevice = false
        bluetoothDevices.value.let {
            if (!it.isNullOrEmpty()) {
                currentList = it.toMutableList() as ArrayList
                currentList.map { oldDevice ->
                    if (oldDevice.address == device.address) {
                        // same device, don't need to add
                        isExistDevice = true
                    }
                }
            }
        }
        if (!isExistDevice) {
            currentList.add(device)
            bluetoothDevices.value = currentList
        }
    }

    @SuppressLint("MissingPermission")
    fun scanBluetoothDevices() {
        bluetoothAdapter.startDiscovery()
    }



}

class BluetoothDeviceViewModelFactory(private val application: Application)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothDeviceViewModel::class.java)) {
            return BluetoothDeviceViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.bitpunchlab.android.analyzer.devices

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.analyzer.models.BluetoothDeviceDetail
import java.util.*
import kotlin.collections.ArrayList

class BluetoothDeviceViewModel(application: Application) : AndroidViewModel(application) {

    private var bluetoothManager : BluetoothManager
    private var bluetoothAdapter : BluetoothAdapter
    private var bluetoothLeScanner : BluetoothLeScanner
    private val SCAN_PERIOD: Long = 100000
    var bluetoothDevices = MutableLiveData<ArrayList<BluetoothDevice>>()
    var bluetoothList = MutableLiveData<ArrayList<BluetoothDeviceDetail>>()
    var rssi = MutableLiveData<ArrayList<Int>>()

    var _chosenDevice = MutableLiveData<BluetoothDeviceDetail?>()
    val chosenDevice get() = _chosenDevice

    var isScanningBluetooth = MutableLiveData<Boolean>(false)
    var isScanningBLE = MutableLiveData<Boolean>(false)

    // for bluetooth devices (not BLE devices)
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action
            if (action != null) {
                when (action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        // Discovery has found a device. Get the BluetoothDevice
                        // object and its info from the Intent.
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        val theRssi = (intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)).toInt()
                        device?.let {
                            Log.i("bluetooth vm", "found a device ${it.address}")
                            //val deviceName = it.name
                            //val deviceHardwareAddress = device.address // MAC address
                            addBluetoothDevice(device, theRssi)
                        }

                    }
                    //BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    //    Log.i("bluetooth receiver", "discovery finished detected")
                    //}
                }
            }
        }
    }

    init {
        // for bluetooth devices (not BLE devices)
        bluetoothManager = getApplication<Application>().getSystemService<BluetoothManager>() as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter != null) {
            Log.i("bluetooth vm", "got adapter")
        }
        // for bluetooth devices (not BLE devices)
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        getApplication<Application>().applicationContext.registerReceiver(bluetoothReceiver, intentFilter)

        // get le scanner
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        bluetoothLeScanner?.let {
            Log.i("bluetooth vm init", "got le scanner")
        }
    }

    fun addBluetoothDevice(device: BluetoothDevice, rssiNum: Int) {
        var currentList = ArrayList<BluetoothDeviceDetail>()
        var isExistDevice = false
        //var rssiList = ArrayList<Int>()

        bluetoothList.value.let {
            if (!it.isNullOrEmpty()) {
                currentList = it.toMutableList() as ArrayList
                //rssiList = rssi.value!!.toMutableList() as ArrayList
                currentList.map { oldDevice ->
                    if (oldDevice.device.address == device.address) {
                        // same device, don't need to add
                        isExistDevice = true
                    }
                }
            }
        }
        if (!isExistDevice) {
            // create BluetoothDeviceDetail and put in device and rssi
            val bluetoothDevice = BluetoothDeviceDetail(device = device, rssi = rssiNum)
            currentList.add(bluetoothDevice)
            //rssiList.add(rssiNum)
            bluetoothList.value = currentList
            //rssi.value = rssiList
        }
    }


    // for bluetooth devices (not BLE devices)
    // I'll stop scanning, after 1.30 min.
    @SuppressLint("MissingPermission")
    fun scanBluetoothDevices() {
        bluetoothAdapter.startDiscovery()
        val task = object : TimerTask() {
            override fun run() {
                bluetoothAdapter.cancelDiscovery()
                isScanningBluetooth.postValue(false)
            }
        }

        Timer().schedule(task, 90000)
        isScanningBluetooth.value = true
    }

    // for BLE devices (not Bluetooth devices)
    @SuppressLint("MissingPermission")
    fun scanBLEDevices() {
        if (isScanningBluetooth.value == false) {
            bluetoothLeScanner.startScan(scanCallback)
            isScanningBLE.value = true
            Handler(Looper.getMainLooper()).postDelayed({
                isScanningBLE.postValue(false)
                bluetoothLeScanner.stopScan(scanCallback)
                bluetoothAdapter.startDiscovery()
                isScanningBluetooth.postValue(true)
            }, SCAN_PERIOD)
        }
    }

    // for BLE devices (not Bluetooth devices)
    private val scanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.let {
                for (item in results) {
                    addBluetoothDevice(item.device, 1)
                }
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                Log.i("one by one", "added a device")
                addBluetoothDevice(device, 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.i("scan callback", "there is a scanning error $errorCode")
        }
    }


    fun onDeviceClicked(device: BluetoothDeviceDetail) {
        _chosenDevice.value = device
    }

    fun finishDevice(device: BluetoothDeviceDetail) {
        _chosenDevice.value = null
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
package com.bitpunchlab.android.analyzer.models

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BluetoothDeviceDetail(
    val device : BluetoothDevice,
    val rssi : Int
) : Parcelable
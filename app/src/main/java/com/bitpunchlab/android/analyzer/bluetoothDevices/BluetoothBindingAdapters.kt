package com.bitpunchlab.android.analyzer.bluetoothDevices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.customViews.SignalBarView

@BindingAdapter("processBluetoothSignal")
fun setBluetoothStrength(view: SignalBarView, level: Int) {
    val strength = when (level) {
        in -50..100 -> 4
        in -60..-51 -> 3
        in -70..-61 -> 2
        in -150..-71 -> 1
        else -> 0

    }
    view.setStrength(strength)
    view.setSignalColor(R.color.signals_bluetooth)
}

@SuppressLint("SetTextI18n")
@BindingAdapter("processBondState")
fun getBondState(view: TextView, state: Int) {
    val bondState = when (state) {
        BluetoothDevice.BOND_BONDING -> "Bonding"
        BluetoothDevice.BOND_BONDED -> "Bonded"
        BluetoothDevice.BOND_NONE -> "No Bond"
        else -> "No Bond"
    }
    view.text = "Bond State:  $bondState"
}
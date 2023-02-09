package com.bitpunchlab.android.analyzer.wifiDevices

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.customViews.SignalBarView


@BindingAdapter("processSignal")
fun parseSignal(view: TextView, level: Int) {

    var result = when (level) {
        in -50..100 -> "Excellent"
        in -60..-51 -> "Good"
        in -70..-61 -> "Fair"
        in -150..-71 -> "Weak"
        else -> "Unknown"
    }
    view.text = "Signal:  $result"

}
@BindingAdapter("processWifiSignal")
fun setWifiStrength(view: SignalBarView, level: Int) {
    val strength = when (level) {
        in -50..100 -> 4
        in -60..-51 -> 3
        in -70..-61 -> 2
        in -150..-71 -> 1
        else -> 0

    }
    view.setStrength(strength)
    view.setSignalColor(R.color.signals_wifi)
}

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
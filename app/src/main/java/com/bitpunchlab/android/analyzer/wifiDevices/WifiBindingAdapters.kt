package com.bitpunchlab.android.analyzer.wifiDevices

import android.widget.TextView
import androidx.databinding.BindingAdapter



@BindingAdapter("processSignal")
fun parseSignal(view: TextView, level: Int) {

    var result = when (level) {
        in -50..100 -> "Excellent"
        in -60..-51 -> "Good"
        in -70..-61 -> "Fair"
        in -150..-71 -> "Weak"
        else -> "Unknown"

    }
    view.text = result

}
package com.bitpunchlab.android.analyzer.wifiDevices

import android.net.wifi.ScanResult
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.databinding.FragmentWifiDeviceBinding


class WifiDeviceFragment : Fragment() {

    private var _binding : FragmentWifiDeviceBinding? = null
    private val binding get() = _binding!!
    private var device : ScanResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWifiDeviceBinding.inflate(inflater, container, false)
        device = requireArguments().getParcelable<ScanResult>("device")
        //Log.i("wifi device fragment", "name ${device?.BSSID}")
        binding.lifecycleOwner = viewLifecycleOwner
        binding.device = device

        displaySecurityModes()
        displayStandard()
        displaySignalStrength()
        //device.level
        // freqCenter = freqStart + 5 * channelNumber

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun displaySecurityModes() {
        //if (device!!.capabilities != null && device!!.capabilities != "") {
        val modes = identifySecurityMode(device!!.capabilities)
        if (!modes.isNullOrEmpty()) {
            binding.textviewCap.text = "Security:  ${formatModes(modes)}"
            binding.textviewCap.visibility = View.VISIBLE
        } else {
            binding.textviewCap.visibility = View.GONE
        }
    }

    // it is a string, use contains
    private fun identifySecurityMode(cap: String) : List<String> {
        //val securityModes = { WEP, WPA, WPA2, WPA_EAP, IEEE8021X, ESS }
        var modes = mutableListOf<String>("WEP", "WPA", "WPA2", "WPA_EAP", "IEEE8021X", "ESS")
        var result = mutableListOf<String>()
        modes.map { mode ->
            if (cap.contains(mode)) {
                result.add(mode)
            }
        }

        return result

    }

    private fun formatModes(cap: List<String>) : String {
        var result = ""
        cap.map { mode ->
            result += "$mode, "
        }
        return if (result != "") {
            result.substring(0, result.length-2)
        } else
            result
    }

    // deal with the case below R
    @RequiresApi(Build.VERSION_CODES.R)
    private fun displayStandard() {
        val standard = identifyStandard(device!!.wifiStandard)
        if (standard != "") {
            binding.textviewStandard.visibility = View.VISIBLE
            binding.textviewStandard.text = "Standard:  $standard"
        } else {
            binding.textviewStandard.visibility = View.GONE
        }
    }

    private fun identifyStandard(standard: Int) : String {
        return when (standard) {
            4 -> "802.11n   2.4/5 GHz"
            5 -> "802.11ac   5 GHz"
            6 -> "802.11ax   2.4/5/6 GHz"
            else -> ""
        }
    }

    private fun displaySignalStrength() {
        if (device?.level != null) {
            binding.textviewSignalStrength.visibility = View.VISIBLE
            //binding.textviewSignalStrength.text = "Signal Strength:  ${parseSignalStrength(device!!.level)}"
        } else {
            binding.textviewSignalStrength.visibility = View.GONE
        }
    }

    private fun parseSignalStrength(level: Int) : String {
        return when (level) {
            in -50 .. 100 -> "Excellent"
            in -60 .. -51 -> "Good"
            in -70 .. -61 -> "Fair"
            in -150 .. -71 -> "Weak"
            else -> "Unknown"
        }
    }
}
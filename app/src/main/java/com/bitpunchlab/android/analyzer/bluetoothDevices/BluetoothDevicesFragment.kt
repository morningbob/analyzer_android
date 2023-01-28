package com.bitpunchlab.android.analyzer.bluetoothDevices

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.databinding.FragmentBluetoothDevicesBinding

class BluetoothDevicesFragment : Fragment() {

    private var _binding : FragmentBluetoothDevicesBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBluetoothDevicesBinding.inflate(inflater, container, false)



        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        //requireActivity().unregisterReceiver(wifiReceiver)
        requireActivity().unregisterReceiver(bluetoothReceiver)
    }

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
                            //val deviceName = it.name
                            //val deviceHardwareAddress = device.address // MAC address
                        }
                    }
                }
            }
        }
    }
}
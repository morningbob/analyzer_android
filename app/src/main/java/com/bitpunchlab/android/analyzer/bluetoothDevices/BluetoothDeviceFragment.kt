package com.bitpunchlab.android.analyzer.bluetoothDevices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.databinding.FragmentBluetoothDeviceBinding
import com.bitpunchlab.android.analyzer.models.BluetoothDeviceDetail


class BluetoothDeviceFragment : Fragment() {

    private var _binding : FragmentBluetoothDeviceBinding? = null
    private val binding get() = _binding!!
    private var bluetoothDevice : BluetoothDeviceDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBluetoothDeviceBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        bluetoothDevice = requireArguments().getParcelable<BluetoothDeviceDetail>("device")
        binding.bluetooth = bluetoothDevice!!

        bluetoothDevice?.device

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
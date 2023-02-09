package com.bitpunchlab.android.analyzer.bluetoothDevices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.databinding.FragmentBluetoothDeviceBinding
import com.bitpunchlab.android.analyzer.databinding.FragmentBluetoothDevicesBinding
import com.bitpunchlab.android.analyzer.devices.BluetoothClickListener
import com.bitpunchlab.android.analyzer.devices.BluetoothDeviceAdapter
import com.bitpunchlab.android.analyzer.devices.BluetoothDeviceViewModel
import com.bitpunchlab.android.analyzer.devices.BluetoothDeviceViewModelFactory


class BluetoothDevicesFragment : Fragment() {

    private var _binding : FragmentBluetoothDevicesBinding? = null
    private val binding get() = _binding!!
    private lateinit var bluetoothDeviceViewModel: BluetoothDeviceViewModel
    private lateinit var bluetoothAdapter: BluetoothDeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBluetoothDevicesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        bluetoothDeviceViewModel = ViewModelProvider(requireActivity(),
            BluetoothDeviceViewModelFactory(requireActivity().application))
            .get(BluetoothDeviceViewModel::class.java)
        bluetoothAdapter = BluetoothDeviceAdapter(BluetoothClickListener { device ->
            bluetoothDeviceViewModel.onDeviceClicked(device)
        })
        binding.bluetoothRecycler.adapter = bluetoothAdapter

        startScanning()

        bluetoothDeviceViewModel.bluetoothList.observe(viewLifecycleOwner, Observer { devices ->
            if (!devices.isNullOrEmpty()) {
                bluetoothAdapter.submitList(devices)
                bluetoothAdapter.notifyDataSetChanged()
            }
        })

        bluetoothDeviceViewModel.chosenDevice.observe(viewLifecycleOwner, Observer { device ->
            device?.let {
                val bundle = Bundle()
                bundle.putParcelable("device", device)
                findNavController().navigate(R.id.toBluetoothDeviceAction, bundle)
                bluetoothDeviceViewModel.finishDevice(device)
            }
        })

        bluetoothDeviceViewModel.isScanningBluetooth.observe(viewLifecycleOwner, Observer { status ->
            if (status) {
                binding.textviewBluetoothStatus.text = getString(R.string.scanning)
                //binding.textviewBluetoothStatus.isClickable = false
            } else {
                binding.textviewBluetoothStatus.text = getString(R.string.not_scanning)
                //binding.textviewBluetoothStatus.isClickable = true
            }
        })

        binding.textviewBluetoothStatus.setOnClickListener {
            if (bluetoothDeviceViewModel.isScanningBluetooth.value == false) {
                startScanning()
            }
        }

        Log.i("bluetooth devices fragment", "resource signal bluetooth: ${R.color.signals_bluetooth}")

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startScanning() {
        bluetoothDeviceViewModel.scanBluetoothDevices()
    }
}
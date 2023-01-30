package com.bitpunchlab.android.analyzer.devices

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.databinding.FragmentDevicesBinding

class DevicesFragment : Fragment() {

    private var _binding : FragmentDevicesBinding? = null
    private val binding get() = _binding!!
    private lateinit var wifiDeviceViewModel : WifiDeviceViewModel
    private lateinit var bluetoothDeviceViewModel : BluetoothDeviceViewModel
    private lateinit var wifiDeviceAdapter : WifiDeviceAdapter
    private lateinit var bluetoothDeviceAdapter : BluetoothDeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        wifiDeviceViewModel = ViewModelProvider(requireActivity(),
            WifiDeviceViewModelFactory(requireActivity().application))
            .get(WifiDeviceViewModel::class.java)
        bluetoothDeviceViewModel = ViewModelProvider(requireActivity(),
            BluetoothDeviceViewModelFactory(requireActivity().application)
        )
            .get(BluetoothDeviceViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        // default to Wifi
        wifiDeviceAdapter = WifiDeviceAdapter( WifiOnClickListener { device ->
            // to do
            wifiDeviceViewModel.onDeviceClicked(device)
        })
        bluetoothDeviceAdapter = BluetoothDeviceAdapter(BluetoothClickListener { device ->
            // to do
            bluetoothDeviceViewModel.onDeviceClicked(device)
        })
        binding.wifiDeviceRecycler.adapter = wifiDeviceAdapter
        binding.bluetoothDeviceRecycler.adapter = bluetoothDeviceAdapter

        wifiDeviceViewModel.isScanningWifi.observe(viewLifecycleOwner, Observer { scanning ->
            if (scanning) {
                binding.textviewWifiStatus.text = "Scanning"
            } else {
                binding.textviewWifiStatus.text = "Not scanning"
            }
        })

        bluetoothDeviceViewModel.isScanningBLE.observe(viewLifecycleOwner, Observer { scanning ->
            if (scanning || bluetoothDeviceViewModel.isScanningBluetooth.value == true) {
                binding.textviewBluetoothStatus.text = "Scanning"
            } else {
                binding.textviewBluetoothStatus.text = "Not scanning"
            }
        })


        binding.buttonWifi.setOnClickListener {
            processWifiScanningRequest()
        }

        binding.buttonBluetooth.setOnClickListener {
            processBluetoothScanningRequest()
        }


        wifiDeviceViewModel.scanWifiResults.observe(viewLifecycleOwner, Observer { results ->
            results?.let {
                wifiDeviceAdapter.submitList(results)
                wifiDeviceAdapter.notifyDataSetChanged()
            }
        })

        bluetoothDeviceViewModel.bluetoothDevices.observe(viewLifecycleOwner, Observer { devices ->
            devices?.let {
                bluetoothDeviceAdapter.submitList(devices)
                bluetoothDeviceAdapter.notifyDataSetChanged()
            }
        })

        wifiDeviceViewModel.chosenDevice.observe(viewLifecycleOwner, Observer { device ->
            // to do
        })

        bluetoothDeviceViewModel.chosenDevice.observe(viewLifecycleOwner, Observer { device ->
            // to do
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val wifiPermissionResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            //var result = true
            //allPermissionGranted = true
            permissions.entries.forEach {
                if (!it.value) {
                    Log.e("result launcher", "Permission ${it.key} not granted : ${it.value}")
                    // I don't check the permissions here.
                    // I'll check again when the user clicks the scan button again
                    // let the check permission test check it again
                } else {
                    Log.i("permission result launcher","granted ${it.key}")
                }
            }
            //isPermissionGranted = result
        }

    private var wifiPermissions =
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

    private val bluetoothPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

    private fun checkPermission(permissions: Array<String>) : Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) !=
                PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }

    private fun processWifiScanningRequest() {
        Log.i("process scan request", "started")
        if (checkPermission(wifiPermissions)) {
            //binding.scanResultRecycler.adapter = wifiDeviceAdapter
            wifiDeviceViewModel.wifiManager!!.startScan()
            wifiDeviceViewModel.isScanningWifi.value = true
            Log.i("wifi fragment", "start scanning wifi")
        } else {
            // request wifi permissions
            wifiPermissionResultLauncher.launch(wifiPermissions)
        }
    }

    private fun processBluetoothScanningRequest() {
        Log.i("process scan request", "started")
        if (checkPermission(bluetoothPermissions)) {
            //binding.scanResultRecycler.adapter = bluetoothDeviceAdapter
            //bluetoothDeviceViewModel.scanBluetoothDevices()
            bluetoothDeviceViewModel.scanBLEDevices()
            //isScanningBluetooth.value = true
            Log.i("wifi fragment", "starxxxxxxxxxxxxt scanning wifi")
        } else {
            // request wifi permissions
            wifiPermissionResultLauncher.launch(bluetoothPermissions)
        }
    }
}
package com.bitpunchlab.android.analyzer.wifiDevices

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.ScanResultAdapter
import com.bitpunchlab.android.analyzer.databinding.FragmentWifiDevicesBinding
import com.bitpunchlab.android.analyzer.devices.WifiDeviceViewModel
import com.bitpunchlab.android.analyzer.devices.WifiDeviceViewModelFactory

class WifiDevicesFragment : Fragment() {

    private var _binding : FragmentWifiDevicesBinding? = null
    private val binding get() = _binding!!
    private lateinit var wifiDeviceViewModel : WifiDeviceViewModel
    private lateinit var scanResultAdapter: ScanResultAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWifiDevicesBinding.inflate(inflater, container, false)
        wifiDeviceViewModel = ViewModelProvider(requireActivity(),
            WifiDeviceViewModelFactory(requireActivity().application)
        )
            .get(WifiDeviceViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner
        scanResultAdapter = ScanResultAdapter()
        binding.scanWifiResultRecycler.adapter = scanResultAdapter

        checkPermission(wifiPermissions)
        registerWifiReceiver()

        wifiDeviceViewModel.scanWifiResults.observe(viewLifecycleOwner, Observer { results ->
            results?.let {
                scanResultAdapter.submitList(results)
                scanResultAdapter.notifyDataSetChanged()
            }
        })

        binding.buttonScan.setOnClickListener {
            processWifiScanningRequest()
        }

        return binding.root
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
            wifiDeviceViewModel.wifiManager!!.startScan()
            Log.i("wifi fragment", "start scanning wifi")
        } else {
            // request wifi permissions
            wifiPermissionResultLauncher.launch(wifiPermissions)
        }
    }


    private fun registerWifiReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context?.registerReceiver(wifiReceiver, intentFilter)
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //intent?.let {
            Log.i("main fragment", "onReceive called")
            val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success != null)
                if (success) {
                    scanSuccess()
                } else {
                    //scanFailure()
                    // alert user the error
                }//}

        }
    }

    @SuppressLint("NotifyDataSetChanged", "MissingPermission")
    private fun scanSuccess() {
        wifiDeviceViewModel.scanWifiResults.value = wifiDeviceViewModel.wifiManager?.scanResults
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireActivity().unregisterReceiver(wifiReceiver)
        //requireActivity().unregisterReceiver(bluetoothReceiver)
    }
}
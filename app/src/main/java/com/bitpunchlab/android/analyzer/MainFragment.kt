package com.bitpunchlab.android.analyzer

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.analyzer.bluetoothDevices.BLEDeviceViewModel
import com.bitpunchlab.android.analyzer.databinding.FragmentMainBinding
import com.bitpunchlab.android.analyzer.wifiDevices.WifiDeviceViewModel
import com.bitpunchlab.android.analyzer.wifiDevices.WifiDeviceViewModelFactory
import java.util.Scanner


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var scanResultAdapter: ScanResultAdapter
    private var scanResults = MutableLiveData<List<ScanResult>>()
    private lateinit var bluetoothAdapter : BluetoothAdapter
    private lateinit var bleDeviceViewModel : BLEDeviceViewModel
    private lateinit var wifiDeviceViewModel: WifiDeviceViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        scanResultAdapter = ScanResultAdapter()
        bleDeviceViewModel = ViewModelProvider(requireActivity())
            .get(BLEDeviceViewModel::class.java)
        wifiDeviceViewModel = ViewModelProvider(requireActivity(),
            WifiDeviceViewModelFactory(requireActivity().application))
            .get(WifiDeviceViewModel::class.java)

        setupMenu()

        // get the wifi manager here, if the user scans for Wifi,
        // we'll start the scanning.
        //wifiDeviceViewModel.wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager
        //registerWifiReceiver()

        wifiDeviceViewModel.scanWifiResults.observe(viewLifecycleOwner, Observer { results ->
            results?.let {
                scanResultAdapter.submitList(results)
                scanResultAdapter.notifyDataSetChanged()
            }
        })

        return binding.root

    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.wifi -> {
                        findNavController().navigate(R.id.toDeviceListAction)
                        true
                    }
                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }








    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        //requireActivity().unregisterReceiver(wifiReceiver)
        //requireActivity().unregisterReceiver(bluetoothReceiver)
    }

    private fun getBatteryLevel() {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        val batteryStatus = requireContext().registerReceiver(null, intentFilter)

        var level = -1
        var scale = -1

        if (batteryStatus != null) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        }

        val batteryPercentage = (level * 100) / scale

        Log.i("battery level", "got % $batteryPercentage")
        //} else
    }

}

/*
    @SuppressLint("NotifyDataSetChanged")
    private fun getProcesses() {
        val activityManager = requireActivity()
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        //val processInfos = activityManager.runningAppProcesses
        val processInfos = Runtime.getRuntime().exec("/system/bin/ps")

        val processes = processInfos.map { info ->
            Log.i("get processes", "parsed 1 process: $info.name")
            Process(id = info.pid.toString(), name = info.processName)

        }

        processAdapter.submitList(processes)
        processAdapter.notifyDataSetChanged()

    }

 */
//}
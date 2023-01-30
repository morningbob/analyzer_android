package com.bitpunchlab.android.analyzer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.analyzer.devices.BluetoothDeviceViewModel
import com.bitpunchlab.android.analyzer.devices.BluetoothDeviceViewModelFactory
import com.bitpunchlab.android.analyzer.databinding.FragmentMainBinding
import com.bitpunchlab.android.analyzer.devices.WifiDeviceViewModel
import com.bitpunchlab.android.analyzer.devices.WifiDeviceViewModelFactory


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMainBinding.inflate(inflater, container, false)

        setupMenu()

        return binding.root

    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.nearbyDevices -> {
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
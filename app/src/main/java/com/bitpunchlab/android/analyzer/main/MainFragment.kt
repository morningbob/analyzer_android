package com.bitpunchlab.android.analyzer.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.security.Permissions


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private var userLocation : Location? = null

    @SuppressLint("MissingPermission", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMainBinding.inflate(inflater, container, false)

        setupMenu()

        binding.lifecycleOwner = viewLifecycleOwner
        binding.userLocation = userLocation

        val internalCapacity = getInternalStorageCapacity()
        val internalTotal = internalCapacity.first
        val internalAvailable = internalCapacity.second
        binding.textviewInternalSize.text = formatSize(internalAvailable)

        val externalCapacity = getExternalStorageCapacity()
        val externalTotal = externalCapacity.first
        val externalAvailable = externalCapacity.second
        binding.textviewExternalSize.text = formatSize(externalAvailable)

        binding.textviewRamSize.text = formatSize(getRAMSize().second)

        if (checkPermission(locationPermissions)) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

            fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
                Log.i("fused client", "latitude: ${location?.latitude}, longitude: ${location?.longitude}")
                userLocation = location
            }
            // show location
            //binding.textviewLatitude.text = "latitude: ${userLocation?.latitude}"
            //binding.textviewLongitide.text = "longitude: ${userLocation?.longitude}"
            binding.textviewLatitude.visibility = View.VISIBLE
            binding.textviewLongitide.visibility = View.VISIBLE
            binding.textviewGetLocation.visibility = View.GONE
        } else {
            binding.textviewGetLocation.setOnClickListener {
                //checkPermission(locationPermissions)
                permissionsResultLauncher.launch(locationPermissions)
            }
            binding.textviewLatitude.visibility = View.GONE
            binding.textviewLongitide.visibility = View.GONE
            binding.textviewGetLocation.visibility = View.VISIBLE
        }


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

    private fun getInternalStorageCapacity() : Pair<Long, Long> {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val availableSize = stat.blockSizeLong * stat.availableBlocksLong
        //Log.i("total internal memory size", totalSize.toString())
        val totalSize = stat.blockSizeLong * stat.blockCountLong
        return Pair(totalSize, availableSize)
    }

    private fun getExternalStorageCapacity() : Pair<Long, Long> {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)
        val availableSize = stat.blockSizeLong * stat.availableBlocksLong
        val totalSize = stat.blockSizeLong * stat.blockCountLong
        return Pair(totalSize, availableSize)
    }

    private fun formatSize(size: Long) : String {
        var suffix : String? = null
        var result = size

        if (result >= 1024) {
            suffix = "KB"
            result /= 1024
            if (result >= 1024) {
                suffix = "MB"
                result /= 1024
            }
        }

        val resultBuffer = java.lang.StringBuilder(result.toString())
        var commaOffset = resultBuffer.length - 3
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',')
            commaOffset -= 3
        }

        suffix?.let {
            resultBuffer.append(suffix)
        }

        return resultBuffer.toString()
    }

    private fun getRAMSize() : Pair<Long, Long> {
        val activityManager = requireContext().getSystemService<ActivityManager>()
        var memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        return Pair(memInfo.totalMem, memInfo.availMem)
    }

    private val permissionsResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                if (!it.value) {
                    Log.i("result launcher", "Permission ${it.key} not granted")
                } else {
                    Log.i("result launcher", "Permission ${it.key} granted")
                }
            }
        }

    private val locationPermissions =
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    private fun checkPermission(permissions: Array<String>) : Boolean {
        for (per in permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), per) !=
                PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
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
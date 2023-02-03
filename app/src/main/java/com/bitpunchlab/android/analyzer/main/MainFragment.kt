package com.bitpunchlab.android.analyzer.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.*
import android.telecom.TelecomManager
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.text.style.TtsSpan.TelephoneBuilder
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.reflect.Method
import java.security.Permissions


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private var userLocation = MutableLiveData<Location?>()
    private var connectivityManager: ConnectivityManager? = null
    private var isWifiConn: Boolean = false
    private var isMobileConn: Boolean = false
    private var wifiName = MutableLiveData<String?>()
    private var bluetoothName = MutableLiveData<String?>()
    private var isLocationPermissionGranted = MutableLiveData<Boolean>(false)
    private var isBluetoothPermissionGranted = MutableLiveData<Boolean>(false)


    @SuppressLint("MissingPermission", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMainBinding.inflate(inflater, container, false)

        setupMenu()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        connectivityManager = requireContext().getSystemService<ConnectivityManager>()

        binding.lifecycleOwner = viewLifecycleOwner
        //binding.userLocation = userLocation
        //setupNetworkCallback()
        // to get the name of the wifi connection, if there is one
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
        prepareSimCard()
        prepareBattery()

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
            prepareLocation()
            val enabled = isLocationEnabled()
            Log.i("upon start", "location is enabled? $enabled")
            // show location
            //binding.textviewLatitude.text = "latitude: ${userLocation?.latitude}"
            //binding.textviewLongitude.text = "longitude: ${userLocation?.longitude}"

            //binding.textviewGetLocation.visibility = View.GONE
        } else {

            //binding.textviewGetLocation.visibility = View.VISIBLE
        }

        binding.networkStatus.text = "Active: ${checkActiveNetwork()}"
        //binding.networkName.text = wifiName.value ?: ""
        wifiName.observe(viewLifecycleOwner, Observer { name ->
            name?.let {
                binding.networkName.text = name
            }
        })

        // prepare bluetooth display
        if (checkPermission(bluetoothPermissions)) {
            //binding.blueStatus.visibility = View.VISIBLE
            //binding.blueDevice.visibility = View.VISIBLE
            //binding.buttonCheckBlue.visibility = View.GONE
        } else {
            //binding.buttonCheckBlue.visibility = View.VISIBLE
            //binding.blueStatus.visibility = View.GONE
            //binding.blueDevice.visibility = View.GONE
        }

        userLocation.observe(viewLifecycleOwner, Observer { location ->
            binding.userLocation = location
            binding.textviewLatitude.text = "lat: ${formatCoordinate(location!!.latitude)}"
            binding.textviewLongitude.text = "lng: ${formatCoordinate(location!!.longitude)}"
        })

        bluetoothName.observe(viewLifecycleOwner, Observer { name ->
            if (name != null) {
                binding.blueStatus.text = "Connected: $name"
            } else {
                binding.blueStatus.text = "Not Connected"
            }
        })

        binding.textviewGetLocation.setOnClickListener {
            locationPermissionsResultLauncher.launch(locationPermissions)
        }

        binding.buttonCheckBlue.setOnClickListener {
            prepareBluetooth()
        }

        isLocationPermissionGranted.observe(viewLifecycleOwner, Observer { granted ->
            if (granted != null && granted == true) {
                binding.textviewGetLocation.visibility = View.GONE
                binding.textviewLatitude.visibility = View.VISIBLE
                binding.textviewLongitude.visibility = View.VISIBLE
            } else {
                binding.textviewGetLocation.visibility = View.VISIBLE
                binding.textviewLatitude.visibility = View.GONE
                binding.textviewLongitude.visibility = View.GONE
            }
        })

        isBluetoothPermissionGranted.observe(viewLifecycleOwner, Observer { granted ->
            if (granted != null && granted == true) {
                binding.buttonCheckBlue.visibility = View.GONE
                binding.blueStatus.visibility = View.VISIBLE
                binding.blueDevice.visibility = View.VISIBLE
            } else {
                binding.buttonCheckBlue.visibility = View.VISIBLE
                binding.blueStatus.visibility = View.GONE
                binding.blueDevice.visibility = View.GONE
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

    private fun getBatteryLevel() : String {
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
        return batteryPercentage.toString()
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
            suffix = " KB"
            result /= 1024
            if (result >= 1024) {
                suffix = " MB"
                result /= 1024
                if (result >= 1024) {
                    suffix = " GB"
                    result /= 1024
                }
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

    private val locationPermissionsResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var allResults = true
            permissions.entries.forEach {
                if (!it.value) {
                    Log.i("result launcher", "Permission ${it.key} not granted")
                    allResults = false
                } else {
                    Log.i("result launcher", "Permission ${it.key} granted")
                }
            }
            isLocationPermissionGranted.postValue(allResults)
        }

    private val bluetoothPermissionsResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var allResults = true
            permissions.entries.forEach {
                if (!it.value) {
                    Log.i("result launcher", "Permission ${it.key} not granted")
                    allResults = false
                } else {
                    Log.i("result launcher", "Permission ${it.key} granted")
                }
            }
            isBluetoothPermissionGranted.postValue(allResults)
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


    @SuppressLint("MissingPermission")
    private suspend fun findCurrentLocation() : Location? =
        suspendCancellableCoroutine<Location?> { cancellableContinuation ->
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                //Log.i("fused client", "latitude: ${location?.latitude}, longitude: ${location?.longitude}")
                //userLocation = location
                if (task.isSuccessful && task.result != null) {
                    //userLocation = task.result
                    cancellableContinuation.resume(task.result) {}
                } else {
                    Log.i("fused Location", "error getting location")
                    cancellableContinuation.resume(null) {}
                }
            }
        }

    // if we got the permissions, we execute find location,
    // and show the lat lng
    private fun prepareLocation() {
        CoroutineScope(Dispatchers.IO).launch {
            userLocation.postValue(findCurrentLocation())
            //Log.i("prepare location")
        }
    }

    private fun isLocationEnabled() : Boolean{
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun formatCoordinate(cor: Double) : String {
        val corString = cor.toString()
        return corString.substring(0, 7)
    }

    // this function checks what the network is, wifi, or mobile
    private fun checkActiveNetwork() : String {
        val network = connectivityManager?.activeNetwork
        // network here represent if there is a network
        // that means, it is available
        if (network != null) {
            connectivityManager
            val activeNetworkCap = connectivityManager!!.getNetworkCapabilities(network)

            // here, check if it is connected
            when {
                activeNetworkCap?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> return "Wifi"
                activeNetworkCap?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> return "Mobile"
                else -> return "None"
            }
        }
        return "None"

    }

    // use this method to get wifi name before api Q, 29
    private fun getNameWifi() {
        val wifiManager = requireContext().getSystemService<WifiManager>()
        wifiManager
    }

    private val networkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val wifiIno = networkCapabilities.transportInfo as WifiInfo

            wifiName.postValue(wifiIno.ssid)
        }
    }

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

    private fun prepareBluetooth() {
        if (checkPermission(bluetoothPermissions)) {
            bluetoothName.value = getConnectedBluetoothDevice()
        } else {
            bluetoothPermissionsResultLauncher.launch(bluetoothPermissions)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getConnectedBluetoothDevice() : String? {
        val bluetoothManager = requireContext().getSystemService<BluetoothManager>()
        val pairedDevices = bluetoothManager?.adapter?.bondedDevices
        Log.i("get paired devices", pairedDevices?.size.toString())
        //val connectedDevices = bluetoothManager?.getConnectedDevices()
        pairedDevices?.map { device ->
            if (isConnected(device)) {
                return device.name
            }
        }
        return null
    }

    // this method is used to test if the device is connected
    private fun isConnected(device: BluetoothDevice) : Boolean {
        return try {
            val method: Method = device.javaClass.getMethod("isConnected")
            method.invoke(device) as Boolean
        } catch (e: java.lang.Exception) {
            throw java.lang.IllegalStateException(e)
        }
    }

    //@RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    private fun prepareSimCard() {
        val telephonyManager = requireContext().getSystemService<TelephonyManager>()
        //val simSN = telephonyManager?.simSerialNumber
        //Log.i("sim card", "sn: $simSN")
        //val simLineNumber = telephonyManager?.line1Number
        //Log.i("sim card", "line number: $simLineNumber")
        var carrierName : String? = null
        var simSignal : Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            carrierName = telephonyManager?.simCarrierIdName.toString()
            carrierName?.let {
                binding.simCarrierName.text = "$carrierName"
                binding.simCarrierName.visibility = View.VISIBLE
            }
            simSignal = telephonyManager?.signalStrength?.level
            simSignal?.let {
                binding.simSignal.text = "Signal: ${simSignal.toString()}"
                binding.simSignal.visibility = View.VISIBLE
            }
        } else {
            binding.simCarrierName.visibility = View.GONE
            binding.simSignal.visibility = View.GONE
        }
        val simState = telephonyManager?.simState
        when (simState) {
            TelephonyManager.SIM_STATE_ABSENT -> binding.simCardStatus.text = "no sim card"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> binding.simCardStatus.text = "network locked"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> binding.simCardStatus.text = "pin required"
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> binding.simCardStatus.text = "puk required"
            TelephonyManager.SIM_STATE_READY -> binding.simCardStatus.text = "state ready"
            TelephonyManager.SIM_STATE_UNKNOWN -> binding.simCardStatus.text = "unknown state"
            else -> binding.simCardStatus.text = "unknown state"
        }

    }

    private fun prepareBattery() {
        binding.batteryLevel.text = getBatteryLevel().toString()
    }



/*
    private fun getNewLocation() {
        val locationRequest = com.google.android.gms.location.LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 2
        fusedLocationClient!!.requestLocationUpdates(
            locationRequest, locationCallback, null
        )
    }

    @SuppressLint("MissingPermission")
    private fun getNewLocation(){
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 2
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest, locationCallback, null
        )

    }

 */
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
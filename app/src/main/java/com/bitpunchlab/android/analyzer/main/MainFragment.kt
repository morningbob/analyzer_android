package com.bitpunchlab.android.analyzer.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.TelephonyManager
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.analyzer.BatteryInfo
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.reflect.Method


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private var userLocation = MutableLiveData<Location?>()
    private var connectivityManager: ConnectivityManager? = null
    private var wifiName = MutableLiveData<String?>()
    private var bluetoothName = MutableLiveData<String?>()
    private var isLocationPermissionGranted = MutableLiveData<Boolean>()
    private var isBluetoothPermissionGranted = MutableLiveData<Boolean>()
    private var isWifiPermissionGranted = MutableLiveData<Boolean>()
    private var usbDevices = MutableLiveData<List<UsbDevice>?>()
    private lateinit var generalInfoViewModel: GeneralInfoViewModel


    @SuppressLint("MissingPermission", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMainBinding.inflate(inflater, container, false)
        generalInfoViewModel = ViewModelProvider(requireActivity(),
            GeneralInfoViewModelFactory(requireActivity().application))
            .get(GeneralInfoViewModel::class.java)

        setupMenu()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        connectivityManager = requireContext().getSystemService<ConnectivityManager>()

        binding.lifecycleOwner = viewLifecycleOwner

        //setupNetworkCallback()
        // to get the name of the wifi connection, if there is one
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
        prepareSimCard()
        prepareBattery()
        prepareUSB()

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
            getUserLocation()
            val enabled = isLocationEnabled()
            Log.i("upon start", "location is enabled? $enabled")
            // show location
            //binding.textviewLatitude.text = "latitude: ${userLocation?.latitude}"
            //binding.textviewLongitude.text = "longitude: ${userLocation?.longitude}"
            binding.textviewLatitude.visibility = View.VISIBLE
            binding.textviewLongitude.visibility = View.VISIBLE
            binding.textviewGetLocation.visibility = View.GONE
        } else {
            binding.textviewGetLocation.visibility = View.VISIBLE
            binding.textviewLatitude.visibility = View.GONE
            binding.textviewLongitude.visibility = View.GONE
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
            binding.blueStatus.visibility = View.VISIBLE
            binding.blueDevice.visibility = View.VISIBLE
            binding.buttonCheckBlue.visibility = View.GONE
        } else {
            binding.buttonCheckBlue.visibility = View.VISIBLE
            binding.blueStatus.visibility = View.GONE
            binding.blueDevice.visibility = View.GONE
        }

        userLocation.observe(viewLifecycleOwner, Observer { location ->
            location?.let {
                binding.userLocation = location
                binding.textviewLatitude.text = "lat: ${formatCoordinate(location.latitude)}"
                binding.textviewLongitude.text = "lng: ${formatCoordinate(location.longitude)}"
            }
        })

        bluetoothName.observe(viewLifecycleOwner, Observer { name ->
            if (name != null) {
                binding.blueStatus.text = "Connected: $name"
            } else {
                binding.blueStatus.text = "Not Connected"
            }
        })

        usbDevices.observe(viewLifecycleOwner, Observer { devices ->
            if (!devices.isNullOrEmpty()) {
                binding.firstUsb.text = "Connected: $devices[0]"
                binding.noUsb.text = "No of devices: ${devices.size}"
            }
        })

        // make sure the wifi permissions are granted
        binding.signalLayout.setOnClickListener {
            if (checkPermission(wifiPermissions)) {
                isWifiPermissionGranted.value = true
                findNavController().navigate(R.id.toWifiAction)
            } else {
                wifiPermissionsResultLauncher.launch(wifiPermissions)
            }
        }

        // if the get location button is displayed, that means the permissions are not granted
        // so, upon click, we request it
        binding.textviewGetLocation.setOnClickListener {
            locationPermissionsResultLauncher.launch(locationPermissions)
        }

        binding.blueLayout.setOnClickListener {
            if (checkPermission(bluetoothPermissions)) {
                isBluetoothPermissionGranted.value = true
                findNavController().navigate(R.id.toBluetoothAction)
            } else {
                bluetoothPermissionsResultLauncher.launch(bluetoothPermissions)
            }
        }

        binding.buttonCheckBlue.setOnClickListener {
            prepareBluetooth()
        }

        binding.compassLayout.setOnClickListener {
            findNavController().navigate(R.id.toCompassAction)
        }

        // this part is for the occasion that the user clicked get location
        // and granted the permissions
        // the app can react immediately to detect the location and show it
        isLocationPermissionGranted.observe(viewLifecycleOwner, Observer { granted ->
            if (granted != null && granted == true) {
                binding.textviewGetLocation.visibility = View.GONE
                binding.textviewLatitude.visibility = View.VISIBLE
                binding.textviewLongitude.visibility = View.VISIBLE
                getUserLocation()
            } else if (granted == false) {
                binding.textviewGetLocation.visibility = View.VISIBLE
                binding.textviewLatitude.visibility = View.GONE
                binding.textviewLongitude.visibility = View.GONE
            }
        })

        // this part is for the occasion that the user clicked check bluetooth
        // and granted the permissions
        // the app can react immediately to detect the bluetooth and show it
        isBluetoothPermissionGranted.observe(viewLifecycleOwner, Observer { granted ->
            if (granted != null && granted == true) {
                binding.buttonCheckBlue.visibility = View.GONE
                binding.blueStatus.visibility = View.VISIBLE
                binding.blueDevice.visibility = View.VISIBLE
                prepareBluetooth()
            } else if (granted == false) {
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

    private fun getBatteryStatus() : HashMap<BatteryInfo, Int> {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        val batteryStatus = requireContext().registerReceiver(null, intentFilter)

        var level = -1
        var scale = -1
        var voltage = -1
        var temp = -1
        var health = -1
        //var isCharging = -1

        if (batteryStatus != null) {
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            //isCharging = batteryStatus.getIntExtra(BatteryManager.ACTION_CHARGING, -1)
        }

        val batteryPercentage = (level * 100) / scale

        //Log.i("battery level", "got % $batteryPercentage")
        val hashmap = HashMap<BatteryInfo, Int>()
        hashmap[BatteryInfo.LEVEL] = batteryPercentage
        hashmap[BatteryInfo.VOLTAGE] = voltage
        hashmap[BatteryInfo.TEMPERATURE] = temp
        hashmap[BatteryInfo.HEALTH] = health

        val isCharging = requireContext().getSystemService<BatteryManager>()?.isCharging
        if (isCharging == true) {

        }
        hashmap[BatteryInfo.IS_CHARGING] =

        return hashmap
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

    private val wifiPermissionsResultLauncher =
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
            isWifiPermissionGranted.postValue(allResults)
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

    private val usbPermissionsResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var allResults = true
            permissions.entries.forEach {
                if (!it.value) {
                    //Log.i("result launcher", "Permission ${it.key} not granted")
                    //allResults = false
                    Log.i("usb permission", "not granted")
                } else {
                    //Log.i("result launcher", "Permission ${it.key} granted")
                    Log.i("usb permission", "granted")
                }
            }
            //isBluetoothPermissionGranted.postValue(allResults)
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
    private fun getUserLocation() {
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
            return when {
                activeNetworkCap?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wifi"
                activeNetworkCap?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
                else -> "None"
            }
        }
        return "None"

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
        var carrierName : String?
        var simSignal : Int?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            carrierName = telephonyManager?.simCarrierIdName.toString()
            carrierName?.let {
                binding.simCarrierName.text = "$carrierName"
                binding.simCarrierName.visibility = View.VISIBLE
            }
            simSignal = telephonyManager?.signalStrength?.level
            simSignal?.let {
                binding.simSignal.text = "Strength: ${simSignal.toString()}"
                binding.simSignal.visibility = View.VISIBLE
            }
        } else {
            binding.simCarrierName.visibility = View.GONE
            binding.simSignal.visibility = View.GONE
        }
        val simState = telephonyManager?.simState
        simState.let {
            binding.simCardStatus.text = parseSimState(it!!)
        }

    }

    private fun parseSimState(state: Int) : String {
        return when (state) {
            TelephonyManager.SIM_STATE_ABSENT -> "no sim card"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "network locked"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> "pin required"
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> "puk required"
            TelephonyManager.SIM_STATE_READY -> "state ready"
            -1 -> "can't read"
            else -> "unknown state"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun prepareBattery() {
        val status = getBatteryStatus()
        binding.batteryLevel.text = status[BatteryInfo.LEVEL].toString()
        binding.batteryVoltage.text = "volt: ${status[BatteryInfo.VOLTAGE].toString()}"
        binding.batteryTemp.text = "temp: ${status[BatteryInfo.TEMPERATURE].toString()}"
        status[BatteryInfo.HEALTH]?.let {
            binding.batteryHealth.text = "health: ${parseBatteryHealth(it!!)}"
        }
        val isCharging = requireContext().getSystemService<BatteryManager>()?.isCharging
        if (isCharging != null && isCharging == true) {
            binding.batteryCharging.text = "Charging"
        } else {
            binding.batteryCharging.text = "Not Charging"
        }
    }

    private fun parseBatteryHealth(health: Int) : String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_COLD -> "cold"
            BatteryManager.BATTERY_HEALTH_DEAD -> "died"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over voltage"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "over heat"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "failure"
            -1 -> return "can't read"
            else -> "unknown"
        }
    }

    private val usbDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device: UsbDevice? = intent?.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)

        }

    }

    private fun getConnectedUSB() : List<UsbDevice>? {
        val usbManager = requireContext().getSystemService<UsbManager>()
        usbManager?.let {
            Log.i("get connected usb", "got usb manager")
        }
        val devicesList = usbManager?.deviceList?.map { usbMap ->
            usbMap.value
        }
        Log.i("get connected usb", "connected USBs: ${devicesList?.size}")


        return devicesList
    }

    private fun prepareUSB() {

        usbDevices.value = getConnectedUSB()
        if (usbDevices.value != null && usbDevices.value!!.isNotEmpty()) {
            binding.firstUsb.visibility = View.VISIBLE
            binding.noUsb.visibility = View.VISIBLE
            binding.firstUsb.text = "Connected to USB"
            binding.noUsb.text = usbDevices.value!!.size.toString()
        } else {
            binding.firstUsb.visibility = View.INVISIBLE
            binding.noUsb.visibility = View.INVISIBLE
        }

        // since, when we listen to the usb state, state connected detected
        // it might not be pc, it can be usb connected
        // so if the usbManager.getDeviceList shows 0,
        // we can say, it is probably connected to a pc
        // we are the usb client and pc is the host
        // when we are the host, there is no usb detected
        val connected = isConnectedToPC(requireContext())

        if (connected == true &&
            (usbDevices.value == null || usbDevices.value!!.isEmpty())) {

            binding.usbStatus.text = "Connected to PC\n or accessory"
            //binding.firstUsb.visibility = View.GONE
            //binding.noUsb.visibility = View.GONE
        } else if (connected == false &&
            (usbDevices.value == null || usbDevices.value!!.isEmpty())) {
            binding.usbStatus.text = "Not Connected"
        } // there is a case when connected == false but there are usb devices in device list
        // in this case, we say nothing


    }

    private val connectedToDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i("connected to device",
                "trying to get device name ${intent?.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)}")
        }

    }

    private fun isConnectedToPC(context: Context) : Boolean? {
        val intent = context.registerReceiver(connectedToDeviceReceiver,
            IntentFilter("android.hardware.usb.action.USB_STATE"));
        return intent?.getExtras()?.getBoolean("connected");
    }

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
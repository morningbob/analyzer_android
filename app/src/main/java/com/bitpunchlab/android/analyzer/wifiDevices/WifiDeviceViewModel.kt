package com.bitpunchlab.android.analyzer.wifiDevices

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.analyzer.ScanResultAdapter

class WifiDeviceViewModel(application: Application) : AndroidViewModel(application) {

    //private lateinit var scanResultAdapter: ScanResultAdapter
    var wifiManager : WifiManager? = null
    var scanWifiResults = MutableLiveData<List<ScanResult>>()


    init {
        wifiManager = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as WifiManager
        //getApplication<Application>().registerReceiver(null, wifiReceiver)
        //registerWifiReceiver()
        Log.i("starting wifi vm", "init ran")
    }

    // need to unregister it
/*
    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            Log.i("vm", "onReceive was called")
            if (success != null)
                if (success) {
                    scanSuccess()
                } else {
                    //scanFailure()
                    // alert user the error
                }//}

        }
    }


    private fun registerWifiReceiver() {
        Log.i("vm", "registering wifi receiver")
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        getApplication<Application>().applicationContext.registerReceiver(wifiReceiver, intentFilter)
    }
*/
    @SuppressLint("NotifyDataSetChanged", "MissingPermission")
    private fun scanSuccess() {
        Log.i("vm", "scanning was successful")
        scanWifiResults.value = wifiManager?.scanResults
    }
}

class WifiDeviceViewModelFactory(private val application: Application)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WifiDeviceViewModel::class.java)) {
            return WifiDeviceViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
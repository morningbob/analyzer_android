package com.bitpunchlab.android.analyzer.wifiDevices

import android.annotation.SuppressLint
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
import com.bitpunchlab.android.analyzer.databinding.FragmentWifiDevicesBinding
import com.bitpunchlab.android.analyzer.devices.*

// upon navigation, this fragment starts scanning immediately
// the fragment assumes wifi permissions were granted before
// I'll make sure that before I navigate to this fragment
class WifiDevicesFragment : Fragment() {

    private var _binding : FragmentWifiDevicesBinding? = null
    private val binding get() = _binding!!
    private lateinit var wifiDeviceViewModel : WifiDeviceViewModel
    private lateinit var wifiDeviceAdapter : WifiDeviceAdapter

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
            WifiDeviceViewModelFactory(requireActivity().application))
            .get(WifiDeviceViewModel::class.java)
        binding.lifecycleOwner = viewLifecycleOwner

        startScanning()

        wifiDeviceAdapter = WifiDeviceAdapter(WifiOnClickListener { device ->
            // show details
            wifiDeviceViewModel.onDeviceClicked(device)
            Log.i("on click listener", "clicked")
        })

        binding.wifiRecycler.adapter = wifiDeviceAdapter

        wifiDeviceViewModel.isScanningWifi.observe(viewLifecycleOwner, Observer { scanning ->
            if (scanning) {
                binding.textviewWifiStatus.text = getString(R.string.scanning)
                //binding.textviewWifiStatus.isClickable = false
            } else {
                binding.textviewWifiStatus.text = getString(R.string.not_scanning)
                //binding.textviewWifiStatus.isClickable = true
            }
        })

        wifiDeviceViewModel.scanWifiResults.observe(viewLifecycleOwner, Observer { results ->
            results?.let {
                wifiDeviceAdapter.submitList(results)
                wifiDeviceAdapter.notifyDataSetChanged()
            }
        })

        wifiDeviceViewModel.chosenDevice.observe(viewLifecycleOwner, Observer { device ->
            // navigate to device for further checks
            device?.let {
                //val action = WifiDevicesFragment
                val bundle = Bundle()
                bundle.putParcelable("device", device)
                Log.i("chosen device", "about to navigate")
                findNavController().navigate(R.id.toDeviceAction, bundle)
                wifiDeviceViewModel.finishDevice(device)
            }
        })

        binding.textviewWifiStatus.setOnClickListener {
            if (wifiDeviceViewModel.isScanningWifi.value == false) {
                startScanning()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startScanning() {
        wifiDeviceViewModel.wifiManager!!.startScan()
        wifiDeviceViewModel.isScanningWifi.value = true
        Log.i("wifi fragment", "start scanning wifi")
    }
}
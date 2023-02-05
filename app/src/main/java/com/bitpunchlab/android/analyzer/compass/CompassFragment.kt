package com.bitpunchlab.android.analyzer.compass

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.Image
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.core.content.getSystemService
import com.bitpunchlab.android.analyzer.R
import com.bitpunchlab.android.analyzer.databinding.FragmentCompassBinding
import java.lang.Float


class CompassFragment : Fragment(), SensorEventListener{

    private var _binding : FragmentCompassBinding? = null
    private val binding get() = _binding!!
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    private var azimuth = 0
    private var currentAzimuth = 0
    private var sensorManager : SensorManager? = null
    private val alpha = 0.97f
    private var compassView : ImageView? = null
    //private var a = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCompassBinding.inflate(inflater, container, false)

        sensorManager = requireContext().getSystemService<SensorManager>()

        // we use findViewById to get the view itself,
        // if we find it from binding, it is just a reference
        compassView = binding.root.findViewById(R.id.compass)
        compassView?.let {
            Log.i("compass fragment", "got compass view")
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.let {
            it.unregisterListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.let {
            it.registerListener(
                this,
                it.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager?.let {
            it.registerListener(
                this,
                it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        //Log.i("on sensor changed", "movement detected")

        synchronized(this) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
            }

            if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * event.values[0]
                geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * event.values[1]
                geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * event.values[2]
            }

            var R = FloatArray(9)
            var I = FloatArray(9)

            val success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)
            if (success) {
                var orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                azimuth = Math.toDegrees(orientation[0].toDouble()).toInt()
                azimuth = (azimuth + 360) % 360
                //azimuth = Math.round(azimuth)
                Log.i("on sensor changed", "success, azimuth $azimuth")
            }

            val animation = RotateAnimation(
                (-currentAzimuth).toFloat(), -azimuth.toFloat(),
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f)

            currentAzimuth = azimuth

            animation.duration = 500
            animation.repeatCount = 0
            animation.fillAfter = true

            compassView?.startAnimation(animation)

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
/*
    private fun parseAzimuth(azimuth: Int) : String {
        when (azimuth) {
            azimuth > 350 || azimuth < 10 ->
        }
    }

 */
}
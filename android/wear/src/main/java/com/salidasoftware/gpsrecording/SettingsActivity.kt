package com.salidasoftware.gpsrecording

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : WearableActivity() {

    val store = GPSRecordingWearApplication.store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Enables Always-on
        // setAmbientEnabled()

        store.let {
            val df = GPSRecordingWearApplication.storeView.distanceFilterInMeters.get()
            if (df != null) {
                seekBarDistanceFilter.progress = (df / 10).toInt()  // Using a scale of 10 on the seek bar
            }

            val tf = GPSRecordingWearApplication.storeView.timeFilterInMilliseconds.get()
            if (tf != null) {
                seekBarTimeFilter.progress = (tf / 1000).toInt()
            }

            val metric = GPSRecordingWearApplication.storeView.displayMetricUnits.get() ?: false
            checkBoxMetricUnits.isChecked = metric

            GPSRecordingService.recording.get()?.let {
                if (it) {
                    seekBarDistanceFilter.isEnabled = false
                    seekBarTimeFilter.isEnabled = false
                }
            }
        }
        updateLabels()

        checkBoxMetricUnits.setOnCheckedChangeListener { buttonView, isChecked ->
            GPSRecordingWearApplication.storeView.displayMetricUnits.set(isChecked)
        }

        seekBarTimeFilter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                GPSRecordingWearApplication.storeView.timeFilterInMilliseconds.set(progress * 1000L)
                updateLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        seekBarDistanceFilter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                GPSRecordingWearApplication.storeView.distanceFilterInMeters.set(progress * 10f)
                updateLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    private fun updateLabels() {
        textViewDistanceFilterLabel.text = getString(R.string.distance_filter_meters) + " : " + ((GPSRecordingWearApplication.storeView.distanceFilterInMeters.get() ?: 10L))
        textViewTimeFilterLabel.text = getString(R.string.time_filter_seconds) + " : " + ((GPSRecordingWearApplication.storeView.timeFilterInMilliseconds.get() ?: 1000) / 1000)
    }


}

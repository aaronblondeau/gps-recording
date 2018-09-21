package com.salidasoftware.gpsrecording.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.SeekBar
import com.salidasoftware.gpsrecording.GPSRecordingApplication
import com.salidasoftware.gpsrecording.GPSRecordingService
import com.salidasoftware.gpsrecording.R
import com.salidasoftware.gpsrecording.room.GPSRecordingStore
import kotlinx.android.synthetic.main.activity_record.*
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_settings.*

class SettingsActivity : AppCompatActivity() {

    val store = GPSRecordingApplication.store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setSupportActionBar(toolbarSettings)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true) }

        store?.let {
            val df = GPSRecordingStore.distanceFilterInMeters.get()
            if (df != null) {
                seekBarDistanceFilter.progress = (df / 10).toInt()  // Using a scale of 10 on the seek bar
            }

            val tf = GPSRecordingStore.timeFilterInMilliseconds.get()
            if (tf != null) {
                seekBarTimeFilter.progress = (tf / 1000).toInt()
            }

            val metric = GPSRecordingStore.displayMetricUnits.get() ?: false
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
            GPSRecordingStore.displayMetricUnits.set(isChecked)
        }

        seekBarTimeFilter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                GPSRecordingStore.timeFilterInMilliseconds.set(progress * 1000L)
                updateLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        seekBarDistanceFilter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                GPSRecordingStore.distanceFilterInMeters.set(progress * 10f)
                updateLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    private fun updateLabels() {
        textViewDistanceFilterLabel.text = getString(R.string.distance_filter_meters) + " : " + ((GPSRecordingStore.distanceFilterInMeters.get() ?: 10L))
        textViewTimeFilterLabel.text = getString(R.string.time_filter_seconds) + " : " + ((GPSRecordingStore.timeFilterInMilliseconds.get() ?: 1000) / 1000)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
            = when(item?.itemId) {
        android.R.id.home -> navigateUpTo(Intent(this, MainActivity::class.java))
        else -> super.onOptionsItemSelected(item)
    }
}

package com.salidasoftware.gpsrecording.activities

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.salidasoftware.gpsrecording.*
import com.salidasoftware.gpsrecording.databinding.ActivityRecordBinding
import com.salidasoftware.gpsrecording.room.GPSRecordingStore
import com.salidasoftware.gpsrecording.view_models.RecordingViewModel
import com.salidasoftware.gpsrecording.view_models.TrackViewModel

import kotlinx.android.synthetic.main.activity_record.*
import kotlinx.android.synthetic.main.content_record.*
import org.jetbrains.anko.doAsync

class RecordActivity : AppCompatActivity() {

    private val GPS_REQUEST_CODE = 101

    val store = GPSRecordingApplication.store
    lateinit var binding : ActivityRecordBinding

    var currentTrackCallback : Observable.OnPropertyChangedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_record)

        binding  = DataBindingUtil.setContentView(this, R.layout.activity_record)
        val trackViewModel = ViewModelProviders.of(this).get(TrackViewModel::class.java)
        binding.track = trackViewModel

        val recordingViewModel = ViewModelProviders.of(this).get(RecordingViewModel::class.java)
        binding.recording = recordingViewModel

        setSupportActionBar(toolbarRecord)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true) }

        buttonStartRecording.setOnClickListener {
            startOrResumeRecording()
        }

        buttonResumeRecording.setOnClickListener {
            startOrResumeRecording()
        }

        buttonPauseRecording.setOnClickListener {
            // Stop service if running
            val recording =  if (GPSRecordingService.recording.get() != null) GPSRecordingService.recording.get()!! else false
            if(recording) {
                stopRecording()
            }
        }

        buttonFinishRecording.setOnClickListener {
            // Stop service if running
            val recording =  if (GPSRecordingService.recording.get() != null) GPSRecordingService.recording.get()!! else false
            if(recording) {
                stopRecording()
            }

            // Unset current track
            val currentTrackId = GPSRecordingStore.currentTrackId.get() ?: -1
            GPSRecordingStore.currentTrackId.set(-1)

            // Go to track activity
            val intent = Intent(this, TrackActivity::class.java)
            intent.putExtra(TrackActivity.TRACK_ID, currentTrackId)
            this.startActivity(intent)
        }


    }

    fun updateCurrentTrack() {
        val trackId = GPSRecordingStore.currentTrackId.get() ?: -1
        if (trackId >= 0 && store != null) {
            doAsync {
                val trk = store.trackDAO.getByIdLive(trackId)
                if (trk != null) {
                    Log.d("RecordActivity", "~~ HERE A")
                    binding.track?.setTrack(this@RecordActivity, trk)
                }
            }
        } else {
            Log.d("RecordActivity", "~~ HERE B")
            binding.track?.unsetTrack()
        }
    }

    override fun onResume() {
        super.onResume()
        currentTrackCallback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                this@RecordActivity.updateCurrentTrack()
            }
        }
        GPSRecordingStore.currentTrackId.addOnPropertyChangedCallback(currentTrackCallback!!)
        updateCurrentTrack()
    }

    override fun onPause() {
        currentTrackCallback?.let {
            GPSRecordingStore.currentTrackId.removeOnPropertyChangedCallback(it)
        }
        super.onPause()
    }

    private fun startOrResumeRecording() {
        // Request permissions if needed or start service
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // permission needed
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), GPS_REQUEST_CODE)
            } else {
                startRecording()
            }
        } else {
            startRecording()
        }
    }

    fun startRecording() {
        val intent = Intent(this.application, GPSRecordingService::class.java)
        startService(intent)
    }

    fun stopRecording() {
        val intent = Intent(this.application, GPSRecordingService::class.java)
        stopService(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GPS_REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // denied
            } else {
                // granted
                startRecording()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
            = when(item?.itemId) {
        android.R.id.home -> navigateUpTo(Intent(this, MainActivity::class.java))
        else -> super.onOptionsItemSelected(item)
    }

}

package com.salidasoftware.gpsrecording

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.databinding.Observable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_record.*
import kotlinx.android.synthetic.main.content_record.*

class RecordActivity : AppCompatActivity() {

    private val GPS_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        setSupportActionBar(toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true) }

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        buttonStartRecording.setOnClickListener {
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
            val currentTrackId = GPSRecordingStore.getCurrentTrackId(this)
            GPSRecordingStore.setCurrentTrackId(this, -1)

            // Go to track activity
            val intent = Intent(this, TrackActivity::class.java)
            intent.putExtra(TrackActivity.TRACK_ID, currentTrackId)
            this.startActivity(intent)
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

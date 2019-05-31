package com.salidasoftware.gpsrecording

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.salidasoftware.gpsrecording.databinding.ActivityRecordBinding
import com.salidasoftware.gpsrecording.room.Track
import kotlinx.android.synthetic.main.activity_record.*
import kotlinx.android.synthetic.main.activity_track.*
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timerTask

class RecordActivity : WearableActivity() {

    val store = GPSRecordingWearApplication.store
    private val GPS_REQUEST_CODE = 101
    lateinit var binding : ActivityRecordBinding

    var currentTrackCallback : Observable.OnPropertyChangedCallback? = null

    var trackObserver: Observer<Track>? = null
    var observedTrack: LiveData<Track>? = null

    val timer = Timer()
    var ticker: TimerTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_record)

        binding  = DataBindingUtil.setContentView(this, R.layout.activity_record)
        val trackViewModel = TrackViewModel()
        binding.track = trackViewModel

        textViewRecordCurrentTime.visibility = View.GONE
        textViewRecordCurrentTime.text = ""

        val recordingViewModel = RecordingViewModel()
        binding.recording = recordingViewModel

        // Enables Always-on
        setAmbientEnabled()

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
            val currentTrackId = GPSRecordingWearApplication.storeView.currentTrackId.get() ?: -1
            GPSRecordingWearApplication.storeView.currentTrackId.set(-1)

            // Go to track activity
            val intent = Intent(this, TrackActivity::class.java)
            intent.putExtra(TrackActivity.TRACK_ID, currentTrackId)
            this.startActivity(intent)
        }
    }

    fun updateCurrentTrack() {
        val trackId = GPSRecordingWearApplication.storeView.currentTrackId.get() ?: -1
        if (trackId >= 0) {
            doAsync {
                val trk = store.trackDAO.getByIdLive(trackId)

                uiThread {

                    trackObserver?.let {
                        if (observedTrack != null) {
                            observedTrack?.removeObserver(it)
                        }
                    }

                    trackObserver = Observer<Track> {
                        binding.track?.setTrack(it)
                    }
                    observedTrack = trk
                    trackObserver?.let {
                        trk.observeForever(it)
                    }
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        startUpdates()
        startClock()
    }

    fun startUpdates() {
        currentTrackCallback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                this@RecordActivity.updateCurrentTrack()
            }
        }
        GPSRecordingWearApplication.storeView.currentTrackId.addOnPropertyChangedCallback(currentTrackCallback!!)
        updateCurrentTrack()
        binding.recording?.watch()
    }

    fun stopUpdates() {
        currentTrackCallback?.let {
            GPSRecordingWearApplication.storeView.currentTrackId.removeOnPropertyChangedCallback(it)
        }
        trackObserver?.let {
            if (observedTrack != null) {
                observedTrack?.removeObserver(it)
            }
        }
        binding.recording?.unwatch()
    }

    override fun onPause() {
        stopUpdates()
        stopClock()

        super.onPause()
    }

    fun startClock() {
        runOnUiThread {
            textViewRecordCurrentTime.visibility = View.VISIBLE
        }
        ticker = timerTask{
            runOnUiThread {
                textViewRecordCurrentTime.text = SimpleDateFormat("h:mm:ss a").format(Date())
            }
        }
        timer.scheduleAtFixedRate(ticker, 0L, 1000L)
    }

    fun stopClock() {
        ticker?.let {
            it.cancel()
            ticker = null
        }
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

    override fun onEnterAmbient(ambientDetails: Bundle?) {
        super.onEnterAmbient(ambientDetails)
        stopUpdates()
    }

    override fun onExitAmbient() {
        super.onExitAmbient()
        startUpdates()
    }

    override fun onUpdateAmbient() {
        super.onUpdateAmbient()

        val trackId = GPSRecordingWearApplication.storeView.currentTrackId.get() ?: -1
        if (trackId >= 0) {
            doAsync {
                val trk = store.trackDAO.getById(trackId)
                trk?.let {
                    binding.track?.setTrack(trk)
                }
            }
        }
        binding.recording?.updateHasCurrentTrack()
        binding.recording?.updateRecording()
    }
}

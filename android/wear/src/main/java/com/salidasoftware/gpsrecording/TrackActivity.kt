package com.salidasoftware.gpsrecording

import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.salidasoftware.gpsrecording.databinding.ActivityTrackBinding
import com.salidasoftware.gpsrecording.room.Track
import kotlinx.android.synthetic.main.activity_track.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.lang.Exception

class TrackActivity : WearableActivity() {

    val store = GPSRecordingWearApplication.store
    lateinit var track : LiveData<Track>

    lateinit var binding : ActivityTrackBinding
    lateinit var trackObserver: Observer<Track>

    companion object {
        const val TRACK_ID = "track_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_track)

        binding  = DataBindingUtil.setContentView(this, R.layout.activity_track)
        val trackViewModel = TrackViewModel()
        binding.track = trackViewModel

        // Enables Always-on
        // setAmbientEnabled()

        buttonDeleteTrack.setOnClickListener {
            doAsync {
                track.value?.let {
                    store?.deleteTrack(it)
                    this@TrackActivity.finish()
                }
            }
        }

        val trackId = intent.getLongExtra(TRACK_ID, -1)
        buttonTrackMap.setOnClickListener {
            // Go to track activity
            val intent = Intent(this, TrackMapActivity::class.java)
            intent.putExtra(TrackMapActivity.TRACK_ID, trackId)
            this.startActivity(intent)
        }

        trackObserver = Observer<Track> {
            trackViewModel.setTrack(it)
            if (it != null) {
                buttonDeleteTrack.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()

        buttonDeleteTrack.isEnabled = false
        val trackId = intent.getLongExtra(TRACK_ID, -1)
        Log.d("TrackActivity", "~~ Got Track Id " + trackId)
        if (trackId >= 0 && store != null) {
            doAsync {
                try {
                    track = store.trackDAO.getByIdLive(trackId)
                    uiThread {
                        track.observeForever(trackObserver)
                    }
                }
                catch (e: Throwable) {
                    Log.d("TrackActivity", "~~ doAsync Error", e)
                }
            }
        } else {
            this.finish()
        }
    }

    override fun onPause() {
        super.onPause()
        track.removeObserver(trackObserver)
    }
}

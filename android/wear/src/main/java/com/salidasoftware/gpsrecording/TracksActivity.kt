package com.salidasoftware.gpsrecording

import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.salidasoftware.gpsrecording.ui.OnTrackClickListener
import com.salidasoftware.gpsrecording.room.Track
import kotlinx.android.synthetic.main.activity_track.*
import kotlinx.android.synthetic.main.activity_tracks.*
import org.jetbrains.anko.doAsync

class TracksActivity : WearableActivity(), OnTrackClickListener {

    lateinit var adapter: TracksRecyclerViewAdapter
    lateinit var tracksObserver: Observer<List<Track>>
    lateinit var liveTracks : LiveData<List<Track>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracks)

        // Enables Always-on
        // setAmbientEnabled()

        adapter = TracksRecyclerViewAdapter(arrayOf<Track>())

        recyclerViewTracks.layoutManager = LinearLayoutManager(this)
        recyclerViewTracks.adapter = adapter
        adapter.setOnTrackClickListener(this)

        liveTracks = GPSRecordingWearApplication.store.trackDAO.getAllLive()

        tracksObserver = Observer<List<Track>> {tracks ->
            Log.d("TracksActivity", "~~ Got " +tracks.size+ " tracks")
            adapter.setTracks(tracks.toTypedArray())
            adapter.notifyDataSetChanged()
            if (tracks.isEmpty()) {
                textViewNoTracksFound.visibility = View.VISIBLE
            } else {
                textViewNoTracksFound.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        liveTracks.observeForever(tracksObserver)
    }

    override fun onPause() {
        super.onPause()
        liveTracks.removeObserver(tracksObserver)
    }

    override fun onTrackClick(track: Track) {
        val intent = Intent(this, TrackActivity::class.java)
        intent.putExtra(TrackActivity.TRACK_ID, track.id)
        this.startActivity(intent)
    }
}

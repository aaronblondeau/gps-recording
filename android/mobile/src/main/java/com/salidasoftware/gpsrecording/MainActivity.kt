package com.salidasoftware.gpsrecording

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync

class MainActivity : AppCompatActivity(), OnTrackClickListener {

    val store = GPSRecordingApplication.store

    private lateinit var viewModel: TrackViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationViewMain.setOnNavigationItemSelectedListener {
            if (it.itemId == R.id.home_menu_record) {
                val intent = Intent(this, RecordActivity::class.java)
                this.startActivity(intent)
            } else {
                Log.d("MainActivity", "~~ TODO - launch settings activity")

                if (store != null) {

                    doAsync {
                        val track = store.createTrack("Track " + System.currentTimeMillis(), "Test", Track.Activity.BIKE.name)
                        Log.d("MainActivity", "~~ Created track " + track.name)
                    }
                }
            }
            true
        }

        // https://code.tutsplus.com/tutorials/android-architecture-components-using-the-paging-library-with-room--cms-31535

        viewModel = ViewModelProviders.of(this).get(TrackViewModel::class.java)

        val adapter = TracksRecyclerViewAdapter(this)
        recyclerViewMain.layoutManager = LinearLayoutManager(this)
        recyclerViewMain.adapter = adapter
        adapter.setOnTrackClickListener(this)

        viewModel.getTracks().observe(this, Observer { tracks ->
            if(tracks != null) adapter.submitList(tracks)
        })
    }

    override fun onTrackClick(track: Track) {
        Log.d("MainActivity", "~~ Clicked track " + track.name)

        val intent = Intent(this, TrackActivity::class.java)
        intent.putExtra(TrackActivity.TRACK_ID, track.id)
        this.startActivity(intent)
    }

}

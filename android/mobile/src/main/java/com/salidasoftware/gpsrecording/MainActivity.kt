package com.salidasoftware.gpsrecording

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync

class MainActivity : AppCompatActivity(), View.OnLongClickListener {

    val store = GPSRecordingApplication.store

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

        val recyclerViewAdapter = TracksRecyclerViewAdapter(listOf<Track>(), this)
        recyclerViewMain.layoutManager = LinearLayoutManager(this)
        recyclerViewMain.adapter = recyclerViewAdapter

        val viewModel = ViewModelProviders.of(this).get(TrackViewModel::class.java)
        viewModel.getTracks().observe(this, Observer {

            // TODO - is there a better way to do this than wholesale replace with resetTracks?

            if (it != null) {
                Log.d("MainActivity", "~~ I see " + it.size + " tracks!")
                recyclerViewAdapter.resetTracks(it)
            } else {
                Log.d("MainActivity", "~~ I see 0 (null) tracks!")
                recyclerViewAdapter.resetTracks(listOf<Track>())
            }
        })

    }

    override fun onLongClick(v: View?): Boolean {

        // TODO - use an action sheet here

        Log.d("MainActivity", "~~ onLongClick")
        if (v != null && store != null) {
            val track = v.getTag() as Track
            Log.d("MainActivity", "~~ Long clicked track " + track.name)
            doAsync {
                store.deleteTrack(track)
            }
        }
        return true
    }

}

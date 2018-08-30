package com.salidasoftware.gpsrecording

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_track.*
import kotlinx.android.synthetic.main.content_track.*
import org.jetbrains.anko.doAsync

class TrackActivity : AppCompatActivity() {

    val store = GPSRecordingApplication.store
    lateinit var track : LiveData<Track>

    companion object {
        const val TRACK_ID = "track_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track)
        setSupportActionBar(toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true) }

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", {
                        Log.d("TrackActivity", "~~ snack action!")
                        doAsync {
                            Log.d("TrackActivity", "~~ HERE A")
                            track.value?.let {
                                Log.d("TrackActivity", "~~ HERE B")
                                it.name = it.name + "X"
                                store?.saveTrack(it)
                            }
                        }
                    }).show()
        }

        val trackId = intent.getLongExtra(TRACK_ID, -1)
        if (trackId >= 0 && store != null) {
            doAsync {
                track = store.trackDAO.getByIdLive(trackId)
                track.observe(this@TrackActivity, Observer {
                    Log.d("TrackActivity", "~~ Track LiveData changed!")
                    it?.let {
                        trackDetailName.text = it.name
                    }
                })
            }
        } else {
            this.finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
            = when(item?.itemId) {
        android.R.id.home -> navigateUpTo(Intent(this, MainActivity::class.java))
        else -> super.onOptionsItemSelected(item)
    }

}

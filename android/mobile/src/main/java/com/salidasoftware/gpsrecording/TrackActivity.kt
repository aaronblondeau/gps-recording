package com.salidasoftware.gpsrecording

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.salidasoftware.gpsrecording.databinding.ActivityTrackBinding
import com.salidasoftware.gpsrecording.databinding.ContentTrackBinding

import kotlinx.android.synthetic.main.activity_track.*
import kotlinx.android.synthetic.main.content_track.*
import org.jetbrains.anko.doAsync

class TrackActivity : AppCompatActivity() {

    val store = GPSRecordingApplication.store
    lateinit var track : LiveData<Track>

    lateinit var binding : ActivityTrackBinding

    companion object {
        const val TRACK_ID = "track_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //setContentView(R.layout.activity_track)

        // Setup data bindings
        binding  = DataBindingUtil.setContentView(this, R.layout.activity_track)
        val trackViewModel = ViewModelProviders.of(this).get(TrackViewModel::class.java)
        binding.track = trackViewModel

        // Back button goes back to main activity
        setSupportActionBar(toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true) }

        emailTrackFAB.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", {
                        Log.d("TrackActivity", "~~ snack action!")
                        doAsync {
                            track.value?.let {
                                it.name = it.name + "X"
                                store?.saveTrack(it)
                            }
                        }
                    }).show()
        }

        deleteTrackFAB.setOnClickListener { view ->
            Snackbar.make(view, "Really delete this track?", Snackbar.LENGTH_LONG)
                    .setAction("Delete", {
                        doAsync {
                            track.value?.let {
                                store?.deleteTrack(it)
                                this@TrackActivity.finish()
                            }
                        }
                    })
                    .setActionTextColor(getResources().getColor(R.color.colorDanger))
                    .show()
        }

        // Fetch the track and add it to the view model
        val trackId = intent.getLongExtra(TRACK_ID, -1)
        if (trackId >= 0 && store != null) {
            doAsync {
                track = store.trackDAO.getByIdLive(trackId)
                trackViewModel.setTrack(this@TrackActivity, track)
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

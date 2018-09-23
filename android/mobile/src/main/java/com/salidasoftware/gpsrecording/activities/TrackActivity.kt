package com.salidasoftware.gpsrecording.activities

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import com.salidasoftware.gpsrecording.GPSRecordingApplication
import com.salidasoftware.gpsrecording.R
import com.salidasoftware.gpsrecording.room.Track
import com.salidasoftware.gpsrecording.view_models.TrackViewModel
import com.salidasoftware.gpsrecording.databinding.ActivityTrackBinding
import kotlinx.android.synthetic.main.activity_track.*
import kotlinx.android.synthetic.main.content_track.*
import org.jetbrains.anko.*
import java.io.IOException

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
        setSupportActionBar(toolbarTrack)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true) }

        emailTrackFAB.setOnClickListener { view ->

            val outputs = listOf("GPX File", "GeoJSON File")
            selector("Share Track", outputs, { i ->
                track.value?.let {
                    if (i == 0) {
                        doAsync {
                            try {
                                store?.emailGpx(it, this@TrackActivity)
                            } catch(e: IOException) {
                                Log.e("Track Activity", "Failed to send GPX", e)
                            }
                        }
                    } else {
                        doAsync {
                            try {
                                store?.emailGeoJson(it, this@TrackActivity)
                            } catch(e: IOException) {
                                Log.e("Track Activity", "Failed to send GeoJSON", e)
                            }
                        }
                    }
                }
            })

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
        buttonSaveTrack.isEnabled = false
        val trackId = intent.getLongExtra(TRACK_ID, -1)
        if (trackId >= 0 && store != null) {
            doAsync {
                track = store.trackDAO.getByIdLive(trackId)
                trackViewModel.setTrack(this@TrackActivity, track)
                buttonSaveTrack.isEnabled = true
            }
        } else {
            this.finish()
        }

        buttonSaveTrack.setOnClickListener {
            Log.d("TrackActivity", "~~ " + trackDetailName.text.toString() + " | " + trackDetailNote.text.toString())
            buttonSaveTrack.isEnabled = false
            doAsync {
                store?.let { store ->
                    this@TrackActivity.track.value?.let {

                        var activity = ""
                        if (radioButtonBike.isChecked) {
                            activity = Track.Activity.BIKE.activityName
                        }
                        if (radioButtonRun.isChecked) {
                            activity = Track.Activity.RUN.activityName
                        }
                        if (radioButtonSki.isChecked) {
                            activity = Track.Activity.SKI.activityName
                        }
                        if (radioButtonHike.isChecked) {
                            activity = Track.Activity.HIKE.activityName
                        }
                        if (radioButtonWalk.isChecked) {
                            activity = Track.Activity.WALK.activityName
                        }

                        store.updateTrack(it, trackDetailName.text.toString(), trackDetailNote.text.toString(), activity)
                        uiThread {
                            toast(getString(R.string.track_saved))
                        }
                    }
                }
                uiThread {
                    buttonSaveTrack.isEnabled = true
                }
            }
            trackDetailName.onEditorAction(EditorInfo.IME_ACTION_DONE)
            trackDetailNote.onEditorAction(EditorInfo.IME_ACTION_DONE)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
            = when(item?.itemId) {
        android.R.id.home -> navigateUpTo(Intent(this, MainActivity::class.java))
        else -> super.onOptionsItemSelected(item)
    }

}

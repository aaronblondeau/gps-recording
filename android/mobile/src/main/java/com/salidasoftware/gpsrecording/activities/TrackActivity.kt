package com.salidasoftware.gpsrecording.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Intent
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import com.salidasoftware.gpsrecording.GPSRecordingApplication
import com.salidasoftware.gpsrecording.R
import com.salidasoftware.gpsrecording.room.Track
import com.salidasoftware.gpsrecording.view_models.TrackViewModel
import com.salidasoftware.gpsrecording.databinding.ActivityTrackBinding
import kotlinx.android.synthetic.main.activity_track.*
import kotlinx.android.synthetic.main.content_track.*
import org.jetbrains.anko.*
import java.io.IOException
import java.lang.Exception

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
                            track.value?.let {track ->
                                store?.deleteTrack(track)

                                // If track was from wear, let it know if deleted
                                if(!track.upstreamId.isEmpty()) {

                                    val nodeClient: NodeClient = Wearable.getNodeClient(this@TrackActivity)
                                    nodeClient.localNode.addOnCompleteListener {
                                        it.result?.let { node ->

                                            val wearNodeId = track.upstreamId.split(":")[0]
                                            val wearId = (track.upstreamId.split(":")[1]).toLong()

                                            val dataItemPath = "/track_downstream/" + wearNodeId + "/" + wearId

                                            val putDataReq: PutDataRequest = PutDataMapRequest.create(dataItemPath).run {
                                                dataMap.putString("mobileNodeId", node.id)
                                                dataMap.putString("wearNodeId", wearNodeId)
                                                dataMap.putLong("mobileId", -1)  // Use a negative value to indicate deletion
                                                dataMap.putLong("wearId", wearId)
                                                dataMap.putLong("forceUpdate", System.currentTimeMillis())
                                                asPutDataRequest()
                                            }

                                            val mDataClient: DataClient = Wearable.getDataClient(this@TrackActivity)
                                            val putDataTask: Task<DataItem> = mDataClient.putDataItem(putDataReq)

                                            putDataTask.addOnSuccessListener {
                                                Log.d("TrackActivity", "~~ sent track delete notice on path " + dataItemPath)
                                            }

                                            putDataTask.addOnFailureListener {
                                                Log.e("TrackActivity", "~~ failed to send track delete notice on path " + dataItemPath, it)
                                            }
                                        }
                                    }
                                }
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
                try {
                    track = store.trackDAO.getByIdLive(trackId)
                    uiThread {
                        trackViewModel.setTrack(this@TrackActivity, track)
                        buttonSaveTrack.isEnabled = true
                        track.observe(this@TrackActivity, Observer { track ->
                            if (track != null) {
                                setTitle(track.name)
                            }
                        })
                    }
                }
                catch (e: Exception) {
                    Log.d("TrackActivity", "~~ doAsync Error", e)
                }
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

        buttonOpenTrackMap.setOnClickListener{
            // Go to track activity
            val intent = Intent(this, TrackMapActivity::class.java)
            intent.putExtra(TrackMapActivity.TRACK_ID, trackId)
            this.startActivity(intent)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
            = when(item?.itemId) {
        android.R.id.home -> navigateUpTo(Intent(this, MainActivity::class.java))
        else -> super.onOptionsItemSelected(item)
    }

}

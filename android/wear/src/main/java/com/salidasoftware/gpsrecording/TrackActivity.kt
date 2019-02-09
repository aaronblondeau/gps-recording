package com.salidasoftware.gpsrecording

import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.salidasoftware.gpsrecording.databinding.ActivityTrackBinding
import com.salidasoftware.gpsrecording.room.Track
import kotlinx.android.synthetic.main.activity_track.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
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
                    store.deleteTrack(it)
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

        val mDataClient: DataClient = Wearable.getDataClient(this)
        val nodeClient: NodeClient = Wearable.getNodeClient(this)
        buttonSyncTrack.setOnClickListener {

            track.value?.let { track ->

                nodeClient.localNode.addOnCompleteListener {

                    doAsync {

                        try {

                            it.result?.let {
                                val wearNodeId = it.id

                                Log.d("TrackActivity", "~~ node id is " + wearNodeId)

                                val dataItemPath = "/track_upstream/" + wearNodeId + "/" + track.id

                                val putDataReq: PutDataRequest = PutDataMapRequest.create(dataItemPath).run {
                                    dataMap.putString("wearNodeId", wearNodeId)
                                    dataMap.putLong("wearId", track.id)
                                    dataMap.putAsset("track", store.trackAsAsset(track))
                                    dataMap.putLong("forceUpdate", System.currentTimeMillis())
                                    asPutDataRequest()
                                }
                                val putDataTask: Task<DataItem> = mDataClient.putDataItem(putDataReq)

                                putDataTask.addOnSuccessListener {
                                    Log.d("TrackActivity", "~~ track sent on path " + dataItemPath)

                                    // Save "pending" as downstreamId to prevent re-syncs
                                    doAsync {
                                        track.downstreamId = "pending"
                                        store.saveTrack(track)
                                    }
                                }

                                putDataTask.addOnFailureListener {
                                    Log.e("TrackActivity", "~~ failed to send track on path " + dataItemPath, it)
                                }


                            }

                        } catch(e: Exception) {
                            Log.e("TrackActivity", "~~ error starting track sync", e)
                        }
                    }
                }
            }
        }

        buttonOpenTrackOnPhone.setOnClickListener {
            track.value?.let { track ->

                // Check if downstreamId empty or "pending"
                if(track.downstreamId.isEmpty() || track.downstreamId.equals("pending")) {
                    toast(getString(R.string.track_not_synced))
                } else {

                    // downstream Ids are stored as nodeId:remoteTrackId
                    // val downstreamNodeId = track.downstreamId.split(":")[0]
                    // val downstreamTrackId = track.downstreamId.split(":")[1]

                    buttonOpenTrackOnPhone.isEnabled = false
                    doAsync {

                        try {

                            val capabilityInfo: CapabilityInfo = Tasks.await(
                                    Wearable.getCapabilityClient(this@TrackActivity)
                                            .getCapability(
                                                    "open_downstream_track",
                                                    CapabilityClient.FILTER_REACHABLE
                                            )
                            )
                            for (node in capabilityInfo.nodes) {
                                if (node.isNearby) {
                                    node.id?.also { nodeId ->
                                        Wearable.getMessageClient(this@TrackActivity).sendMessage(
                                                nodeId,
                                                "/open_downstream_track/" + track.downstreamId,
                                                (track.downstreamId).toByteArray()
                                        ).apply {
                                            addOnSuccessListener {
                                                Log.d("TrackActivity", "~~ track downstream open sent to node " + nodeId)
                                            }
                                            addOnFailureListener {
                                                Log.e("TrackActivity", "~~ failed to send track downstream open to node " + nodeId, it)
                                            }
                                        }
                                    }
                                }
                            }

                            uiThread {
                                toast(getString(R.string.track_opened_on_phone))
                                buttonOpenTrackOnPhone.isEnabled = true
                            }

                        } catch (e: Exception) {
                            Log.d("TrackActivity", "~~ failed to send track downstream open message", e)
                        }

                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()

        buttonDeleteTrack.isEnabled = false
        val trackId = intent.getLongExtra(TRACK_ID, -1)
        Log.d("TrackActivity", "~~ Got Track Id " + trackId)
        if (trackId >= 0) {
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

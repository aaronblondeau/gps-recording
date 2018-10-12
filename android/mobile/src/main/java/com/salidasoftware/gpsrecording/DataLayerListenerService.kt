package com.salidasoftware.gpsrecording

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.JsonReader
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.salidasoftware.gpsrecording.activities.TrackActivity
import java.lang.Exception
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.InputStream
import org.json.JSONObject

class DataLayerListenerService : WearableListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("Mobile:DLLS", "~~ onCreate")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer?) {
        super.onDataChanged(dataEvents)

        if(dataEvents != null) {
            for (event in dataEvents) {

                Log.d("Mobile:DLLS", "~~ Processing data event with path " + event.getDataItem().getUri().getPath())

                val data = DataMapItem.fromDataItem(event.dataItem)

                if (event.dataItem.uri.path.startsWith("/track_upstream")) {
                    val wearNodeId = data.dataMap.getString("wearNodeId")
                    val wearId = data.dataMap.getLong("wearId")
                    Log.d("Mobile:DLLS", "~~ Got track from wear: wearNodeId=" + wearNodeId + " id="+wearId)

                    val trackAsset = data.dataMap.getAsset("track")
                    Log.d("Mobile:DLLS", "~~ Asset : " + trackAsset.data)

                    val assetInputStream: InputStream? = Tasks.await(Wearable.getDataClient(this@DataLayerListenerService).getFdForAsset(trackAsset))?.inputStream
                    val assetAsString = assetInputStream?.bufferedReader().use { it?.readText() }
                    val assetAsJson = JSONObject(assetAsString)

                    doAsync {
                        try {

                            // Use existing if we've already synced this track
                            val track = GPSRecordingApplication.store.trackDAO.getByUpstreamId(wearNodeId + ":" + wearId) ?: GPSRecordingApplication.store.createTrackFromUpstreamAsset(assetAsJson, wearNodeId)

                            Log.d("Mobile:DLLS", "~~ Created track from wear: localId=" + track.id + " upstreamId=" + track.upstreamId)

                            // Send our local id back to wear so it can store it as downstreamId

                            val dataItemPath = "/track_downstream/" + wearNodeId + "/" + wearId

                            val nodeClient: NodeClient = Wearable.getNodeClient(this@DataLayerListenerService)
                            nodeClient.localNode.addOnCompleteListener {

                                it.result?.let { node ->

                                    val putDataReq: PutDataRequest = PutDataMapRequest.create(dataItemPath).run {
                                        dataMap.putString("mobileNodeId", node.id)
                                        dataMap.putString("wearNodeId", wearNodeId)
                                        dataMap.putLong("mobileId", track.id)
                                        dataMap.putLong("wearId", wearId)
                                        dataMap.putLong("forceUpdate", System.currentTimeMillis())
                                        asPutDataRequest()
                                    }

                                    val mDataClient: DataClient = Wearable.getDataClient(this@DataLayerListenerService)
                                    val putDataTask: Task<DataItem> = mDataClient.putDataItem(putDataReq)

                                }

                            }
                        } catch(e: Exception) {
                            Log.e("Mobile:DLLS", "~~ Failed to create track from wear", e)
                        }

                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("Mobile:DLLS", "~~ Got a message on path "+messageEvent?.path)

        if(messageEvent.path.startsWith("/open_downstream_track")) {
            try {
                val mobileTrackId = String(messageEvent.data).split(":")[1].toLong()
                val mobileNodeId = String(messageEvent.data).split(":")[0]
                Log.d("Mobile:DLLS", "~~ Would open track with id " + mobileTrackId + " for node " + mobileNodeId)

                doAsync {
                    GPSRecordingApplication.store.trackDAO.getById(mobileTrackId)?.let {track ->

                        val nodeClient: NodeClient = Wearable.getNodeClient(this@DataLayerListenerService)
                        nodeClient.localNode.addOnCompleteListener {

                            it.result?.let { node ->

                                if(node.id == mobileNodeId) {

                                    uiThread {
                                        // Go to track activity
                                        val intent = Intent(this@DataLayerListenerService, TrackActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        intent.putExtra(TrackActivity.TRACK_ID, track.id)
                                        this@DataLayerListenerService.startActivity(intent)
                                    }

                                }

                            }
                        }

                    }
                }

            } catch(e: Exception) {
                Log.d("Mobile:DLLS", "~~ Failed to process open downstream track message", e)
            }
        }
    }
}

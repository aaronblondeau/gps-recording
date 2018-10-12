package com.salidasoftware.gpsrecording

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import java.lang.Exception
import com.google.android.gms.wearable.Asset
import org.jetbrains.anko.doAsync


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
                    val nodeId = data.dataMap.getString("nodeId")
                    val trackAsset = data.dataMap.getAsset("track")
                    val id = data.dataMap.getLong("id")
                    Log.d("Mobile:DLLS", "~~ Got track from wear: nodeId=" + nodeId + " id="+id)

                    doAsync {
                        val track = GPSRecordingApplication.store.createTrackFromUpstreamAsset(trackAsset, nodeId)

                        // Send our local id back to wear so it can store it as downstreamId

                        val dataItemPath = "/track_downstream/" + nodeId + "/" + id

                        val putDataReq: PutDataRequest = PutDataMapRequest.create(dataItemPath).run {
                            dataMap.putLong("downstreamId", track.id)
                            dataMap.putString("nodeId", nodeId)
                            dataMap.putLong("id", id)
                            dataMap.putLong("forceUpdate", System.currentTimeMillis())
                            asPutDataRequest()
                        }

                        val mDataClient: DataClient = Wearable.getDataClient(this@DataLayerListenerService)
                        val putDataTask: Task<DataItem> = mDataClient.putDataItem(putDataReq)

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
                val trackId = String(messageEvent.data).toLong()
                Log.d("Mobile:DLLS", "~~ Would open track with id " + trackId)
            } catch(e: Exception) {
                Log.d("Mobile:DLLS", "~~ Failed to process open downstream track message", e)
            }
        }
    }
}

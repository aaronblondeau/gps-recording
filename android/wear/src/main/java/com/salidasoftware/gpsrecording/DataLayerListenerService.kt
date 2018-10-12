package com.salidasoftware.gpsrecording

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import org.jetbrains.anko.doAsync

class DataLayerListenerService : WearableListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("Wear:DLLS", "~~ onCreate")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer?) {
        super.onDataChanged(dataEvents)

        if (dataEvents != null) {
            for (event in dataEvents) {

                Log.d("Wear:DLLS", "~~ Processing data event with path " + event.getDataItem().getUri().getPath())

                val data = DataMapItem.fromDataItem(event.dataItem)

                if (event.dataItem.uri.path.startsWith("/track_downstream")) {
                    val downstreamId = data.dataMap.getLong("downstreamId")
                    val dataNodeId = data.dataMap.getString("nodeId")

                    val nodeClient: NodeClient = Wearable.getNodeClient(this)

                    nodeClient.localNode.addOnCompleteListener {

                        it.result?.let {
                            val localNodeId = it.id

                            // Make sure node id matches
                            if (dataNodeId == localNodeId) {
                                val id = data.dataMap.getLong("id")
                                Log.d("Wear:DLLS", "~~ Got sync response from phone.  id=" + id + " nodeId=" + dataNodeId + " downstreamId=" + downstreamId)

                                doAsync {
                                    val store = GPSRecordingWearApplication.store
                                    val track = store.trackDAO.getById(id)
                                    track?.let {track ->
                                        track.downstreamId = dataNodeId + ":" + downstreamId
                                        store.saveTrack(track)
                                        Log.d("Wear:DLLS", "~~ Saved sync response downstream id : " + track.downstreamId)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
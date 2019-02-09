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

                event.dataItem.uri.path.let {
                    if (it != null) {
                        if (it.startsWith("/track_downstream")) {
                            val wearNodeId = data.dataMap.getString("wearNodeId")
                            val mobileNodeId = data.dataMap.getString("mobileNodeId")
                            val mobileId = data.dataMap.getLong("mobileId")
                            val wearId = data.dataMap.getLong("wearId")

                            Log.d("Wear:DLLS", "~~ Got sync response from phone.  id=" + wearId + " mobileNodeId=" + mobileNodeId + " mobileId=" + mobileId)

                            val nodeClient: NodeClient = Wearable.getNodeClient(this)

                            nodeClient.localNode.addOnCompleteListener {

                                it.result?.let {
                                    val localNodeId = it.id

                                    // Make sure node id matches
                                    if (wearNodeId == localNodeId) {

                                        doAsync {
                                            try {

                                                val store = GPSRecordingWearApplication.store
                                                val track = store.trackDAO.getById(wearId)
                                                track?.let { existingTrack ->
                                                    if (mobileId < 0) {
                                                        // Track was deleted on downstream device
                                                        existingTrack.downstreamId = ""
                                                    } else {
                                                        existingTrack.downstreamId = mobileNodeId + ":" + mobileId
                                                    }
                                                    store.saveTrack(existingTrack)
                                                    Log.d("Wear:DLLS", "~~ Saved sync response downstream id : " + existingTrack.downstreamId)
                                                }

                                            } catch (e: Exception) {
                                                Log.e("Wear:DLLS", "~~ Failed to handle a sync response", e)
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
    }
}

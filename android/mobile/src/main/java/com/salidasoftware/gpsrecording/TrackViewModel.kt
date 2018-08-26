package com.salidasoftware.gpsrecording

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.os.AsyncTask
import org.jetbrains.anko.doAsync

class TrackViewModel(application: Application) : AndroidViewModel(application) {
    val store = GPSRecordingApplication.getStore(GPSRecordingApplication.getDatabase(application))
    val items = store.trackDAO.getAllLive()

    fun getTracks() : LiveData<List<Track>> {
        return items
    }

    fun deleteTrack(track: Track) {
        doAsync {
            store.deleteTrack(track)
        }
    }
}
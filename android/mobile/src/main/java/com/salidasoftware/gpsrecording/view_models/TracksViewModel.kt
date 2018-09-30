package com.salidasoftware.gpsrecording.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.salidasoftware.gpsrecording.GPSRecordingApplication
import com.salidasoftware.gpsrecording.room.Track
import org.jetbrains.anko.doAsync

class TracksViewModel(application: Application) : AndroidViewModel(application) {
    val store = GPSRecordingApplication.getStore(GPSRecordingApplication.getDatabase(application))
    var items : LiveData<PagedList<Track>>

    init {
        val factory : DataSource.Factory<Int, Track> = store.trackDAO.getAllPaged()
        val pagedListBuilder: LivePagedListBuilder<Int, Track> =  LivePagedListBuilder<Int, Track>(factory, 50)
        items = pagedListBuilder.build()
    }

    fun getTracks() : LiveData<PagedList<Track>> {
        return items
    }

    fun deleteTrack(track: Track) {
        doAsync {
            store.deleteTrack(track)
        }
    }
}
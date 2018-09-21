package com.salidasoftware.gpsrecording.view_models

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import android.arch.lifecycle.Observer
import android.databinding.ObservableField
import com.salidasoftware.gpsrecording.room.GPSRecordingStore
import com.salidasoftware.gpsrecording.room.Track

class TrackViewModel() : ViewModel() {

    // https://developer.android.com/reference/android/databinding/ObservableField
    val name : ObservableField<String> = ObservableField("")
    val distance : ObservableField<String> = ObservableField("")
    val duration : ObservableField<String> = ObservableField("")

    var observer: Observer<Track>? = null
    var observed: LiveData<Track>? = null

    fun unsetTrack() {
        unObserve()
        observed = null
        name.set("")
        distance.set("")
        duration.set("")
    }

    fun setTrack(owner: LifecycleOwner, track: LiveData<Track>) {

        unObserve()

        observed = track
        observer = Observer {
            Log.d("TrackActivity", "~~ Track LiveData changed!")
            if (it == null) {
                name.set("")
                distance.set("")
                duration.set("")
                unObserve()
            } else {
                name.set(it.name)
                distance.set(track.value?.formattedDistance(GPSRecordingStore.displayMetricUnits.get() ?: false))
                duration.set(track.value?.formattedDuration())
            }
        }
        track.observe(owner, observer!!)
    }

    override fun onCleared() {
        unObserve()
        super.onCleared()
    }

    private fun unObserve() {
        // Remove old observer if there is one
        observer?.let { observer ->
            observed?.let { observed ->
                observed.removeObserver(observer)
            }
        }
    }

}
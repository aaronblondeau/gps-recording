package com.salidasoftware.gpsrecording.view_models

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import android.arch.lifecycle.Observer
import android.databinding.ObservableField
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
                distance.set("%.2f".format(it.totalDistanceInMeters / 1609.34) + " miles")
                val seconds = (it.totalDurationInMilliseconds / 1000) % 60
                val minutes = (it.totalDurationInMilliseconds / (1000 * 60) % 60)
                val hours = (it.totalDurationInMilliseconds / (1000 * 60 * 60) % 24)
                val dur = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                duration.set(dur)
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
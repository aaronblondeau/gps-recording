package com.salidasoftware.gpsrecording

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import android.arch.lifecycle.Observer
import android.databinding.ObservableField

class TrackViewModel() : ViewModel() {

    // https://developer.android.com/reference/android/databinding/ObservableField
    val name : ObservableField<String> = ObservableField("")
    val distance : ObservableField<String> = ObservableField("")

    var observer: Observer<Track>? = null
    var observed: LiveData<Track>? = null

    fun setTrack(owner: LifecycleOwner, track: LiveData<Track>) {

        unObserve()

        observed = track
        observer = Observer {
            Log.d("TrackActivity", "~~ Track LiveData changed!")
            it?.let {
                name.set(it.name)
                distance.set("%.2f".format(it.totalDistanceInMeters / 1609.34) + " miles")
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
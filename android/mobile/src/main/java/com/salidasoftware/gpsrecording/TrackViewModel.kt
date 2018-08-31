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

    fun setTrack(owner: LifecycleOwner, track: LiveData<Track>) {
        track.observe(owner, Observer {
            Log.d("TrackActivity", "~~ Track LiveData changed!")
            it?.let {
                name.set(it.name)
            }
        })
    }

}
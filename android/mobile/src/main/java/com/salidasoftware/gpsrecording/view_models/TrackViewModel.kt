package com.salidasoftware.gpsrecording.view_models

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.Observer
import androidx.databinding.ObservableField
import com.salidasoftware.gpsrecording.room.GPSRecordingStore
import com.salidasoftware.gpsrecording.room.Track

class TrackViewModel() : ViewModel() {

    // https://developer.android.com/reference/android/databinding/ObservableField
    val name : ObservableField<String> = ObservableField("")
    val note : ObservableField<String> = ObservableField("")
    val distance : ObservableField<String> = ObservableField("")
    val duration : ObservableField<String> = ObservableField("")
    val activity : ObservableField<String> = ObservableField("")

    val isHike: ObservableField<Boolean> = ObservableField(false)
    val isBike: ObservableField<Boolean> = ObservableField(false)
    val isRun: ObservableField<Boolean> = ObservableField(false)
    val isSki: ObservableField<Boolean> = ObservableField(false)
    val isWalk: ObservableField<Boolean> = ObservableField(false)

    var observer: Observer<Track>? = null
    var observed: LiveData<Track>? = null

    fun unsetTrack() {
        unObserve()
        observed = null
        clearObservables()
    }

    fun clearObservables() {
        name.set("")
        note.set("")
        activity.set("")
        distance.set("")
        duration.set("")
        clearActivityObservables()
    }

    fun clearActivityObservables() {
        isHike.set(false)
        isBike.set(false)
        isRun.set(false)
        isSki.set(false)
        isWalk.set(false)
    }

    fun setTrack(owner: LifecycleOwner, track: LiveData<Track>) {

        unObserve()

        observed = track
        observer = Observer {
            Log.d("TrackActivity", "~~ Track LiveData changed!")
            if (it == null) {
                clearObservables()
                unObserve()
            } else {
                name.set(it.name)
                note.set(it.note)
                activity.set(it.activity)
                distance.set(track.value?.formattedDistance(GPSRecordingStore.displayMetricUnits.get() ?: false))
                duration.set(track.value?.formattedDuration())

                clearActivityObservables()
                if(activity.get().equals(Track.Activity.RUN.activityName)) {
                    isRun.set(true)
                }
                if(activity.get().equals(Track.Activity.HIKE.activityName)) {
                    isHike.set(true)
                }
                if(activity.get().equals(Track.Activity.BIKE.activityName)) {
                    isBike.set(true)
                }
                if(activity.get().equals(Track.Activity.SKI.activityName)) {
                    isSki.set(true)
                }
                if(activity.get().equals(Track.Activity.WALK.activityName)) {
                    isWalk.set(true)
                }

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
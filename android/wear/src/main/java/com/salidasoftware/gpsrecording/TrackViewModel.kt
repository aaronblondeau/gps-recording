package com.salidasoftware.gpsrecording

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.salidasoftware.gpsrecording.room.Track

class TrackViewModel : ViewModel() {

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

    fun setTrack(track: LiveData<Track>) {

        track.value?.let {

            name.set(it.name)
            note.set(it.note)
            activity.set(it.activity)
            distance.set(track.value?.formattedDistance(GPSRecordingWearApplication.storeView.displayMetricUnits.get()
                    ?: false))
            duration.set(track.value?.formattedDuration())

            if (activity.get().equals(Track.Activity.RUN.activityName)) {
                isRun.set(true)
            }
            if (activity.get().equals(Track.Activity.HIKE.activityName)) {
                isHike.set(true)
            }
            if (activity.get().equals(Track.Activity.BIKE.activityName)) {
                isBike.set(true)
            }
            if (activity.get().equals(Track.Activity.SKI.activityName)) {
                isSki.set(true)
            }
            if (activity.get().equals(Track.Activity.WALK.activityName)) {
                isWalk.set(true)
            }

        }


    }


}
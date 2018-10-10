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

    fun setTrack(track: Track?) {
        if (track != null) {
            name.set(track.name)
            note.set(track.note)
            activity.set(track.activity)
            distance.set(track.formattedDistance(GPSRecordingWearApplication.storeView.displayMetricUnits.get()
                    ?: false))
            duration.set(track.formattedDuration())

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
        } else {
            name.set("")
            note.set("")
            activity.set("")
            distance.set("")
            duration.set("")
            activity.set("")
            isHike.set(false)
            isBike.set(false)
            isRun.set(false)
            isSki.set(false)
            isWalk.set(false)
        }
    }
}
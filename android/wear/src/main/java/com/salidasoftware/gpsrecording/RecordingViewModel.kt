package com.salidasoftware.gpsrecording

import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel

class RecordingViewModel  : ViewModel() {

    val recording : ObservableField<Boolean> = ObservableField(false)
    val hasCurrentTrack : ObservableField<Boolean> = ObservableField(false)

    var recordingCallback : Observable.OnPropertyChangedCallback? = null
    var currentTrackCallback : Observable.OnPropertyChangedCallback? = null

    init {
        recordingCallback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                this@RecordingViewModel.updateRecording()
            }
        }
        currentTrackCallback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                this@RecordingViewModel.updateHasCurrentTrack()
            }
        }
        updateHasCurrentTrack()
        updateRecording()
    }

    fun watch() {
        GPSRecordingService.recording.addOnPropertyChangedCallback(recordingCallback!!)
        GPSRecordingWearApplication.storeView.currentTrackId.addOnPropertyChangedCallback(currentTrackCallback!!)
        updateHasCurrentTrack()
        updateRecording()
    }

    fun unwatch() {
        GPSRecordingService.recording.removeOnPropertyChangedCallback(recordingCallback!!)
        GPSRecordingWearApplication.storeView.currentTrackId.removeOnPropertyChangedCallback(currentTrackCallback!!)
    }

    fun updateRecording() {
        this.recording.set(GPSRecordingService.recording.get())
    }

    fun updateHasCurrentTrack() {
        var currentTrackId: Long = GPSRecordingWearApplication.storeView.currentTrackId.get() ?: -1
        this@RecordingViewModel.hasCurrentTrack.set(currentTrackId > -1)
    }

    override fun onCleared() {

        recordingCallback?.let {
            GPSRecordingService.recording.removeOnPropertyChangedCallback(it)
        }

        currentTrackCallback?.let {
            GPSRecordingWearApplication.storeView.currentTrackId.removeOnPropertyChangedCallback(it)
        }

        super.onCleared()
    }
}
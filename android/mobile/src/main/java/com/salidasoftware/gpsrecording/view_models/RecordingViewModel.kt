package com.salidasoftware.gpsrecording.view_models

import android.arch.lifecycle.ViewModel
import android.databinding.Observable
import android.databinding.ObservableField
import com.salidasoftware.gpsrecording.GPSRecordingService
import com.salidasoftware.gpsrecording.room.GPSRecordingStore

class RecordingViewModel : ViewModel() {

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
        GPSRecordingService.recording.addOnPropertyChangedCallback(recordingCallback!!)
        updateRecording()

        currentTrackCallback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                this@RecordingViewModel.updateHasCurrentTrack()
            }
        }
        GPSRecordingStore.currentTrackId.addOnPropertyChangedCallback(currentTrackCallback!!)
        updateHasCurrentTrack()
    }

    fun updateRecording() {
        this.recording.set(GPSRecordingService.recording.get())
    }

    fun updateHasCurrentTrack() {
        var currentTrackId: Long = GPSRecordingStore.currentTrackId.get() ?: -1
        this@RecordingViewModel.hasCurrentTrack.set(currentTrackId > -1)
    }

    override fun onCleared() {

        recordingCallback?.let {
            GPSRecordingService.recording.removeOnPropertyChangedCallback(it)
        }

        currentTrackCallback?.let {
            GPSRecordingStore.currentTrackId.removeOnPropertyChangedCallback(it)
        }

        super.onCleared()
    }
}
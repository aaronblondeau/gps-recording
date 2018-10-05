package com.salidasoftware.gpsrecording.view_models

import androidx.lifecycle.ViewModel
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import com.salidasoftware.gpsrecording.GPSRecordingApplication
import com.salidasoftware.gpsrecording.GPSRecordingService

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
        GPSRecordingApplication.storeView.currentTrackId.addOnPropertyChangedCallback(currentTrackCallback!!)
        updateHasCurrentTrack()
    }

    fun updateRecording() {
        this.recording.set(GPSRecordingService.recording.get())
    }

    fun updateHasCurrentTrack() {
        var currentTrackId: Long = GPSRecordingApplication.storeView.currentTrackId.get() ?: -1
        this@RecordingViewModel.hasCurrentTrack.set(currentTrackId > -1)
    }

    override fun onCleared() {

        recordingCallback?.let {
            GPSRecordingService.recording.removeOnPropertyChangedCallback(it)
        }

        currentTrackCallback?.let {
            GPSRecordingApplication.storeView.currentTrackId.removeOnPropertyChangedCallback(it)
        }

        super.onCleared()
    }
}
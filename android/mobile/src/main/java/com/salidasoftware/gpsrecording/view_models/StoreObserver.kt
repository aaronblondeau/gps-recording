package com.salidasoftware.gpsrecording.view_models

import androidx.databinding.Observable
import androidx.databinding.ObservableField
import com.salidasoftware.gpsrecording.room.CurrentTrackDeletedHandler
import com.salidasoftware.gpsrecording.room.GPSRecordingStore

class StoreObserver(store: GPSRecordingStore) : CurrentTrackDeletedHandler {

    val store = store
    val currentTrackId: ObservableField<Long> = ObservableField(-1)
    val distanceFilterInMeters: ObservableField<Float> = ObservableField(10f)
    val timeFilterInMilliseconds: ObservableField<Long> = ObservableField(1000L)
    val displayMetricUnits: ObservableField<Boolean> = ObservableField(false)

    init {

        store.addCurrentTrackDeletedHandler(this)

        currentTrackId.set(store.currentTrackId)

        // Update shared prefs if currentTrackId changes
        currentTrackId.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                currentTrackId.get()?.let {
                    store.currentTrackId = it
                }
            }
        })

        distanceFilterInMeters.set(store.distanceFilterInMeters)

        // Update shared prefs if distanceFilterInMeters changes
        distanceFilterInMeters.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                distanceFilterInMeters.get()?.let {
                    store.distanceFilterInMeters = it
                }
            }
        })

        timeFilterInMilliseconds.set(store.timeFilterInMilliseconds)

        // Update shared prefs if distanceFilterInMeters changes
        timeFilterInMilliseconds.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                timeFilterInMilliseconds.get()?.let {
                    store.timeFilterInMilliseconds = it
                }
            }
        })

        displayMetricUnits.set(store.displayMetricUnits)

        // Update shared prefs if distanceFilterInMeters changes
        displayMetricUnits.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                displayMetricUnits.get()?.let {
                    store.displayMetricUnits = it
                }
            }
        })

    }

    override fun onCurrentTrackDeleted() {
        currentTrackId.set(-1)
    }
}
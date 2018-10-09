package com.salidasoftware.gpsrecording

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import kotlinx.android.synthetic.main.activity_record.*
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast

class RecordActivity : WearableActivity() {

    val store = GPSRecordingWearApplication.store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        // Enables Always-on
        setAmbientEnabled()

        buttonTest.setOnClickListener {
            doAsync { store.createTrack() }
            toast("Test track created!")
        }
    }
}

package com.salidasoftware.gpsrecording

import android.app.Application
import android.arch.persistence.room.Room

class GPSRecordingApplication: Application() {
    companion object {
        var database: GPSRecordingDatabase? = null
    }

    override fun onCreate() {
        super.onCreate()
        GPSRecordingApplication.database = Room.databaseBuilder(this, GPSRecordingDatabase::class.java, "gps-recording-database").build()
    }
}
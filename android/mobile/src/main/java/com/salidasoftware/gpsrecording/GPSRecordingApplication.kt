package com.salidasoftware.gpsrecording

import android.app.Application
import android.arch.persistence.room.Room

class GPSRecordingApplication: Application() {
    companion object {
        var database: GPSRecordingDatabase? = null
        fun getDatabase(application: Application) : GPSRecordingDatabase {
            val db = database
            if (db != null) {
                return db
            } else {
                val newDatabase = Room.databaseBuilder(application, GPSRecordingDatabase::class.java, "gps-recording-database").build()
                database = newDatabase
                return newDatabase
            }
        }

        var store: GPSRecordingStore? = null
        fun getStore(database: GPSRecordingDatabase) : GPSRecordingStore {
            val s = store
            if (s != null) {
                return s
            } else {
                val newStore = GPSRecordingStore(database)
                store = newStore
                return newStore
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val database = GPSRecordingApplication.getDatabase(this)
        GPSRecordingApplication.getStore(database)
    }

}
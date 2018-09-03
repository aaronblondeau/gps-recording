package com.salidasoftware.gpsrecording

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.arch.persistence.room.Room
import android.os.Build

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

        val CHANNEL_DEFAULT_IMPORTANCE = "GPSRecordingService"
    }

    override fun onCreate() {
        super.onCreate()
        val database = GPSRecordingApplication.getDatabase(this)
        // Init the store's observable track id
        GPSRecordingStore.getCurrentTrackId(this)
        GPSRecordingApplication.getStore(database)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.record_channel_name)
            val description = getString(R.string.record_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_DEFAULT_IMPORTANCE, name, importance)
            channel.setDescription(description)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService<NotificationManager>(NotificationManager::class.java!!)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

}
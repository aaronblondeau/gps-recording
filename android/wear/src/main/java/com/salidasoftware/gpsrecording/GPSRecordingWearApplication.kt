package com.salidasoftware.gpsrecording

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.salidasoftware.gpsrecording.room.GPSRecordingStore

class GPSRecordingWearApplication: Application() {
    companion object {
        lateinit var context: Context
        lateinit var store: GPSRecordingStore
        lateinit var storeView : StoreObserver
        val CHANNEL_DEFAULT_IMPORTANCE = "GPSRecordingService"
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        store = GPSRecordingStore(this)
        storeView = StoreObserver(store)
        createNotificationChannel()
    }

    // This is necessary for showing the foreground service notification on 26+
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
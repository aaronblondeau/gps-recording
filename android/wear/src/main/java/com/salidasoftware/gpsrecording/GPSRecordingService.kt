package com.salidasoftware.gpsrecording

import android.app.IntentService
import android.app.Service
import android.content.Intent
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.databinding.ObservableField
import com.salidasoftware.gpsrecording.room.GPSRecordingStore
import com.salidasoftware.gpsrecording.room.Track


/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class GPSRecordingService : Service(), LocationListener {

    companion object {
        var recording : ObservableField<Boolean> = ObservableField(false)
    }

    private var store: GPSRecordingStore? = GPSRecordingWearApplication.store
    private var currentTrack: Track? = null

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onLocationChanged(location: Location?) {

    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
        Log.d("GPSRecordingService", "~~ onStatusChanged")
    }

    override fun onProviderEnabled(p0: String?) {
        Log.d("GPSRecordingService", "~~ onProviderEnabled")
    }

    override fun onProviderDisabled(p0: String?) {
        Log.d("GPSRecordingService", "~~ onProviderDisabled")
    }
}

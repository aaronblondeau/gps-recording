package com.salidasoftware.gpsrecording

import android.Manifest
import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableField
import com.google.android.gms.location.*
import com.salidasoftware.gpsrecording.room.GPSRecordingStore
import com.salidasoftware.gpsrecording.room.Track
import org.jetbrains.anko.doAsync
import java.util.*
import kotlin.concurrent.*


/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class GPSRecordingService : Service() {

    companion object {
        var recording : ObservableField<Boolean> = ObservableField(false)
    }

    private var store: GPSRecordingStore = GPSRecordingWearApplication.store
    private var currentTrack: Track? = null
    val ONGOING_NOTIFICATION_ID = 1337

    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var locationCallback: LocationCallback

    val timer = Timer()
    var ticker: TimerTask? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    Log.d("GPSRecordingService", "~~ Got location " + location.latitude + "," + location.longitude)
                    currentTrack?.let {track ->
                        doAsync {
                            try {
                                // Make sure track still exists
                                val exists = store?.getTrack(track.id)
                                if (exists != null) {
                                    try {
                                        Log.d("GPSRecordingService", "~~ Adding location " + location.latitude + "," + location.longitude + " to track " + track.name)
                                        store.addLocationToTrack(track, location)
                                        GPSRecordingWearApplication.storeView.currentElapsedTimeInMilliseconds.set(track.totalDurationInMilliseconds)
                                    } catch (e: Exception) {
                                        Log.e("GPSRecordingService", "~~ Unable to store location!", e)
                                    }
                                } else {
                                    Log.d("GPSRecordingService", "~~ Track no longer exists.  Stopping recording.")
                                    GPSRecordingWearApplication.storeView.currentTrackId.set(-1)
                                    stopSelf()
                                }
                            } catch(e: Exception) {
                                Log.d("GPSRecordingService", "~~ Error adding location", e)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        doAsync {
            var currentTrackId = GPSRecordingWearApplication.storeView.currentTrackId.get() ?: -1
            if (currentTrackId < 0) {
                val newTrk = store.createTrack()
                currentTrack = newTrk
                GPSRecordingWearApplication.storeView.currentTrackId.set(newTrk.id)
            } else {
                val trk = store.getTrack(currentTrackId)
                if (trk != null) {
                    currentTrack = trk
                } else {
                    val newTrk = store.createTrack()
                    currentTrack = newTrk
                    GPSRecordingWearApplication.storeView.currentTrackId.set(newTrk.id)
                }
            }
            currentTrack?.let { track ->
                GPSRecordingWearApplication.storeView.currentElapsedTimeInMilliseconds.set(track.totalDurationInMilliseconds)
            }
        }

        if (ContextCompat.checkSelfPermission(this@GPSRecordingService, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            val tf = GPSRecordingWearApplication.storeView.timeFilterInMilliseconds.get() ?: 1000L
            val df = GPSRecordingWearApplication.storeView.distanceFilterInMeters.get() ?: 10f

            // https://developer.android.com/training/location/receive-location-updates

            val locationRequest = LocationRequest().apply {
                interval = tf
                smallestDisplacement = df
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            recording.set(true)

            val notificationIntent = Intent(this@GPSRecordingService, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this@GPSRecordingService, 0, notificationIntent, 0)

            if (Build.VERSION.SDK_INT >= 26) {
                val notification = Notification.Builder(this@GPSRecordingService, GPSRecordingWearApplication.CHANNEL_DEFAULT_IMPORTANCE)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getText(R.string.record_notification_title))
                        //.setContentText(getText(R.string.record_notification_message))
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .build()

                startForeground(ONGOING_NOTIFICATION_ID, notification)
            } else {
                val notification = Notification.Builder(this@GPSRecordingService)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getText(R.string.record_notification_title))
                        //.setContentText(getText(R.string.record_notification_message))
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .build()

                startForeground(ONGOING_NOTIFICATION_ID, notification)
            }

            ticker = timerTask{
                GPSRecordingWearApplication.storeView.currentElapsedTimeInMilliseconds.get()?.let {
                    GPSRecordingWearApplication.storeView.currentElapsedTimeInMilliseconds.set(it.plus(1000L))
                }
            }
            timer.scheduleAtFixedRate(ticker, 1000L, 1000L)

        } else {
            recording.set(false)
            stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        recording.set(false)
        ticker?.let {
            it.cancel()
            ticker = null
        }
    }
}

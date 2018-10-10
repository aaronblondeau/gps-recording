package com.salidasoftware.gpsrecording

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import androidx.databinding.ObservableField
import android.os.IBinder
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.util.Log
import com.salidasoftware.gpsrecording.activities.RecordActivity
import com.salidasoftware.gpsrecording.room.GPSRecordingStore
import com.salidasoftware.gpsrecording.room.Track
import org.jetbrains.anko.doAsync
import java.util.*
import kotlin.concurrent.timerTask

class GPSRecordingService : Service(), LocationListener {

    companion object {
        var recording : ObservableField<Boolean> = ObservableField(false)
    }

    private var store: GPSRecordingStore = GPSRecordingApplication.store
    private var currentTrack: Track? = null
    val ONGOING_NOTIFICATION_ID = 1337
    var locationManager : LocationManager? = null

    val timer = Timer()
    var ticker: TimerTask? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (store == null) {
            store = GPSRecordingApplication.store
        }
        store?.let { store ->
            doAsync {
                var currentTrackId = GPSRecordingApplication.storeView.currentTrackId.get() ?: -1
                if (currentTrackId < 0) {
                    val newTrk = store.createTrack()
                    currentTrack = newTrk
                    GPSRecordingApplication.storeView.currentTrackId.set(newTrk.id)
                } else {
                    val trk = store.getTrack(currentTrackId)
                    if (trk != null) {
                        currentTrack = trk
                    } else {
                        val newTrk = store.createTrack()
                        currentTrack = newTrk
                        GPSRecordingApplication.storeView.currentTrackId.set(newTrk.id)
                    }
                }
                currentTrack?.let { track ->
                    GPSRecordingApplication.storeView.currentElapsedTimeInMilliseconds.set(track.totalDurationInMilliseconds)
                }
            }

            if (ContextCompat.checkSelfPermission(this@GPSRecordingService, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lm = this@GPSRecordingService.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                locationManager = lm

                val tf = GPSRecordingApplication.storeView.timeFilterInMilliseconds.get() ?: 1000L
                val df = GPSRecordingApplication.storeView.distanceFilterInMeters.get() ?: 10f

                // https://developer.android.com/reference/android/location/LocationManager#requestLocationUpdates(java.lang.String,%20long,%20float,%20android.app.PendingIntent)
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, tf, df, this@GPSRecordingService);
                recording.set(true)

                val notificationIntent = Intent(this@GPSRecordingService, RecordActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(this@GPSRecordingService, 0, notificationIntent, 0)

                if (Build.VERSION.SDK_INT >= 26) {
                    val notification = Notification.Builder(this@GPSRecordingService, GPSRecordingApplication.CHANNEL_DEFAULT_IMPORTANCE)
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
                    GPSRecordingApplication.storeView.currentElapsedTimeInMilliseconds.get()?.let {
                        GPSRecordingApplication.storeView.currentElapsedTimeInMilliseconds.set(it.plus(1000L))
                    }
                }
                timer.scheduleAtFixedRate(ticker, 1000L, 1000L)

            } else {
                recording.set(false)
                stopSelf()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.let {
            it.removeUpdates(this)
        }
        recording.set(false)
        ticker?.let {
            it.cancel()
            ticker = null
        }
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {location ->
            currentTrack?.let {track ->
                doAsync {
                    // Make sure track still exists
                    val exists = store?.getTrack(track.id)
                    if (exists != null) {
                        try {
                            Log.d("GPSRecordingService", "~~ Adding location " + location.latitude + "," + location.longitude + " to track " + track.name)
                            store.addLocationToTrack(track, location)
                            GPSRecordingApplication.storeView.currentElapsedTimeInMilliseconds.set(track.totalDurationInMilliseconds)
                        } catch (e: Exception) {
                            Log.e("GPSRecordingService", "~~ Unable to store location!", e)
                        }
                    } else {
                        Log.d("GPSRecordingService", "~~ Track no longer exists.  Stopping recording.")
                        GPSRecordingApplication.storeView.currentTrackId.set(-1)
                        stopSelf()
                    }
                }
            }
        }
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

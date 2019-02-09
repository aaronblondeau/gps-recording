package com.salidasoftware.gpsrecording

import android.graphics.Color
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.salidasoftware.gpsrecording.room.Track
import kotlinx.android.synthetic.main.activity_track_map.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.lang.Exception
import java.lang.IllegalStateException

class TrackMapActivity : WearableActivity() {

    val store = GPSRecordingWearApplication.store
    var map: GoogleMap? = null
    var trackId: Long = -1

    var lines: ArrayList<Polyline> = ArrayList()

    companion object {
        const val TRACK_ID = "track_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_map)

        // Enables Always-on
        // setAmbientEnabled()

        trackId = intent.getLongExtra(TrackMapActivity.TRACK_ID, -1)

        mapViewTrack.onCreate(savedInstanceState)

        buttonTrackMapClose.setOnClickListener {
            this.finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (trackId >= 0 && store != null) {
            mapViewTrack.onResume()
            mapViewTrack.getMapAsync {
                map = it
                renderMap()
            }
        } else {
            this.finish()
        }
    }

    override fun onPause() {
        super.onPause()
        mapViewTrack.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapViewTrack.onDestroy()
    }

    private fun renderMap() {

        doAsync {
            try {

                val track = store.getTrack(trackId)

                track?.let { existingTrack ->
                    val boundsBuilder = LatLngBounds.Builder()
                    val lines = store.lineDAO.getAllForTrack(existingTrack.id)
                    for (line in lines) {
                        val points = store.pointDAO.getAllForLine(line.id)

                        val lineLLs: ArrayList<LatLng> = ArrayList()
                        if (points.size > 1) {
                            for (point in points) {
                                val ll = LatLng(point.latitude, point.longitude)
                                lineLLs.add(ll)
                                boundsBuilder.include(ll)
                            }
                        }

                        uiThread {
                            try {
                                val mapLine = map?.addPolyline(PolylineOptions().addAll(lineLLs).width(5f).color(Color.RED))
                                if (mapLine != null) {
                                    this@TrackMapActivity.lines.add(mapLine)
                                }
                            }
                            catch(e: Exception) {
                                Log.d("TrackMapActivity", "~~ map lines add error", e)
                            }
                        }
                    }

                    uiThread {
                        try {
                            map?.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 50))
                        } catch(e: IllegalStateException) {
                            Log.d("TrackMapActivity", "~~ failed to set map camera", e)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.d("TrackMapActivity", "~~ map render exception", e)
            }
        }
    }
}

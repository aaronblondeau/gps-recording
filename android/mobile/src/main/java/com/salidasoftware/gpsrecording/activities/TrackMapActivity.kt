package com.salidasoftware.gpsrecording.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.salidasoftware.gpsrecording.GPSRecordingApplication
import com.salidasoftware.gpsrecording.R
import com.salidasoftware.gpsrecording.room.Track

import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.android.synthetic.main.activity_track_map.*
import kotlinx.android.synthetic.main.content_track_map.*
import java.lang.Exception
import java.lang.IllegalStateException

class TrackMapActivity : AppCompatActivity() {

    val store = GPSRecordingApplication.store
    lateinit var track : LiveData<Track>
    var map: GoogleMap? = null

    var lines: ArrayList<Polyline> = ArrayList()

    companion object {
        const val TRACK_ID = "track_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_map)

        setSupportActionBar(toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true) }

        val trackId = intent.getLongExtra(TrackActivity.TRACK_ID, -1)
        if (trackId >= 0 && store != null) {
            (this@TrackMapActivity.trackMap as SupportMapFragment?)?.getMapAsync {
                this@TrackMapActivity.map = it

                doAsync {
                    track = store.trackDAO.getByIdLive(trackId)
                    uiThread {
                        track.observe(this@TrackMapActivity, Observer { track ->
                            if (track != null) {
                                setTitle(track.name)
                                renderMap()
                            }
                        })
                    }
                }
            }
        } else {
            this.finish()
        }
    }

    private fun renderMap() {

        doAsync {
            try {
                if (store != null) {
                    track.value?.let { track ->
                        val boundsBuilder = LatLngBounds.Builder()
                        val lines = store.lineDAO.getAllForTrack(track.id)
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
                }
            } catch (e: Exception) {
                Log.d("TrackMapActivity", "~~ map render exception", e)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
            = when(item?.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true //navigateUpTo(Intent(this, MainActivity::class.java))
        }
        else -> super.onOptionsItemSelected(item)
    }

}

package com.salidasoftware.gpsrecording.activities

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.salidasoftware.gpsrecording.GPSRecordingApplication
import com.salidasoftware.gpsrecording.R
import com.salidasoftware.gpsrecording.databinding.ActivityTrackBinding
import com.salidasoftware.gpsrecording.room.Track

import kotlinx.android.synthetic.main.activity_track_map.*
import kotlinx.android.synthetic.main.content_track.*
import kotlinx.android.synthetic.main.content_track_map.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.LatLngBounds

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
                    track.observe(this@TrackMapActivity, Observer {track ->
                        if(track != null) {
                            uiThread {
                                setTitle(track.name)
                            }
                            renderMap()
                        }
                    })
                }
            }
        } else {
            this.finish()
        }
    }

    private fun renderMap() {

        doAsync {
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
                            val mapLine = map?.addPolyline(PolylineOptions().addAll(lineLLs).width(5f).color(Color.RED))
                            if (mapLine != null) {
                                this@TrackMapActivity.lines.add(mapLine)
                            }
                        }
                    }

                    uiThread {
                        map?.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 50))
                    }
                }
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

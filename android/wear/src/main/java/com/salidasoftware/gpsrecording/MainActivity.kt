package com.salidasoftware.gpsrecording

import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enables Always-on
        //setAmbientEnabled()

        buttonRecord.setOnClickListener {
            var recordIntent: Intent = Intent(this@MainActivity, RecordActivity::class.java)
            startActivity(recordIntent)
        }

        buttonSettings.setOnClickListener {
            var settingsIntent: Intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        buttonTracks.setOnClickListener {
            var tracksIntent: Intent = Intent(this@MainActivity, TracksActivity::class.java)
            startActivity(tracksIntent)
        }

    }
}

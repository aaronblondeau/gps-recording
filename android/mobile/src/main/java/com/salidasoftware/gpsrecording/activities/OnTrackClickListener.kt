package com.salidasoftware.gpsrecording.activities

import com.salidasoftware.gpsrecording.room.Track

interface OnTrackClickListener {
    fun onTrackClick(track: Track)
}
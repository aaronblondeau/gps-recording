package com.salidasoftware.gpsrecording.ui

import com.salidasoftware.gpsrecording.room.Track

interface OnTrackClickListener {
    fun onTrackClick(track: Track)
}
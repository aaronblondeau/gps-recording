package com.salidasoftware.gpsrecording

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView

class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val view = view
    val title: TextView = view.findViewById(R.id.textViewTrackName) as TextView
}
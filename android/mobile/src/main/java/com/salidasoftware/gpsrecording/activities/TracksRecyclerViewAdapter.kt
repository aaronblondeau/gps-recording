package com.salidasoftware.gpsrecording.activities

import android.arch.paging.PagedListAdapter
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.salidasoftware.gpsrecording.R
import com.salidasoftware.gpsrecording.room.Track
import java.text.SimpleDateFormat
import java.util.*

class TracksRecyclerViewAdapter(val context: Context) : PagedListAdapter<Track, TracksRecyclerViewAdapter.TrackViewHolder>(TrackDiffCallback()) {

    var clickListener: OnTrackClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        return TrackViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false))
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = getItem(position)
        if (track == null) {
            holder.clear()
        } else {
            holder.bind(track, clickListener)
        }
    }

    fun setOnTrackClickListener(cl: OnTrackClickListener) {
        clickListener = cl
    }

    class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val title: TextView = view.findViewById(R.id.textViewTrackName) as TextView
        var distance: TextView = view.findViewById(R.id.textViewTrackDistance) as TextView
        var date: TextView = view.findViewById(R.id.textViewTrackDate) as TextView
        var clickListener : OnTrackClickListener? = null

        fun bind(track: Track, cl: OnTrackClickListener?) {
            clickListener = cl
            itemView.setOnClickListener(this)
            title.text = track.name
            distance.text = "%.2f".format(track.totalDistanceInMeters / 1609.34) + " miles"
            date.text = SimpleDateFormat("M/dd/yyyy hh:mm:ss").format(Date(track.startedAt))
            itemView.tag = track
        }

        fun clear() {
            title.text = null
            distance.text = null
            date.text = null
            itemView.tag = null
        }

        override fun onClick(view: View?) {
            val cl = clickListener
            if (view != null && cl != null) {
                val track = view.tag as Track
                if (track != null) {
                    cl.onTrackClick(track)
                }
            }
        }
    }

}
package com.salidasoftware.gpsrecording

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.salidasoftware.gpsrecording.room.Track
import com.salidasoftware.gpsrecording.ui.OnTrackClickListener
import com.salidasoftware.gpsrecording.ui.TrackDiffCallback

class TracksRecyclerViewAdapter (private var tracks: Array<Track>) : RecyclerView.Adapter<TracksRecyclerViewAdapter.TrackViewHolder>() {

    var clickListener: OnTrackClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        return TrackViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false))
    }

    override fun getItemCount(): Int {
        return tracks.size
    }

    public fun setTracks(tracks: Array<Track>) {
        this.tracks = tracks
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks.get(position)
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
        var clickListener: OnTrackClickListener? = null

        fun bind(track: Track, cl: OnTrackClickListener?) {
            clickListener = cl
            itemView.setOnClickListener(this)
            title.text = track.name
            distance.text = track.formattedDistance(GPSRecordingWearApplication.storeView.displayMetricUnits.get()
                    ?: false)
            date.text = track.formattedStartDate()
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
package com.salidasoftware.gpsrecording

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class TracksRecyclerViewAdapter(tracks : List<Track>, longClickListener: View.OnLongClickListener) : RecyclerView.Adapter<TrackViewHolder>() {

    var tracks = tracks
    val longClickListener = longClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        return TrackViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false))
    }

    override fun getItemCount(): Int {
        return tracks.size.toInt()
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks.get(position)
        holder.title.setText(track.name)
        holder.itemView.tag = track
        holder.itemView.setOnLongClickListener(longClickListener)
    }

    fun resetTracks(trackList : List<Track>) {
        tracks = trackList
        notifyDataSetChanged()
    }

}
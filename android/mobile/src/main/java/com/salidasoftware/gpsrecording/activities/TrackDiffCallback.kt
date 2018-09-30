package com.salidasoftware.gpsrecording.activities

import androidx.recyclerview.widget.DiffUtil
import com.salidasoftware.gpsrecording.room.Track

class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
    override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem == newItem
    }
}
package com.salidasoftware.gpsrecording

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface TrackDAO {
    @Query("SELECT * FROM tracks ORDER BY startedAt DESC")
    fun getAll(): List<Track>

    @Insert
    fun insert(track: Track)
}
package com.salidasoftware.gpsrecording

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface LineDAO {
    @Query("SELECT * FROM lines WHERE trackId = :trackId ORDER BY startedAt DESC")
    fun getAllForTrack(trackId: Long): List<Line>

    @Query("SELECT count(1) FROM lines")
    fun count(): Long

    @Insert
    fun insert(line: Line) : Long
}
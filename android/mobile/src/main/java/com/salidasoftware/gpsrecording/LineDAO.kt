package com.salidasoftware.gpsrecording

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update

@Dao
interface LineDAO {
    @Query("SELECT * FROM lines WHERE trackId = :trackId ORDER BY startedAt DESC")
    fun getAllForTrack(trackId: Long): List<Line>

    @Query("SELECT * FROM lines WHERE id = :id")
    fun getById(id: Long): Line?

    @Query("SELECT * FROM lines WHERE trackId = :trackId ORDER BY startedAt DESC LIMIT 1")
    fun getCurrentLineForTrack(trackId: Long) : Line?

    @Query("SELECT count(1) FROM lines")
    fun count(): Long

    @Query("SELECT count(1) FROM lines WHERE trackId = :trackId")
    fun countLinesInTrack(trackId: Long) : Long

    @Insert
    fun insert(line: Line) : Long

    @Update
    fun update(line: Line)
}
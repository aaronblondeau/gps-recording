package com.salidasoftware.gpsrecording

import android.arch.persistence.room.*

@Dao
interface TrackDAO {
    @Query("SELECT * FROM tracks ORDER BY startedAt DESC")
    fun getAll(): List<Track>

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun getById(id: Long): Track

    @Insert
    fun insert(track: Track) : Long

    @Update
    fun update(track: Track)

    @Delete
    fun delete(track: Track)
}
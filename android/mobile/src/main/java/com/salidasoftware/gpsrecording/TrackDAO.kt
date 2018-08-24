package com.salidasoftware.gpsrecording

import android.arch.persistence.room.*

@Dao
interface TrackDAO {
    @Query("SELECT * FROM tracks ORDER BY startedAt DESC")
    fun getAll(): List<Track>

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun getById(id: Long): Track?

    @Query("SELECT * FROM tracks WHERE upstreamid = :id LIMIT 1")
    fun getByUpsreamId(id: String): Track?

    @Query("SELECT count(1) FROM tracks")
    fun count() : Long

    @Insert
    fun insert(track: Track) : Long

    @Update
    fun update(track: Track)

    @Delete
    fun delete(track: Track)
}
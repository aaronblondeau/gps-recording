package com.salidasoftware.gpsrecording

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
import android.arch.persistence.room.*

@Dao
interface TrackDAO {
    @Query("SELECT * FROM tracks ORDER BY startedAt DESC")
    fun getAll(): List<Track>

    @Query("SELECT * FROM tracks ORDER BY startedAt DESC")
    fun getAllLive(): LiveData<List<Track>>

    @Query("SELECT * FROM tracks")
    fun getAllPaged(): DataSource.Factory<Int, Track>

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun getById(id: Long): Track?

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun getByIdLive(id: Long): LiveData<Track>

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
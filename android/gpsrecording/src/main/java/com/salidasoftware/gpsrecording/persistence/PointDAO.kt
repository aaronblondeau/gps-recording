package com.salidasoftware.gpsrecording.room

import androidx.room.*

@Dao
interface PointDAO {
    @Query("SELECT * FROM points WHERE lineId = :lineId ORDER BY time ASC")
    fun getAllForLine(lineId: Long): List<Point>

    @Query("SELECT * FROM points WHERE id = :id")
    fun getById(id: Long): Point?

    @Insert
    fun insert(point: Point) : Long

    @Insert
    fun insertAll(vararg points: Point)

    @Query("SELECT * FROM points WHERE lineId = :lineId ORDER BY time DESC LIMIT 1")
    fun getLastPointInLine(lineId: Long): Point?

    @Query("SELECT count(1) FROM points")
    fun count(): Long

    @Query("SELECT count(1) FROM points WHERE lineId = :lineId")
    fun countPointsInLine(lineId: Long) : Long
}
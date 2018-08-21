package com.salidasoftware.gpsrecording

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface PointDAO {
    @Query("SELECT * FROM points WHERE lineId = :lineId ORDER BY time ASC")
    fun getAllForLine(lineId: Long): List<Point>

    @Insert
    fun insert(point: Point)

    @Insert
    fun insertAll(vararg points: Point)

    @Query("SELECT count(1) FROM points")
    fun count(): Long
}
package com.salidasoftware.gpsrecording.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(Track::class, Line::class, Point::class), version = 1)
abstract class GPSRecordingDatabase : RoomDatabase() {
    abstract fun trackDAO(): TrackDAO
    abstract fun lineDAO(): LineDAO
    abstract fun pointDAO(): PointDAO
}
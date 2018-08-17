package com.salidasoftware.gpsrecording

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = arrayOf(Track::class, Line::class, Point::class), version = 1)
abstract class GPSRecordingDatabase : RoomDatabase() {
    abstract fun trackDAO(): TrackDAO
    abstract fun lineDAO(): LineDAO
    abstract fun pointDAO(): PointDAO
}
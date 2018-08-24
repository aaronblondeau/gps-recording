package com.salidasoftware.gpsrecording

import android.arch.persistence.room.*

@Entity(tableName = "points",
        foreignKeys = arrayOf(
                ForeignKey(entity = Line::class, parentColumns = arrayOf("id"), childColumns = arrayOf("lineId"), onDelete = ForeignKey.CASCADE)
        ),
        indices = arrayOf(
                Index(value = "lineId", name = "idx_point_trackId"),
                Index(value = "time", name = "idx_point_time")
                )
        )
data class Point (
        @ColumnInfo(name="lineId") var lineId: Long,
        @ColumnInfo(name="time") var time: Long,                                  // UTC time of this fix, in milliseconds since January 1, 1970.
        @ColumnInfo(name="latitude") var latitude: Double,
        @ColumnInfo(name="longitude") var longitude: Double,
        @ColumnInfo(name="horizontal_accuracy") var horizontal_accuracy: Float,   // estimated horizontal accuracy of this location, radial, in meters.
        @ColumnInfo(name="altitude") var altitude: Double? = null,                        // altitude if available, in meters above the WGS 84 reference ellipsoid.
        @ColumnInfo(name="vertical_accuracy") var vertical_accuracy: Float? =  null,       // estimated vertical accuracy of this location, in meters.
        @ColumnInfo(name="bearing") var bearing: Float? = null,
        @ColumnInfo(name="bearing_accuracy") var bearing_accuracy: Float? =  null,
        @ColumnInfo(name="speed") var speed: Float? = null,                                // speed if it is available, in meters/second over ground.
        @ColumnInfo(name="speed_accuracy") var speed_accuracy: Float? =  null,
        @ColumnInfo(name="id") @PrimaryKey(autoGenerate = true) var id: Long = 0
        )
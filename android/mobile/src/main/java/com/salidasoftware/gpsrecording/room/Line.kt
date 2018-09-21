package com.salidasoftware.gpsrecording.room

import android.arch.persistence.room.*

@Entity(tableName = "lines",
        foreignKeys = arrayOf(
                ForeignKey(entity = Track::class, parentColumns = arrayOf("id"), childColumns = arrayOf("trackId"), onDelete = ForeignKey.CASCADE)
        ),
        indices = arrayOf(
                Index(value = "trackId", name = "idx_line_trackId"),
                Index(value = "startedAt", name = "idx_line_startedAt")
                )
        )
data class Line(
        @ColumnInfo(name="trackId") var trackId: Long,
        @ColumnInfo(name="totalDistanceInMeters") var totalDistanceInMeters: Float = 0.0f,
        @ColumnInfo(name="startedAt") var startedAt: Long = System.currentTimeMillis(), // UTC time, in milliseconds since January 1, 1970
        @ColumnInfo(name="endedAt") var endedAt: Long = System.currentTimeMillis(),     // UTC time, in milliseconds since January 1, 1970
        @ColumnInfo(name="id") @PrimaryKey(autoGenerate = true) var id: Long = 0
)
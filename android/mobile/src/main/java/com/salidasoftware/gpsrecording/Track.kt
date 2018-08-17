package com.salidasoftware.gpsrecording

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "tracks",
        indices = arrayOf(
                Index(value = "startedAt", name = "idx_track_startedAt")
        ))
data class Track(
        @ColumnInfo(name="name") var name: String,
        @ColumnInfo(name="note") var note: String = "",
        @ColumnInfo(name="activity") var activity: String = "",
        @ColumnInfo(name="downstreamId") var downstreamId: String = "",
        @ColumnInfo(name="upstreamId") var upstreamId: String = "",
        @ColumnInfo(name="totalDistanceInMeters") var totalDistanceInMeters: Float = 0.0f,
        @ColumnInfo(name="totalDurationInMilliseconds") var totalDurationInMilliseconds: Long = 0,
        @ColumnInfo(name="startedAt") var startedAt: Long = System.currentTimeMillis(), // UTC time, in milliseconds since January 1, 1970
        @ColumnInfo(name="endedAt") var endedAt: Long = System.currentTimeMillis(),     // UTC time, in milliseconds since January 1, 1970
        @ColumnInfo(name="id") @PrimaryKey(autoGenerate = true) var id: Long = 0
        )

package com.salidasoftware.gpsrecording.room

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

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
        ) {

        enum class Activity(val activityName: String) {
                RUN("run"),
                BIKE("bike"),
                SKI("ski"),
                HIKE("hike"),
                WALK("walk")
        }

        fun formattedDistance(metric: Boolean) : String {
                if(metric) {
                        return "%.2f".format(this.totalDistanceInMeters / 1000) + " km"
                }
                return "%.2f".format(this.totalDistanceInMeters / 1609.34) + " miles"
        }

        fun formattedStartDate() : String {
                return SimpleDateFormat("M/dd/yyyy hh:mm:ss").format(Date(this.startedAt))
        }

        fun formattedDuration() : String {
                val seconds = (this.totalDurationInMilliseconds / 1000) % 60
                val minutes = (this.totalDurationInMilliseconds / (1000 * 60) % 60)
                val hours = (this.totalDurationInMilliseconds / (1000 * 60 * 60) % 24)
                return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
}

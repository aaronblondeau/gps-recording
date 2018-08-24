package com.salidasoftware.gpsrecording

import android.location.Location
import android.os.Build
import android.util.Log

class GPSRecordingStore(database: GPSRecordingDatabase) {

    private val trackDAO = database.trackDAO()
    private val lineDAO = database.lineDAO()
    private val pointDAO = database.pointDAO()

    fun createTrack(name: String, note: String, activity: String) : Track {
        val track = Track(name, note, activity)
        val trackId = trackDAO.insert(track)
        val result = trackDAO.getById(trackId)
        return result!!
    }

    fun getTrack(id: Long) : Track? {
        return trackDAO.getById(id)
    }

    fun saveTrack(track : Track) {
        trackDAO.update(track)
    }

    fun getTrackWithUpstreamId(id: String) : Track? {
        return trackDAO.getByUpsreamId(id)
    }

    fun setTrackDownstreamId(track: Track, downstreamId: String) {
        track.downstreamId = downstreamId
        trackDAO.update(track)
    }

    fun updateTrack(track: Track, name: String, note: String, activity: String) {
        track.name = name
        track.note = note
        track.activity = activity
        trackDAO.update(track)
    }

    fun deleteTrack(track: Track) {
        trackDAO.delete(track)
    }

    fun addLineToTrack(track: Track) : Line {
        val line = Line(track.id)
        val lineId = lineDAO.insert(line)
        val result = lineDAO.getById(lineId)
        return result!!
    }

    fun getLine(id: Long) : Line? {
        return lineDAO.getById(id)
    }

    @Throws(Exception::class)
    fun addLocationToLine(line: Line, location: Location) : Point {

        val lastPoint = pointDAO.getLastPointInLine(line.id)
        val track = trackDAO.getById(line.trackId)

        var altitude : Double? = null
        if (location.hasAltitude()) {
            altitude = location.altitude
        }
        var verticalAccuracy : Float? = null
        if (location.hasAltitude() && Build.VERSION.SDK_INT >= 26 && location.hasVerticalAccuracy()) {
            verticalAccuracy = location.verticalAccuracyMeters
        }
        var bearing : Float? = null
        if (location.hasBearing()) {
            bearing = location.bearing
        }
        var bearingAccuracy : Float? = null
        if (Build.VERSION.SDK_INT >= 26 && location.hasBearingAccuracy()) {
            bearingAccuracy = location.bearingAccuracyDegrees
        }
        var speed : Float? = null
        if (location.hasSpeed()) {
            speed = location.speed
        }
        var speedAccuracy : Float? = null
        if (Build.VERSION.SDK_INT >= 26 && location.hasSpeedAccuracy()) {
            speedAccuracy = location.speedAccuracyMetersPerSecond
        }
        val point = Point(line.id, location.time, location.latitude, location.longitude, location.accuracy, altitude, verticalAccuracy, bearing, bearingAccuracy, speed, speedAccuracy)
        val pointId = pointDAO.insert(point)

        if (lastPoint == null) {

            // This is the first point in the line
            line.startedAt = location.time
            line.endedAt = location.time
            lineDAO.update(line)

            if (track != null) {
                val lineCount = lineDAO.countLinesInTrack(track.id)
                if (lineCount == 1L) {
                    // If this is the first point in the first line, also update the track's started at
                    track.startedAt = location.time
                }
                track.endedAt = location.time
                trackDAO.update(track)
            }

        } else {

            // This is an additional point in the line
            line.endedAt = location.time

            if(location.time < lastPoint.time) {
                throw Exception("Cannot insert location because it's timestamp is before that of the last point in the line!")
            }

            val lastLocation = Location("GPSRecording")
            lastLocation.latitude = lastPoint.latitude
            lastLocation.longitude = lastPoint.longitude
            val altitude = lastPoint.altitude
            if (altitude != null) {
                lastLocation.altitude = altitude
            }

            val distance = lastLocation.distanceTo(location)
            line.totalDistanceInMeters = line.totalDistanceInMeters + distance
            lineDAO.update(line)

            if (track != null) {
                track.endedAt = location.time
                track.totalDurationInMilliseconds = track.totalDurationInMilliseconds + (location.time - lastPoint.time)
                track.totalDistanceInMeters = track.totalDistanceInMeters + distance
                trackDAO.update(track)
            } else {
                throw Exception("Unable to update line for new point!")
            }
        }

        val result = pointDAO.getById(pointId)
        return result!!
    }

    fun addLocationToTrack(track: Track, location: Location) : Point {
        val line = lineDAO.getCurrentLineForTrack(track.id) ?: addLineToTrack(track)
        return addLocationToLine(line, location)
    }

    // These are mostly for testing purposes:

    fun countTracks() : Long {
        return trackDAO.count()
    }

    fun countLines() : Long {
        return lineDAO.count()
    }

    fun countLinesInTrack(track: Track) : Long {
        return lineDAO.countLinesInTrack(track.id)
    }

    fun countPoints() : Long {
        return pointDAO.count()
    }

    fun countPointsInLine(line: Line) : Long {
        return pointDAO.countPointsInLine(line.id)
    }

}
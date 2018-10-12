package com.salidasoftware.gpsrecording.room

import android.content.Context
import android.content.Intent

import android.location.Location
import android.os.Build
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import java.io.FileWriter
import java.io.IOException
import android.content.Intent.ACTION_SEND
import android.content.SharedPreferences

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.Room
import com.google.android.gms.wearable.Asset
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.ArrayList

class GPSRecordingStore(context: Context, inMemory: Boolean = false) {

    val context = context
    val database: GPSRecordingDatabase
    val prefs: SharedPreferences
    val currentTrackDeletedHandlers : ArrayList<CurrentTrackDeletedHandler> = ArrayList()

    init {
        if(inMemory) {
            database = Room.inMemoryDatabaseBuilder(context, GPSRecordingDatabase::class.java).allowMainThreadQueries().build()
        } else {
            database = Room.databaseBuilder(context, GPSRecordingDatabase::class.java, "gps-recording-database").build()
        }
        prefs = context.getSharedPreferences("GPSRecording", Context.MODE_PRIVATE)
    }

    var currentTrackId: Long
        get() = prefs.getLong("current_track_id", -1)
        set(value) {
            val editor = prefs.edit()
            editor.putLong("current_track_id", value)
            editor.commit()
        }

    var distanceFilterInMeters: Float
        get() = prefs.getFloat("distance_filter_in_meters", 10f)
        set(value) {
            val editor = prefs.edit()
            editor.putFloat("distance_filter_in_meters", value)
            editor.commit()
        }

    var timeFilterInMilliseconds: Long
        get() = prefs.getLong("time_filter_in_milliseconds", 1000L)
        set(value) {
            val editor = prefs.edit()
            editor.putLong("time_filter_in_milliseconds", value)
            editor.commit()
        }

    var displayMetricUnits: Boolean
        get() = prefs.getBoolean("display_metric_units", false)
        set(value) {
            val editor = prefs.edit()
            editor.putBoolean("display_metric_units", value)
            editor.commit()
        }

    val trackDAO = database.trackDAO()
    val lineDAO = database.lineDAO()
    val pointDAO = database.pointDAO()

    fun createTrack(name: String, note: String, activity: String) : Track {
        val track = Track(name, note, activity)
        val trackId = trackDAO.insert(track)
        val result = trackDAO.getById(trackId)
        return result!!
    }

    fun createTrack() : Track {
        val name = SimpleDateFormat("M/dd/yyyy hh:mm:ss").format(Date())
        return createTrack(name, "", "")
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

    fun addCurrentTrackDeletedHandler(handler: CurrentTrackDeletedHandler) {
        currentTrackDeletedHandlers.add(handler)
    }

    fun removeCurrentTrackDeletedHandler(handler: CurrentTrackDeletedHandler) {
        currentTrackDeletedHandlers.remove(handler)
    }

    fun deleteTrack(track: Track) {
        val trackId = track.id
        trackDAO.delete(track)
        if (trackId == currentTrackId) {
            currentTrackId = -1
            // Let TrackViewModel know about this update
            for(currentTrackDeletedHandler in currentTrackDeletedHandlers) {
                currentTrackDeletedHandler.onCurrentTrackDeleted()
            }
        }
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

    @Throws(Exception::class)
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

    private fun getExportFilenameBase(track: Track): String {
        var basename = track.name
        basename = basename.replace("[^A-Za-z0-9]".toRegex(), "_")
        return if (basename.length > 0 && basename != "_") {
            basename
        } else "track_" + track.id
    }

    @Throws(IOException::class)
    fun emailGpx(track: Track, context: Context) {
        val fileLocation = this.exportGpx(track, context)
        fileLocation.deleteOnExit()
        emailAttachment(context, fileLocation, "application/gpx+xml", "Track GPX File")
    }

    @Throws(IOException::class)
    fun emailGeoJson(track: Track, context: Context) {
        val fileLocation = this.exportGeoJson(track, context)
        fileLocation.deleteOnExit()
        emailAttachment(context, fileLocation, "application/json", "Track GeoJSON File")
    }

    private fun emailAttachment(context: Context, filelocation: File, mime: String, subject: String) {
        val emailIntent = Intent(ACTION_SEND)

        if(Build.VERSION.SDK_INT > 24) {
            // https://developer.android.com/reference/android/support/v4/content/FileProvider#SpecifyFiles
            // and
            // https://android--code.blogspot.com/2018/07/android-kotlin-file-provider-image.html

            val path = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", filelocation)
            emailIntent.putExtra(Intent.EXTRA_STREAM, path)
        } else {
            val path = Uri.fromFile(filelocation)
            emailIntent.putExtra(Intent.EXTRA_STREAM, path)
        }

        emailIntent.setType(mime)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(emailIntent, "Share track..."))
    }

    fun createTrackFromUpstreamAsset(asset: Asset, nodeId: String) : Track {

        val trackJson = JSONObject(String(asset.data))

        val name = trackJson.getString("name")
        val note = trackJson.getString("note")
        val activity = trackJson.getString("activity")
        val track = Track(name, note, activity)

        track.totalDistanceInMeters = trackJson.getDouble("totalDistanceInMeters").toFloat()
        track.totalDurationInMilliseconds = trackJson.getLong("totalDurationInMilliseconds")
        track.startedAt = trackJson.getLong("startedAt")
        track.endedAt = trackJson.getLong("endedAt")

        track.upstreamId = nodeId + ":" + trackJson.getLong("id")

        val trackId = trackDAO.insert(track)

        val linesJson = trackJson.getJSONArray("lines")

        for (i in 0..(linesJson.length() - 1)) {
            val lineJson = linesJson.getJSONObject(i)

            val line = Line(trackId)
            line.totalDistanceInMeters = lineJson.getDouble("totalDistanceInMeters").toFloat()
            line.startedAt = lineJson.getLong("startedAt")
            line.endedAt = lineJson.getLong("endedAt")
            val lineId = lineDAO.insert(line)

            val pointsJson = lineJson.getJSONArray("points")
            for (j in 0..(pointsJson.length() - 1)) {
                val pointJson = pointsJson.getJSONObject(j)

                val time = pointJson.getLong("time")
                val latitude = pointJson.getDouble("latitude")
                val longitude = pointJson.getDouble("longitude")
                val accuracy = pointJson.getDouble("accuracy").toFloat()
                val altitude = pointJson.getDouble("altitude")
                val verticalAccuracy = pointJson.getDouble("verticalAccuracy").toFloat()
                val bearing = pointJson.getDouble("bearing").toFloat()
                val bearingAccuracy = pointJson.getDouble("bearingAccuracy").toFloat()
                val speed = pointJson.getDouble("speed").toFloat()
                val speedAccuracy = pointJson.getDouble("speedAccuracy").toFloat()

                val point = Point(lineId, time, latitude, longitude, accuracy, altitude, verticalAccuracy, bearing, bearingAccuracy, speed, speedAccuracy)
                pointDAO.insert(point)

            }

        }

        val result = trackDAO.getById(trackId)
        return result!!
    }

    fun trackAsAsset(track: Track) : Asset {
        val trackJson = JSONObject()
        trackJson.put("name", track.name)
        trackJson.put("note", track.note)
        trackJson.put("activity", track.activity)
        trackJson.put("totalDistanceInMeters", track.totalDistanceInMeters)
        trackJson.put("totalDurationInMilliseconds", track.totalDurationInMilliseconds)
        trackJson.put("startedAt", track.startedAt)
        trackJson.put("endedAt", track.endedAt)
        trackJson.put("upstreamId", track.upstreamId)
        trackJson.put("downstreamId", track.downstreamId)
        trackJson.put("id", track.id)

        val linesJson = JSONArray()

        val lines = lineDAO.getAllForTrack(track.id)
        for (line in lines) {
            val lineJson = JSONObject()
            lineJson.put("trackId", line.trackId)
            lineJson.put("totalDistanceInMeters", line.totalDistanceInMeters)
            lineJson.put("startedAt", line.startedAt)
            lineJson.put("endedAt", line.endedAt)
            lineJson.put("id", line.id)

            val pointsJson = JSONArray()

            val points = pointDAO.getAllForLine(line.id)
            for (point in points) {
                val pointJson = JSONObject()
                pointJson.put("lineId", point.lineId)
                pointJson.put("time", point.time)
                pointJson.put("latitude", point.latitude)
                pointJson.put("longitude", point.longitude)
                pointJson.put("horizontal_accuracy", point.horizontal_accuracy)
                pointJson.put("altitude", point.altitude)
                pointJson.put("vertical_accuracy", point.vertical_accuracy)
                pointJson.put("bearing", point.bearing)
                pointJson.put("bearing_accuracy", point.bearing_accuracy)
                pointJson.put("speed", point.speed)
                pointJson.put("speed_accuracy", point.speed_accuracy)
                pointJson.put("id", point.id)
                pointsJson.put(pointJson)
            }
            lineJson.put("points", pointsJson)

            linesJson.put(lineJson)
        }

        trackJson.put("lines", linesJson)

        val bytes = trackJson.toString().toByteArray(Charsets.UTF_8)
        return Asset.createFromBytes(bytes)
    }


    @Throws(IOException::class)
    fun exportGpx(track: Track, context: Context): File {

        val filename = getExportFilenameBase(track) + ".gpx"
        val filelocation = File(context.cacheDir, filename) //File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), filename)
        filelocation.getParentFile().mkdirs()

        val fw = FileWriter(filelocation)
        this.writeGpx(track, fw)
        fw.close()
        return filelocation
    }

    @Throws(IOException::class)
    fun exportGeoJson(track: Track, context: Context): File {

        val filename = getExportFilenameBase(track) + ".json"
        val filelocation = File(context.cacheDir, filename) //File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), filename)
        filelocation.getParentFile().mkdirs()

        val fw = FileWriter(filelocation)
        this.writeGeoJSON(track, fw)
        fw.close()
        return filelocation
    }

    private val pointDateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    fun writeGpx(track: Track, out: Writer) {
        val gpx_header = "<?xml version=\"1.0\" standalone=\"yes\"?>"
        val gpx_open = ("<gpx"
                + " xmlns=\"http://www.topografix.com/GPX/1/1\""
                + " version=\"1.1\" creator=\"gomaps.us LocationTools 1.0\""
                + " xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.topografix.com/GPX/gpx_style/0/2 http://www.topografix.com/GPX/gpx_style/0/2/gpx_style.xsd\">")

        out.write(gpx_header + "\n")

        //open gpx
        out.write(gpx_open + "\n")
        out.write("\t<trk>\n");
        out.write("\t\t<name>" + track.name + "</name>\n");
        out.write("\t\t<src>com.salidasoftware.gpsrecording</src>\n");
        out.write("\t\t<extensions>\n");
        out.write("\t\t\t<line xmlns=\"http://www.topografix.com/GPX/gpx_style/0/2\">\n");
        out.write("\t\t\t\t<color>ff0000</color>\n");
        out.write("\t\t\t\t<opacity>0.78</opacity>\n");
        out.write("\t\t\t\t<width>0.6000</width>\n");
        out.write("\t\t\t\t<pattern>Solid</pattern>\n");
        out.write("\t\t\t</line>\n");
        out.write("\t\t</extensions>\n");

        val lines = lineDAO.getAllForTrack(track.id)
        for (line in lines) {

            val points = pointDAO.getAllForLine(line.id)
            if(points.size > 1) {
                out.write("\t\t<trkseg>\n");

                for (point in points) {
                    out.write("\t\t\t" + "<trkpt lat=\"" + point.latitude + "\" " + "lon=\"" + point.longitude + "\">");
                    out.write("<ele>" + point.altitude + "</ele>");
                    out.write("<time>" + pointDateFormatter.format(Date(point.time)) + "</time>");
                    out.write("<speed>" + point.speed + "</speed>");
                    out.write("<course>" + point.bearing + "</course>");
                    out.write("</trkpt>\n");
                }

                out.write("\t\t" + "</trkseg>" + "\n");
            }
        }

        out.write("\t</trk>\n")

        //close gpx
        out.write("</gpx>");
    }

    fun writeGeoJSON(track: Track, out: Writer) {
        out.write("{\n")
        out.write("  \"type\": \"FeatureCollection\",\n")
        out.write("  \"features\": [\n")
        out.write("    {\n")
        out.write("      \"type\": \"Feature\",\n")
        out.write("      \"properties\": {")
        out.write("        \"name\":\""+track.name+"\",")
        out.write("        \"note\":\""+track.note+"\",")
        out.write("        \"activity\":\""+track.activity+"\",")
        out.write("        \"totalDistanceInMeters\":"+track.totalDistanceInMeters+",")
        out.write("        \"totalDurationInMilliseconds\":"+track.totalDurationInMilliseconds+",")
        out.write("        \"startedAt\":\""+pointDateFormatter.format(Date(track.startedAt))+"\",")
        out.write("        \"endedAt\":\""+pointDateFormatter.format(Date(track.endedAt))+"\",")
        out.write("      },\n")
        out.write("      \"geometry\": {\n")
        out.write("        \"type\": \"MultiLineString\",\n")
        out.write("        \"coordinates\": [\n")

        val lines = lineDAO.getAllForTrack(track.id)
        var lineIndex = 0
        for (line in lines) {
            val points = pointDAO.getAllForLine(line.id)
            if(points.size > 1) {
                var pointIndex = 0
                if (lineIndex > 0) {
                    out.write("          ,\n")
                }
                out.write("          [\n")

                for (point in points) {
                    out.write("            [\n")
                    out.write("              "+point.longitude+",\n")
                    out.write("              "+point.latitude+"\n")
                    if (pointIndex >= points.size - 1) {
                        // No comma for last coord
                        out.write("            ]\n")
                    } else {
                        out.write("            ],\n")
                    }
                    pointIndex++
                }

                out.write("          ]\n")

            }
            lineIndex++
        }

        out.write("        ]\n")
        out.write("      }\n")
        out.write("    }\n")
        out.write("  ]\n")
        out.write("}\n")
    }

}
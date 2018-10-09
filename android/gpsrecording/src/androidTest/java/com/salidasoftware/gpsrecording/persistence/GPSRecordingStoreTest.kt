package com.salidasoftware.gpsrecording.persistence

import androidx.room.Room
import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import android.util.Log
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import android.location.Location
import com.salidasoftware.gpsrecording.room.GPSRecordingDatabase
import com.salidasoftware.gpsrecording.room.GPSRecordingStore
import com.salidasoftware.gpsrecording.room.Track

@RunWith(AndroidJUnit4::class)
class GPSRecordingStoreTest {

    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    private lateinit var database: GPSRecordingDatabase

    @Before
    fun setup() {
        val context: Context = InstrumentationRegistry.getTargetContext()
        try {
            database = Room.inMemoryDatabaseBuilder(context, GPSRecordingDatabase::class.java).allowMainThreadQueries().build()
        } catch (e: Exception) {
            Log.i("test", e.message)
        }

    }

    @Test
    fun testInit() {
        val store = GPSRecordingStore(InstrumentationRegistry.getTargetContext(), true)
        Assert.assertEquals(0, store.countTracks())
        Assert.assertEquals(0, store.countLines())
        Assert.assertEquals(0, store.countPoints())
    }

    @Test
    fun testCreateAndGetTrack() {
        val store = GPSRecordingStore(InstrumentationRegistry.getTargetContext(), true)
        val track = store.createTrack("Test Create", "Test", Track.Activity.RUN.activityName)
        Assert.assertTrue(track.id > 0)
        var fetched = store.getTrack(track.id)
        if (fetched != null) {
            Assert.assertEquals(track.name, fetched.name)
        } else {
            Assert.fail("Got an unexpected null track")
        }
    }

    @Test
    fun testDownstreamId() {
        val store = GPSRecordingStore(InstrumentationRegistry.getTargetContext(), true)
        val track = store.createTrack("Test Up Down", "Test", Track.Activity.RUN.activityName)
        store.setTrackDownstreamId(track, "abcd1234")
        var fetched = store.getTrack(track.id)
        if (fetched != null) {
            Assert.assertEquals("abcd1234", fetched.downstreamId)
        }
        else {
            Assert.fail("Got an unexpected null track")
        }
    }

    @Test
    fun testUpstreamId() {
        val store = GPSRecordingStore(InstrumentationRegistry.getTargetContext(), true)
        val track = store.createTrack("Test Up Down", "Test", Track.Activity.RUN.activityName)

        track.upstreamId = "upup1234"
        store.saveTrack(track)

        var fetched = store.getTrackWithUpstreamId("upup1234")
        if (fetched != null) {
            Assert.assertEquals("upup1234", fetched.upstreamId)
        }
        else {
            Assert.fail("Got an unexpected null track")
        }
    }

    @Test
    fun testUpdateTrack() {
        val store = GPSRecordingStore(InstrumentationRegistry.getTargetContext(), true)
        val track = store.createTrack("Test Update A", "Test A", Track.Activity.RUN.activityName)

        store.updateTrack(track,"Test Update B", "Test B", Track.Activity.BIKE.activityName)

        var fetched = store.getTrack(track.id)
        if (fetched != null) {
            Assert.assertEquals("Test Update B", fetched.name)
            Assert.assertEquals("Test B", fetched.note)
            Assert.assertEquals("bike", fetched.activity)
        }
        else {
            Assert.fail("Got an unexpected null track")
        }
    }

    @Test
    fun testDeleteTrack() {
        val store = GPSRecordingStore(InstrumentationRegistry.getTargetContext(), true)
        val track = store.createTrack("Test Delete", "Test", Track.Activity.RUN.activityName)

        store.deleteTrack(track)

        val fetched = store.getTrack(track.id)
        Assert.assertNull(fetched)
    }

    @Test
    fun addLine() {
        val store = GPSRecordingStore(InstrumentationRegistry.getTargetContext(), true)
        val track = store.createTrack("Test Add Line", "Test", Track.Activity.RUN.activityName)
        val line = store.addLineToTrack(track)
        val fetched = store.getLine(line.id)
        Assert.assertNotNull(fetched)
        val lineCount = store.countLinesInTrack(track)
        Assert.assertEquals(1, lineCount)
    }

    @Test
    fun testAddLocation() {
        val store = GPSRecordingStore(InstrumentationRegistry.getTargetContext(), true)
        val track = store.createTrack("Test Add Location", "Test", Track.Activity.RUN.activityName)

        val locationA = Location("Test")
        locationA.latitude = 38.5
        locationA.longitude = -106.0
        locationA.time = 1535081977519

        val locationB = Location("Test")
        locationB.latitude = 38.51
        locationB.longitude = -106.01
        locationB.time = 1535081978519

        val pointA = store.addLocationToTrack(track, locationA)

        Assert.assertEquals(38.5, pointA.latitude, 0.01)
        Assert.assertEquals(-106.0, pointA.longitude, 0.01)
        Assert.assertEquals(1535081977519, pointA.time)

        Assert.assertEquals(0.0F, track.totalDistanceInMeters)
        Assert.assertEquals(0L, track.totalDurationInMilliseconds)

        val pointB = store.addLocationToTrack(track, locationB)

        Assert.assertEquals(38.51, pointB.latitude, 0.01)
        Assert.assertEquals(-106.01, pointB.longitude, 0.01)
        Assert.assertEquals(1535081978519, pointB.time)

        val line = store.getLine(pointB.lineId)
        if (line != null) {
            val pointCount = store.countPointsInLine(line)
            Assert.assertEquals(2, pointCount)

            var lineCount = store.countLinesInTrack(track)
            Assert.assertEquals(1, lineCount)

            Assert.assertEquals(1411.7665F, line.totalDistanceInMeters)

            val fetchedTrack = store.getTrack(track.id)
            if(fetchedTrack != null) {
                Assert.assertEquals(1411.7665F, fetchedTrack.totalDistanceInMeters)
                Assert.assertEquals(1000L, fetchedTrack.totalDurationInMilliseconds)
            } else {
                Assert.fail("Got an unexpected null track")
            }
        } else {
            Assert.fail("Got an unexpected null line")
        }

    }

    @After
    fun tearDown() {
        database.close()
    }



}
package com.salidasoftware.gpsrecording

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.salidasoftware.gpsrecording.room.*

@RunWith(AndroidJUnit4::class)
class LineDAOTest {

    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    private lateinit var database: GPSRecordingDatabase
    private lateinit var trackDAO: TrackDAO
    private lateinit var lineDAO: LineDAO

    @Before
    fun setup() {
        val context: Context = InstrumentationRegistry.getTargetContext()
        try {
            database = Room.inMemoryDatabaseBuilder(context, GPSRecordingDatabase::class.java).allowMainThreadQueries().build()
        } catch (e: Exception) {
            Log.i("test", e.message)
        }
        trackDAO = database.trackDAO()
        lineDAO = database.lineDAO()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAddingAndRetrievingData() {

        val track = Track("Test Lines 1")
        val trackId = trackDAO.insert(track)

        val beforeCount = lineDAO.getAllForTrack(trackId)

        val line = Line(trackId, 1.1f)
        lineDAO.insert(line)

        val afterCount = lineDAO.getAllForTrack(trackId)
        val sizeDifference = afterCount.size - beforeCount.size
        Assert.assertEquals(1, sizeDifference)
        val retrievedLine = afterCount.last()
        Assert.assertEquals(1.1f, retrievedLine.totalDistanceInMeters)
    }

    @Test
    fun testDeleteTrackCascade() {

        val track = Track("Test Lines 2")
        val trackId = trackDAO.insert(track)

        val line = Line(trackId, 1.1f)
        lineDAO.insert(line)

        val beforeCount = lineDAO.count()

        val found = trackDAO.getById(trackId)
        if (found != null) {
            trackDAO.delete(found)
        } else {
            Assert.fail("Got an unexpected null track!")
        }

        val afterCount = lineDAO.count()

        val sizeDifference = afterCount - beforeCount
        Assert.assertEquals(-1, sizeDifference)
    }

}
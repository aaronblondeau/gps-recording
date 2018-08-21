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

@RunWith(AndroidJUnit4::class)
class PointDAOTest {

    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    private lateinit var database: GPSRecordingDatabase
    private lateinit var trackDAO: TrackDAO
    private lateinit var lineDAO: LineDAO
    private lateinit var pointDAO: PointDAO

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
        pointDAO = database.pointDAO()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAddingAndRetrievingData() {

        val track = Track("Test Points 1")
        val trackId = trackDAO.insert(track)

        val line = Line(trackId, 1.1f)
        val lineId = lineDAO.insert(line)

        val beforeCount = pointDAO.getAllForLine(lineId)

        val point1 = Point(lineId, System.currentTimeMillis(), 38.51, -106.01, 0.0f, 0.0, 0.0f, 0.0f, 0)
        val point2 = Point(lineId, System.currentTimeMillis() + 1, 38.52, -106.02, 0.0f, 0.0, 0.0f, 0.0f, 0)

        pointDAO.insert(point1)
        pointDAO.insert(point2)

        val afterCount = pointDAO.getAllForLine(lineId)
        val sizeDifference = afterCount.size - beforeCount.size
        Assert.assertEquals(2, sizeDifference)
        Assert.assertEquals(38.51, afterCount[0].latitude, 0.01)
    }

    @Test
    fun testDeleteTrackCascade() {

        val track = Track("Test Points 2")
        val trackId = trackDAO.insert(track)

        val line = Line(trackId, 1.1f)
        val lineId = lineDAO.insert(line)

        val point1 = Point(lineId, System.currentTimeMillis(), 38.51, -106.01, 0.0f, 0.0, 0.0f, 0.0f, 0)
        val point2 = Point(lineId, System.currentTimeMillis() + 1, 38.52, -106.02, 0.0f, 0.0, 0.0f, 0.0f, 0)

        pointDAO.insert(point1)
        pointDAO.insert(point2)

        val beforeCount = pointDAO.count()

        val found = trackDAO.getById(trackId)
        trackDAO.delete(found)

        val afterCount = pointDAO.count()

        val sizeDifference = afterCount - beforeCount
        Assert.assertEquals(-2, sizeDifference)
    }

}
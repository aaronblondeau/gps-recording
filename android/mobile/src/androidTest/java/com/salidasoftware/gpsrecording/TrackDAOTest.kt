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
import com.salidasoftware.gpsrecording.room.GPSRecordingDatabase
import com.salidasoftware.gpsrecording.room.Track
import com.salidasoftware.gpsrecording.room.TrackDAO

@RunWith(AndroidJUnit4::class)
class TrackDAOTest {

    @Rule
    @JvmField
    val rule: TestRule = InstantTaskExecutorRule()

    private lateinit var database: GPSRecordingDatabase
    private lateinit var trackDAO: TrackDAO

    @Before
    fun setup() {
        val context: Context = InstrumentationRegistry.getTargetContext()
        try {
            database = Room.inMemoryDatabaseBuilder(context, GPSRecordingDatabase::class.java).allowMainThreadQueries().build()
        } catch (e: Exception) {
            Log.i("test", e.message)
        }
        trackDAO = database.trackDAO()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAddingAndRetrievingData() {
        val beforeCount = trackDAO.getAll()

        val track = Track("Test 1")
        val trackId = trackDAO.insert(track)

        val afterCount = trackDAO.getAll()
        val sizeDifference = afterCount.size - beforeCount.size
        Assert.assertEquals(1, sizeDifference)
        val retrievedTrack = afterCount.last()
        Assert.assertEquals("Test 1", retrievedTrack.name)
    }

    @Test
    fun testGetById() {
        val track = Track("Test 2")
        val trackId = trackDAO.insert(track)
        val found = trackDAO.getById(trackId)
        if (found != null) {
            Assert.assertEquals("Test 2", found.name)
        } else {
            Assert.fail("Got an unexpected null track!")
        }
    }

    @Test
    fun testUpdateTrack() {
        val track = Track("Test 3")
        val trackId = trackDAO.insert(track)
        val found = trackDAO.getById(trackId)

        if (found != null) {
            found.name = "Test 3 EDITED"
            found.note = "I have been edited!"
            trackDAO.update(found)
        } else {
            Assert.fail("Got an unexpected null track!")
        }

        val found2 = trackDAO.getById(trackId)

        if (found2 != null) {
            Assert.assertEquals("Test 3 EDITED", found2.name)
            Assert.assertEquals("I have been edited!", found2.note)
        } else {
            Assert.fail("Got an unexpected null track!")
        }
    }

    @Test
    fun testDeleteTrack() {
        val track = Track("Test 4")
        val trackId = trackDAO.insert(track)
        val found = trackDAO.getById(trackId)

        val beforeCount = trackDAO.getAll()

        if (found != null) {
            trackDAO.delete(found)
        } else {
            Assert.fail("Got an unexpected null track!")
        }

        val afterCount = trackDAO.getAll()
        val sizeDifference = afterCount.size - beforeCount.size
        Assert.assertEquals(-1, sizeDifference)
    }

}
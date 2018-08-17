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
        trackDAO.insert(track)

        val afterCount = trackDAO.getAll()
        val sizeDifference = afterCount.size - beforeCount.size
        Assert.assertEquals(1, sizeDifference)
        val retrievedTrack = afterCount.last()
        Assert.assertEquals("Test 1", retrievedTrack.name)
    }

}
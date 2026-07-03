package com.example.myapplication

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.SensorDao
import com.example.myapplication.data.SensorReading
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SensorDaoTest {
    private lateinit var sensorDao: SensorDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        sensorDao = db.sensorDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeReadingAndReadInList() = runBlocking {
        val reading = SensorReading(
            sensorName = "TestSensor",
            valueX = 1.0f,
            timestamp = System.currentTimeMillis()
        )
        sensorDao.insert(reading)
        val unsynced = sensorDao.getUnsyncedReadings()
        assertEquals(unsynced[0].sensorName, "TestSensor")
    }
}

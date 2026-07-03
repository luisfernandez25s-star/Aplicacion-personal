package com.example.myapplication.wear

import com.example.myapplication.wear.data.model.SensorData
import com.example.myapplication.wear.data.model.Vector3
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorDataTest {

    private val gson = Gson()

    @Test
    fun `test sensor data json serialization`() {
        val data = SensorData(
            timestamp = 123456789L,
            heartRate = 75f,
            accelerometer = Vector3(1.1f, 2.2f, 3.3f),
            gyroscope = Vector3(0.1f, 0.2f, 0.3f)
        )

        val json = gson.toJson(data)
        val deserialized = gson.fromJson(json, SensorData::class.java)

        assertEquals(data.timestamp, deserialized.timestamp)
        assertEquals(data.heartRate, deserialized.heartRate)
        assertEquals(data.accelerometer.x, deserialized.accelerometer.x)
    }
}

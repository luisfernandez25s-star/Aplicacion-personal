package com.example.myapplication

import com.example.myapplication.data.local.SensorEntity
import com.example.myapplication.data.model.SensorData
import com.example.myapplication.data.model.Vector3
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorDataTest {

    @Test
    fun `test sensor data to entity conversion`() {
        val data = SensorData(
            timestamp = 123456789L,
            heartRate = 75f,
            accelerometer = Vector3(1f, 2f, 3f),
            gyroscope = Vector3(4f, 5f, 6f),
            watchName = "Pixel Watch",
            battery = 90
        )

        val entity = SensorEntity.fromSensorData(data)

        assertEquals(data.timestamp, entity.timestamp)
        assertEquals(data.heartRate, entity.heartRate)
        assertEquals(data.accelerometer.x, entity.accelX)
        assertEquals(data.gyroscope.z, entity.gyroZ)
        assertEquals(data.watchName, entity.watchName)
    }

    @Test
    fun `test entity to sensor data conversion`() {
        val entity = SensorEntity(
            timestamp = 987654321L,
            heartRate = 80f,
            accelX = 0.1f, accelY = 0.2f, accelZ = 0.3f,
            gyroX = 0.4f, gyroY = 0.5f, gyroZ = 0.6f,
            watchName = "Galaxy Watch",
            battery = 85
        )

        val data = entity.toSensorData()

        assertEquals(entity.timestamp, data.timestamp)
        assertEquals(entity.heartRate, data.heartRate)
        assertEquals(entity.accelY, data.accelerometer.y)
        assertEquals(entity.gyroX, data.gyroscope.x)
    }
}

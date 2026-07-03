package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myapplication.data.model.SensorData
import com.example.myapplication.data.model.Vector3

@Entity(tableName = "sensor_queue")
data class SensorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val heartRate: Float,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val watchName: String?,
    val battery: Int?
) {
    fun toSensorData() = SensorData(
        timestamp = timestamp,
        heartRate = heartRate,
        accelerometer = Vector3(accelX, accelY, accelZ),
        gyroscope = Vector3(gyroX, gyroY, gyroZ),
        watchName = watchName,
        battery = battery
    )

    companion object {
        fun fromSensorData(data: SensorData) = SensorEntity(
            timestamp = data.timestamp,
            heartRate = data.heartRate,
            accelX = data.accelerometer.x,
            accelY = data.accelerometer.y,
            accelZ = data.accelerometer.z,
            gyroX = data.gyroscope.x,
            gyroY = data.gyroscope.y,
            gyroZ = data.gyroscope.z,
            watchName = data.watchName,
            battery = data.battery
        )
    }
}

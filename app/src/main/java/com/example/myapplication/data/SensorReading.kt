package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_readings")
data class SensorReading(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sensorName: String,
    val valueX: Float,
    val valueY: Float = 0f,
    val valueZ: Float = 0f,
    val timestamp: Long,
    val isSynced: Boolean = false // Nuevo campo para control de duplicados
)

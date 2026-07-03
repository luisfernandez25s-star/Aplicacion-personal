package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    @Insert
    suspend fun insert(reading: SensorReading): Long

    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<SensorReading>>

    @Query("SELECT * FROM sensor_readings WHERE isSynced = 0 ORDER BY timestamp DESC")
    suspend fun getUnsyncedReadings(): List<SensorReading>

    @Query("UPDATE sensor_readings SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC LIMIT 50")
    suspend fun getLastReadings(): List<SensorReading>

    @Query("DELETE FROM sensor_readings")
    suspend fun deleteAll()
}

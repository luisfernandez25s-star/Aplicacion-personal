package com.example.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SensorEntity)

    @Query("SELECT * FROM sensor_queue ORDER BY timestamp ASC")
    fun getAll(): Flow<List<SensorEntity>>

    @Delete
    suspend fun delete(entity: SensorEntity)

    @Query("SELECT COUNT(*) FROM sensor_queue")
    suspend fun getCount(): Int
}

package com.example.myapplication.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.MongoDBManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val readings = db.sensorDao().getUnsyncedReadings()

            if (readings.isEmpty()) return@withContext Result.success()

            val manager = MongoDBManager.getInstance()
            var allSuccess = true

            readings.forEach { reading ->
                try {
                    manager.saveReading(reading)
                    db.sensorDao().markAsSynced(reading.id)
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Error al sincronizar ${reading.id}: ${e.message}")
                    allSuccess = false
                }
            }

            if (allSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error fatal en worker: ${e.message}")
            Result.retry()
        }
    }
}

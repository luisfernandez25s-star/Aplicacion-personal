package com.example.myapplication.service

import android.util.Log
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.MongoDBManager
import com.example.myapplication.data.SensorReading
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearableService : WearableListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("WearableService", "Evento de datos recibido: ${dataEvents.count} eventos")
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (path == "/sensor_data") {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val sensorName = dataMap.getString("sensor_name") ?: "Unknown"
                    val valX = dataMap.getFloat("value_x")
                    val valY = dataMap.getFloat("value_y")
                    val valZ = dataMap.getFloat("value_z")
                    val timestamp = dataMap.getLong("timestamp")

                    saveToDatabase(sensorName, valX, valY, valZ, timestamp)
                }
            }
        }
    }

    private fun saveToDatabase(name: String, valX: Float, valY: Float, valZ: Float, timestamp: Long) {
        serviceScope.launch {
            val reading = SensorReading(
                sensorName = name,
                valueX = valX,
                valueY = valY,
                valueZ = valZ,
                timestamp = timestamp
            )
            
            // Guardar localmente en Room primero para asegurar que la UI vea algo
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                database.sensorDao().insert(reading)
                Log.d("WearableService", "Dato guardado localmente en Room")
            } catch (e: Exception) {
                Log.e("WearableService", "Error guardando en Room: ${e.message}")
            }

            // Guardar en la nube (MongoDB Atlas)
            try {
                MongoDBManager.getInstance().saveReading(reading)
            } catch (e: Exception) {
                Log.e("WearableService", "Error guardando en Atlas: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

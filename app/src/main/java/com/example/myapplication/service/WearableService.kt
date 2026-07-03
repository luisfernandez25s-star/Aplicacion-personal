package com.example.myapplication.service

import android.util.Log
import com.example.myapplication.data.AppDatabase
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
            
            // Guardar localmente en Room para que el historial se mantenga
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                database.sensorDao().insert(reading)
                Log.d("WearableService", "Dato guardado localmente: $name")
            } catch (e: Exception) {
                Log.e("WearableService", "Error local: ${e.message}")
            }

            // NOTA: Ya NO se guarda en Atlas automáticamente aquí.
            // Se guarda solo cuando el usuario pulsa el botón en FirstFragment.
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

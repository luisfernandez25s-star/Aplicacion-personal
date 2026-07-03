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
        for (event in dataEvents) {
            val path = event.dataItem.uri.path ?: ""
            
            if (event.type == DataEvent.TYPE_CHANGED && path == "/sensor_data") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                
                val hr = dataMap.getFloat("hr")
                val ax = dataMap.getFloat("ax")
                val ay = dataMap.getFloat("ay")
                val az = dataMap.getFloat("az")
                val gx = dataMap.getFloat("gx")
                val gy = dataMap.getFloat("gy")
                val gz = dataMap.getFloat("gz")
                val timestamp = dataMap.getLong("timestamp")

                Log.d("WearableService", "Datos recibidos. Guardando LOCALMENTE.")

                // Solo guardamos localmente en Room. 
                // El envío a Atlas lo hará el usuario desde el botón en el celular.
                saveLocal("Ritmo Cardiaco", hr, 0f, 0f, timestamp)
                saveLocal("Acelerómetro", ax, ay, az, timestamp)
                saveLocal("Giroscopio", gx, gy, gz, timestamp)
            }
        }
    }

    private fun saveLocal(name: String, valX: Float, valY: Float, valZ: Float, timestamp: Long) {
        serviceScope.launch {
            try {
                val reading = SensorReading(
                    sensorName = name,
                    valueX = valX,
                    valueY = valY,
                    valueZ = valZ,
                    timestamp = timestamp
                )
                AppDatabase.getDatabase(applicationContext).sensorDao().insert(reading)
            } catch (e: Exception) {
                Log.e("WearableService", "Error local: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

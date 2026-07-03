package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.BuildConfig
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.Document
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MongoDBManager private constructor() {
    private var client: MongoClient? = null

    private fun getClient(): MongoClient {
        val currentClient = client
        if (currentClient != null) return currentClient

        Log.d("Atlas", "Creando nuevo cliente MongoDB...")
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(BuildConfig.MONGODB_URI))
            .applyToClusterSettings { builder ->
                builder.serverSelectionTimeout(15, TimeUnit.SECONDS)
            }
            .applyToSocketSettings { builder ->
                builder.connectTimeout(15, TimeUnit.SECONDS)
                builder.readTimeout(15, TimeUnit.SECONDS)
            }
            .build()
        
        val newClient = MongoClient.create(settings)
        client = newClient
        return newClient
    }

    suspend fun saveReadingsBulk(readings: List<SensorReading>): Boolean {
        if (readings.isEmpty()) return true
        
        try {
            val mongoClient = getClient()
            val database = mongoClient.getDatabase("Sensores")
            val collection = database.getCollection<Document>("Sensores")

            val documents = readings.map { reading ->
                Document()
                    .append("sensor", reading.sensorName)
                    .append("valor_x", reading.valueX.toDouble())
                    .append("valor_y", reading.valueY.toDouble())
                    .append("valor_z", reading.valueZ.toDouble())
                    .append("timestamp", reading.timestamp)
                    .append("fecha_registro", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(reading.timestamp)))
                    .append("dispositivo", "Android_App")
            }

            Log.d("Atlas", "Intentando insertar ${documents.size} documentos...")
            collection.insertMany(documents)
            Log.d("Atlas", "✅ Sincronización exitosa con Atlas")
            return true
        } catch (e: Exception) {
            Log.e("Atlas", "❌ Error en saveReadingsBulk: ${e.message}", e)
            client = null // Resetear cliente para el próximo intento
            throw e // Lanzar para que el Fragment muestre el error real
        }
    }

    suspend fun saveReading(reading: SensorReading) = saveReadingsBulk(listOf(reading))

    companion object {
        @Volatile private var INSTANCE: MongoDBManager? = null
        fun getInstance(): MongoDBManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: MongoDBManager().also { INSTANCE = it }
        }
    }
}

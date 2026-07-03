package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.BuildConfig
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.Document
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MongoDBManager private constructor() {
    private var client: MongoClient? = null
    private var database: MongoDatabase? = null

    private fun getDatabase(): MongoDatabase? {
        if (client == null) {
            try {
                Log.d("Atlas", "Conectando a: ${BuildConfig.MONGODB_URI}")
                client = MongoClient.create(BuildConfig.MONGODB_URI)
                // Nombre exacto de la base de datos: Sensores
                database = client?.getDatabase("Sensores")
            } catch (e: Exception) {
                Log.e("Atlas", "Error de cliente: ${e.message}")
            }
        }
        return database
    }

    suspend fun saveReading(reading: SensorReading) {
        try {
            val db = getDatabase() ?: throw Exception("No se pudo conectar a la DB")
            // Nombre exacto de la colección: Sensores
            val collection = db.getCollection<Document>("Sensores")

            val doc = Document()
                .append("sensor", reading.sensorName)
                .append("x", reading.valueX.toDouble())
                .append("y", reading.valueY.toDouble())
                .append("z", reading.valueZ.toDouble())
                .append("timestamp", reading.timestamp)
                .append("fecha", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(reading.timestamp)))

            collection.insertOne(doc)
            Log.d("Atlas", "✅ Guardado exitoso: ${reading.sensorName}")
        } catch (e: Exception) {
            Log.e("Atlas", "❌ Error al insertar: ${e.message}")
            throw e
        }
    }

    companion object {
        @Volatile private var INSTANCE: MongoDBManager? = null
        fun getInstance(): MongoDBManager = INSTANCE ?: synchronized(this) {
            MongoDBManager().also { INSTANCE = it }
        }
    }
}

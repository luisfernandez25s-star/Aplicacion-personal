package com.example.myapplication.data

import com.example.myapplication.BuildConfig
import com.mongodb.kotlin.client.coroutine.MongoClient
import org.bson.Document
import android.util.Log

class MongoDBManager {
    private val connectionString = BuildConfig.MONGODB_URI
    private val client = MongoClient.create(connectionString)
    private val database = client.getDatabase("Escuela")
    private val collection = database.getCollection<Document>("LecturasSensores")

    suspend fun saveReading(reading: SensorReading) {
        Log.d("MongoDBManager", "Intentando guardar en MongoDB: ${reading.sensorName}")
        try {
            val doc = Document()
                .append("sensorName", reading.sensorName)
                .append("x", reading.valueX.toDouble())
                .append("y", reading.valueY.toDouble())
                .append("z", reading.valueZ.toDouble())
                .append("timestamp", reading.timestamp)
            
            val result = collection.insertOne(doc)
            Log.d("MongoDBManager", "Dato enviado exitosamente a MongoDB Atlas: ${result.insertedId}")
        } catch (e: Exception) {
            Log.e("MongoDBManager", "Error al conectar o insertar en MongoDB Atlas. Verifica tu IP y la conexión a Internet.", e)
            e.printStackTrace()
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: MongoDBManager? = null

        fun getInstance(): MongoDBManager {
            return INSTANCE ?: synchronized(this) {
                val instance = MongoDBManager()
                INSTANCE = instance
                instance
            }
        }
    }
}

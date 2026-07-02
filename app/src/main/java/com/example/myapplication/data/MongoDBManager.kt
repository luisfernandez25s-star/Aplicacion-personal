package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.BuildConfig
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.Document

class MongoDBManager private constructor() {
    private val connectionString = BuildConfig.MONGODB_URI
    private var client: MongoClient? = null
    private var database: MongoDatabase? = null
    private var collection: MongoCollection<Document>? = null

    private fun init() {
        if (client == null) {
            try {
                client = MongoClient.create(connectionString)
                database = client?.getDatabase("Escuela")
                collection = database?.getCollection<Document>("LecturasSensores")
                Log.d("MongoDBManager", "Cliente MongoDB inicializado correctamente")
            } catch (e: Exception) {
                Log.e("MongoDBManager", "Error al inicializar MongoDB: ${e.message}")
            }
        }
    }

    suspend fun saveReading(reading: SensorReading) {
        init()
        Log.d("MongoDBManager", "Intentando guardar en MongoDB Atlas: ${reading.sensorName}")
        
        val coll = collection ?: run {
            Log.e("MongoDBManager", "La colección es nula, no se puede guardar")
            return
        }

        try {
            val doc = Document()
                .append("sensorName", reading.sensorName)
                .append("x", reading.valueX.toDouble())
                .append("y", reading.valueY.toDouble())
                .append("z", reading.valueZ.toDouble())
                .append("timestamp", reading.timestamp)
            
            val result = coll.insertOne(doc)
            Log.d("MongoDBManager", "Dato enviado exitosamente a MongoDB Atlas: ${result.insertedId}")
        } catch (e: Exception) {
            Log.e("MongoDBManager", "Error al insertar en MongoDB Atlas: ${e.message}", e)
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

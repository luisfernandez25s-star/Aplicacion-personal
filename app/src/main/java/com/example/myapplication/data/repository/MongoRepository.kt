package com.example.myapplication.data.repository

import com.example.myapplication.data.local.SensorDao
import com.example.myapplication.data.local.SensorEntity
import com.example.myapplication.data.manager.SettingsManager
import com.example.myapplication.data.model.SensorData
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.bson.Document
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MongoRepository @Inject constructor(
    private val sensorDao: SensorDao,
    private val settingsManager: SettingsManager
) {
    private var client: MongoClient? = null
    private var database: MongoDatabase? = null
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    enum class ConnectionStatus {
        CONNECTED, DISCONNECTED, CONNECTING, ERROR
    }

    init {
        repositoryScope.launch {
            settingsManager.mongoUriFlow.collect { uri ->
                if (!uri.isNullOrBlank()) {
                    try {
                        connect(uri)
                    } catch (e: Exception) {
                        Timber.e(e, "Connection failed during init")
                    }
                } else {
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    _errorMessage.value = "No se ha configurado la URI de MongoDB"
                }
            }
        }
        
        // Auto-sync job with better error handling
        repositoryScope.launch {
            while (isActive) {
                if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                    try {
                        syncLocalData()
                    } catch (e: Exception) {
                        Timber.w("Auto-sync failed: ${e.message}")
                        // Don't change status to ERROR here to avoid infinite reconnect loops
                        // unless it's a persistent connection error
                    }
                }
                delay(30000) // Increased delay to 30s to reduce resource usage
            }
        }
    }

    private suspend fun connect(uri: String) {
        withContext(Dispatchers.IO) {
            var newClient: MongoClient? = null
            try {
                _connectionStatus.value = ConnectionStatus.CONNECTING
                _errorMessage.value = null
                
                val maskedUri = if (uri.contains("@")) {
                    uri.substring(0, uri.indexOf("://") + 3) + "******" + uri.substring(uri.indexOf("@"))
                } else uri
                
                Timber.d("Attempting to connect to MongoDB with URI: $maskedUri")
                newClient = MongoClient.create(uri)
                val newDatabase = newClient.getDatabase("Sensores")
                
                // Validate connection with a longer timeout
                val pingResult = withTimeoutOrNull(30000) {
                    newDatabase.runCommand(Document("ping", 1))
                }
                
                if (pingResult == null) {
                    throw Exception("Timeout: No se pudo establecer conexión con el servidor en 30 segundos. Verifique su conexión a internet o la configuración del cluster.")
                }
                
                client?.close()
                client = newClient
                database = newDatabase
                
                _connectionStatus.value = ConnectionStatus.CONNECTED
                Timber.i("Connected to MongoDB Atlas")
            } catch (e: Exception) {
                newClient?.close()
                _connectionStatus.value = ConnectionStatus.ERROR
                _errorMessage.value = "Error de conexión: ${e.message}"
                Timber.e(e, "Failed to connect to MongoDB")
            }
        }
    }

    suspend fun saveSensorData(data: SensorData) {
        try {
            // ALWAYS save to local Room database first
            val entity = SensorEntity.fromSensorData(data)
            sensorDao.insert(entity)
            Timber.d("Data saved locally in Room")
            
            // Try to sync if connected, but don't block
            if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                repositoryScope.launch {
                    try {
                        syncLocalData()
                    } catch (e: Exception) {
                        Timber.w("Immediate sync failed, will retry later")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Critical error saving sensor data locally")
        }
    }

    suspend fun manualSync() {
        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            syncLocalData()
        } else {
            // Try to reconnect if disconnected
            settingsManager.mongoUriFlow.first()?.let { uri ->
                connect(uri)
                if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                    syncLocalData()
                }
            }
        }
    }

    private suspend fun syncLocalData() {
        val entities = sensorDao.getAll().first()
        if (entities.isEmpty()) return

        entities.forEach { entity ->
            try {
                val collection = database?.getCollection<Document>("Lecturas")
                val doc = Document()
                    .append("timestamp", entity.timestamp)
                    .append("heartRate", entity.heartRate)
                    .append("accelerometer", Document()
                        .append("x", entity.accelX)
                        .append("y", entity.accelY)
                        .append("z", entity.accelZ))
                    .append("gyroscope", Document()
                        .append("x", entity.gyroX)
                        .append("y", entity.gyroY)
                        .append("z", entity.gyroZ))
                    .append("watchName", entity.watchName)
                    .append("battery", entity.battery)

                collection?.insertOne(doc)
                sensorDao.delete(entity)
                Timber.d("Data synced to MongoDB: ${entity.timestamp}")
            } catch (e: Exception) {
                Timber.e(e, "Error syncing data to MongoDB")
                throw e
            }
        }
    }
}

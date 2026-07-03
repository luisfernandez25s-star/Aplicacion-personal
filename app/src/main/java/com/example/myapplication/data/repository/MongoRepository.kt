package com.example.myapplication.data.repository

import com.example.myapplication.data.local.SensorDao
import com.example.myapplication.data.local.SensorEntity
import com.example.myapplication.data.manager.SettingsManager
import com.example.myapplication.data.model.SensorData
import com.example.myapplication.data.remote.SensorApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MongoRepository @Inject constructor(
    private val sensorDao: SensorDao,
    private val sensorApi: SensorApi,
    private val settingsManager: SettingsManager
) {
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    enum class ConnectionStatus {
        CONNECTED, DISCONNECTED, CONNECTING, ERROR
    }

    init {
        // La sincronización ahora es manual mediante el botón
    }

    suspend fun saveSensorData(data: SensorData) {
        try {
            val entity = SensorEntity.fromSensorData(data)
            sensorDao.insert(entity)
            Timber.d("Data saved locally in Room (Waiting for manual sync)")
        } catch (e: Exception) {
            Timber.e(e, "Critical error saving sensor data locally")
        }
    }

    suspend fun manualSync(data: SensorData?) {
        if (data == null) {
            _errorMessage.value = "No hay datos del reloj para subir"
            return
        }
        
        try {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            
            val baseUrl = settingsManager.mongoUriFlow.first() ?: throw Exception("URL de API no configurada")
            val fullUrl = if (baseUrl.endsWith("/")) "${baseUrl}api/sensors" else "$baseUrl/api/sensors"
            
            Timber.d("Syncing single record to $fullUrl")
            
            val response = sensorApi.sendSensorData(fullUrl, data)
            if (response.isSuccessful) {
                Timber.d("Successfully synced to Render")
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _errorMessage.value = null
            } else {
                throw Exception("Error del servidor: ${response.code()}")
            }
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.ERROR
            _errorMessage.value = "Sync failed: ${e.message}"
        }
    }
}

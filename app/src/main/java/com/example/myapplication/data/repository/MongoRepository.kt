package com.example.myapplication.data.repository

import com.example.myapplication.data.manager.SettingsManager
import com.example.myapplication.data.model.SensorData
import com.example.myapplication.data.remote.SensorApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MongoRepository @Inject constructor(
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

    suspend fun manualSync(data: SensorData?) {
        if (data == null) {
            _errorMessage.value = "No hay datos del reloj en pantalla"
            return
        }
        
        try {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            val baseUrl = settingsManager.mongoUriFlow.first() ?: throw Exception("URL no configurada")
            val fullUrl = if (baseUrl.endsWith("/")) "${baseUrl}api/sensors" else "$baseUrl/api/sensors"
            
            Timber.i("SUBIDA MANUAL: Enviando un único registro a $fullUrl")
            
            val response = sensorApi.sendSensorData(fullUrl, data)
            if (response.isSuccessful) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _errorMessage.value = null
                Timber.i("SUBIDA EXITOSA: Se guardó 1 registro.")
            } else {
                throw Exception("Error ${response.code()}")
            }
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.ERROR
            _errorMessage.value = "Fallo: ${e.message}"
        }
    }
}

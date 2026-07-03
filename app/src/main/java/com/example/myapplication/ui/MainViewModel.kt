package com.example.myapplication.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.manager.SettingsManager
import com.example.myapplication.data.model.SensorData
import com.example.myapplication.data.repository.MongoRepository
import com.example.myapplication.service.WearDataListenerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import java.io.InputStream
import java.util.Properties

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mongoRepository: MongoRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    val latestSensorData: StateFlow<SensorData?> = WearDataListenerService.latestData
    val connectionStatus: StateFlow<MongoRepository.ConnectionStatus> = mongoRepository.connectionStatus
    val connectionError: StateFlow<String?> = mongoRepository.errorMessage
    val apiUri: StateFlow<String?> = settingsManager.mongoUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun forceSync() {
        viewModelScope.launch {
            mongoRepository.manualSync(latestSensorData.value)
        }
    }

    fun updateApiUri(uri: String) {
        viewModelScope.launch {
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                settingsManager.saveMongoUri(uri)
                Timber.i("API URI updated manually")
            } else {
                Timber.w("Invalid API URI format")
            }
        }
    }

    fun importConfigFromFile(context: Context, fileUri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
                val uri = parseUriFromStream(inputStream, fileUri.toString())
                
                if (!uri.isNullOrBlank()) {
                    settingsManager.saveMongoUri(uri)
                    Timber.i("Config imported successfully")
                } else {
                    Timber.w("Could not find API_URI in file")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error importing config")
            }
        }
    }

    private fun parseUriFromStream(inputStream: InputStream?, fileName: String): String? {
        if (inputStream == null) return null
        
        val content = inputStream.bufferedReader().use { it.readText() }
        
        // Try parsing as Properties first
        try {
            val props = Properties()
            props.load(content.reader())
            val uri = props.getProperty("API_URI") ?: props.getProperty("MONGODB_URI")
            if (!uri.isNullOrBlank()) return uri
        } catch (e: Exception) {
        }

        // Try parsing as JSON
        try {
            val regex = "\"(?:API_URI|MONGODB_URI)\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val match = regex.find(content)
            if (match != null) return match.groupValues[1]
        } catch (e: Exception) {
        }

        val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() }
        if (!firstLine.isNullOrBlank() && (firstLine.startsWith("http") || firstLine.startsWith("mongodb"))) {
            return firstLine.trim()
        }

        return null
    }
}

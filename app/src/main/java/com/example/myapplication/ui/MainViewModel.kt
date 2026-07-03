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
    val mongoUri: StateFlow<String?> = settingsManager.mongoUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun forceSync() {
        viewModelScope.launch {
            mongoRepository.manualSync()
        }
    }

    fun updateMongoUri(uri: String) {
        viewModelScope.launch {
            if (uri.startsWith("mongodb://") || uri.startsWith("mongodb+srv://")) {
                settingsManager.saveMongoUri(uri)
                Timber.i("MongoDB URI updated manually")
            } else {
                Timber.w("Invalid MongoDB URI format")
            }
        }
    }

    fun importMongoUriFromFile(context: Context, fileUri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
                val uri = parseUriFromStream(inputStream, fileUri.toString())
                
                if (!uri.isNullOrBlank()) {
                    settingsManager.saveMongoUri(uri)
                    Timber.i("MongoDB URI imported successfully")
                } else {
                    Timber.w("Could not find MONGODB_URI in file")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error importing MongoDB URI")
            }
        }
    }

    private fun parseUriFromStream(inputStream: InputStream?, fileName: String): String? {
        if (inputStream == null) return null
        
        val content = inputStream.bufferedReader().use { it.readText() }
        
        // Try parsing as Properties first (for .env or .properties)
        try {
            val props = Properties()
            props.load(content.reader())
            val uri = props.getProperty("MONGODB_URI")
            if (!uri.isNullOrBlank()) return uri
        } catch (e: Exception) {
            // Not a properties file
        }

        // Try parsing as JSON
        try {
            val regex = "\"MONGODB_URI\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val match = regex.find(content)
            if (match != null) return match.groupValues[1]
        } catch (e: Exception) {
            // Not valid JSON or regex failed
        }

        // Try reading as plain text (first line or whole content)
        val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() }
        if (!firstLine.isNullOrBlank() && (firstLine.startsWith("mongodb://") || firstLine.startsWith("mongodb+srv://"))) {
            return firstLine.trim()
        }

        return null
    }
}

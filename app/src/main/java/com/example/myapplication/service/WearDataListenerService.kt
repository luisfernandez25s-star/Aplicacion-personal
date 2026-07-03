package com.example.myapplication.service

import com.example.myapplication.data.model.SensorData
import com.example.myapplication.data.repository.MongoRepository
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import javax.inject.Inject

@AndroidEntryPoint
class WearDataListenerService : WearableListenerService() {

    @Inject lateinit var mongoRepository: MongoRepository
    @Inject lateinit var gson: Gson

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val SENSOR_PATH = "/sensor_data"
        
        private val _latestData = MutableStateFlow<SensorData?>(null)
        val latestData: StateFlow<SensorData?> = _latestData.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("DEBUG_SYNC_PHONE: WearDataListenerService CREATED and listening...")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // Log EVERY message received to see if it even reaches the device
        Timber.i("DEBUG_SYNC_PHONE: Message detected! From: ${messageEvent.sourceNodeId}, Path: ${messageEvent.path}")

        if (messageEvent.path == SENSOR_PATH) {
            val bytes = messageEvent.data
            serviceScope.launch {
                try {
                    val json = if (isCompressed(bytes)) {
                        decompress(bytes)
                    } else {
                        String(bytes, Charsets.UTF_8)
                    }
                    
                    Timber.d("DEBUG_SYNC_PHONE: Processing JSON: $json")
                    val data = gson.fromJson(json, SensorData::class.java)
                    _latestData.value = data
                    
                    // Show a quick toast for debugging if needed (remove for production)
                    // Handler(Looper.getMainLooper()).post {
                    //     Toast.makeText(applicationContext, "Datos recibidos: HR ${data.heartRate}", Toast.LENGTH_SHORT).show()
                    // }
                    
                    mongoRepository.saveSensorData(data)
                    Timber.i("DEBUG_SYNC_PHONE: Data SAVED to MongoDB. HR: ${data.heartRate}")
                } catch (e: Exception) {
                    Timber.e(e, "DEBUG_SYNC_PHONE: Error processing message from ${messageEvent.sourceNodeId}")
                }
            }
        } else {
            Timber.w("DEBUG_SYNC_PHONE: Ignored message with path: ${messageEvent.path}")
        }
    }

    private fun isCompressed(data: ByteArray): Boolean {
        return data.size > 2 && data[0] == (0x1f).toByte() && data[1] == (0x8b).toByte()
    }

    private fun decompress(data: ByteArray): String {
        val bis = ByteArrayInputStream(data)
        val gis = GZIPInputStream(bis)
        return gis.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

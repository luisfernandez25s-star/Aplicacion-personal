package com.example.myapplication.wear.data.manager

import android.content.Context
import com.example.myapplication.wear.data.model.SensorData
import com.google.android.gms.wearable.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import java.util.zip.GZIPOutputStream
import java.io.ByteArrayOutputStream

@Singleton
class WearDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)

    companion object {
        private const val SENSOR_PATH = "/sensor_data"
        private const val PHONE_CAPABILITY = "phone_app"
        private const val COMPRESSION_THRESHOLD = 1024 // 1KB
    }

    suspend fun sendSensorData(data: SensorData) {
        Timber.d("DEBUG_SYNC: Attempting to send data. Timestamp: ${data.timestamp}, HR: ${data.heartRate}")
        try {
            val json = gson.toJson(data)
            var bytes = json.toByteArray(Charsets.UTF_8)

            if (bytes.size > COMPRESSION_THRESHOLD) {
                bytes = compress(bytes)
                Timber.d("DEBUG_SYNC: Data compressed: ${json.length} -> ${bytes.size} bytes")
            }

            var nodes = capabilityClient
                .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes

            Timber.d("DEBUG_SYNC: Nodes with capability '$PHONE_CAPABILITY': ${nodes.size}")

            if (nodes.isEmpty()) {
                Timber.w("DEBUG_SYNC: No nodes with capability found. Falling back to all connected nodes.")
                val allNodes = nodeClient.connectedNodes.await()
                Timber.d("DEBUG_SYNC: Total connected nodes found: ${allNodes.size}")
                nodes = allNodes.toSet()
            }

            if (nodes.isEmpty()) {
                Timber.e("DEBUG_SYNC: ABORTING. No connected nodes found. Is Bluetooth on and devices paired?")
                return
            }

            nodes.forEach { node ->
                Timber.d("DEBUG_SYNC: Sending message to node: ${node.displayName} (${node.id}) via path $SENSOR_PATH")
                messageClient.sendMessage(node.id, SENSOR_PATH, bytes)
                    .addOnSuccessListener {
                        Timber.i("DEBUG_SYNC: SUCCESS! Message sent to ${node.displayName}")
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "DEBUG_SYNC: FAILED to send message to ${node.displayName}")
                    }
            }
        } catch (e: Exception) {
            Timber.e(e, "DEBUG_SYNC: ERROR in sendSensorData")
        }
    }

    private fun compress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream(data.size)
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }
}

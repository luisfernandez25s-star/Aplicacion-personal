package com.example.myapplication.wear.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.myapplication.wear.data.manager.WearDataManager
import com.example.myapplication.wear.data.model.SensorData
import com.example.myapplication.wear.data.model.Vector3
import com.example.myapplication.wear.data.sensor.AccelerometerSensor
import com.example.myapplication.wear.data.sensor.GyroscopeSensor
import com.example.myapplication.wear.data.sensor.HeartRateSensor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.FlowPreview
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WearSensorService : Service() {

    @Inject lateinit var heartRateSensor: HeartRateSensor
    @Inject lateinit var accelerometerSensor: AccelerometerSensor
    @Inject lateinit var gyroscopeSensor: GyroscopeSensor
    @Inject lateinit var wearDataManager: WearDataManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val deviceId by lazy { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) }
    private val watchName = Build.MODEL

    companion object {
        private const val CHANNEL_ID = "wear_sensor_channel"
        private const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        observeSensors()
    }

    private fun startSensors() {
        heartRateSensor.start()
        accelerometerSensor.start()
        gyroscopeSensor.start()
    }

    @OptIn(FlowPreview::class)
    private fun observeSensors() {
        serviceScope.launch {
            // Usamos flows que siempre emiten algo para que el combine no se bloquee
            combine(
                heartRateSensor.dataFlow,
                accelerometerSensor.dataFlow,
                gyroscopeSensor.dataFlow
            ) { hr, accel, gyro ->
                SensorData(
                    timestamp = System.currentTimeMillis(),
                    heartRate = hr.firstOrNull() ?: 0f,
                    accelerometer = Vector3(
                        accel.getOrNull(0) ?: 0f,
                        accel.getOrNull(1) ?: 0f,
                        accel.getOrNull(2) ?: 0f
                    ),
                    gyroscope = Vector3(
                        gyro.getOrNull(0) ?: 0f,
                        gyro.getOrNull(1) ?: 0f,
                        gyro.getOrNull(2) ?: 0f
                    ),
                    deviceId = deviceId,
                    watchName = watchName
                )
            }
            .sample(2000)
            .collect { sensorData ->
                Timber.d("DEBUG_SYNC_WATCH: Collected data to send. HR: ${sensorData.heartRate}")
                wearDataManager.sendSensorData(sensorData)
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wear OS Sensor Monitor")
            .setContentText("Capturando y enviando datos...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sensor Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val simulate = intent?.getBooleanExtra("simulate", false) ?: false
        if (simulate) {
            Timber.i("Starting service in simulation mode")
            heartRateSensor.start(forceMock = true)
            accelerometerSensor.start(forceMock = true)
            gyroscopeSensor.start(forceMock = true)
        } else {
            startSensors()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        heartRateSensor.stop()
        accelerometerSensor.stop()
        gyroscopeSensor.stop()
        Timber.d("WearSensorService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.example.myapplication.service

import android.app.*
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var lastHR = 0f
    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f
    private var lastGyroX = 0f
    private var lastGyroY = 0f
    private var lastGyroZ = 0f

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "sensor_channel")
            .setContentTitle("Monitor de Sensores Activo")
            .setContentText("Enviando datos al celular...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)
        registerSensors()
    }

    private fun registerSensors() {
        heartRateSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> lastHR = event.values[0]
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccelX = event.values[0]
                lastAccelY = event.values[1]
                lastAccelZ = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroX = event.values[0]
                lastGyroY = event.values[1]
                lastGyroZ = event.values[2]
            }
        }
        sendData()
    }

    private var lastSendTime = 0L
    private fun sendData() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSendTime < 5000) return // Enviar cada 5 segundos

        lastSendTime = currentTime
        val dataClient = Wearable.getDataClient(this)
        val putDataMapReq = PutDataMapRequest.create("/sensor_data")
        putDataMapReq.dataMap.putFloat("hr", lastHR)
        putDataMapReq.dataMap.putFloat("ax", lastAccelX)
        putDataMapReq.dataMap.putFloat("ay", lastAccelY)
        putDataMapReq.dataMap.putFloat("az", lastAccelZ)
        putDataMapReq.dataMap.putFloat("gx", lastGyroX)
        putDataMapReq.dataMap.putFloat("gy", lastGyroY)
        putDataMapReq.dataMap.putFloat("gz", lastGyroZ)
        putDataMapReq.dataMap.putLong("timestamp", currentTime)
        putDataMapReq.dataMap.putLong("sync_id", System.nanoTime())
        
        dataClient.putDataItem(putDataMapReq.asPutDataRequest().setUrgent())
        Log.d("SensorService", "Datos enviados en segundo plano")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "sensor_channel", "Sensor Monitor Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}

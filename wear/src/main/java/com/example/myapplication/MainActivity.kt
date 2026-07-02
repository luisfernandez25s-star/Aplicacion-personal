package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import java.util.Locale
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private lateinit var tvStatus: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var tvAccel: TextView
    private lateinit var tvGyro: TextView
    
    private val handler = Handler(Looper.getMainLooper())
    private val checkNodesRunnable = object : Runnable {
        override fun run() {
            checkNodes()
            handler.postDelayed(this, 5000) // Reintentar cada 5 segundos
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)
        tvHeartRate = findViewById(R.id.tv_heart_rate)
        tvAccel = findViewById(R.id.tv_accel)
        tvGyro = findViewById(R.id.tv_gyro)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 1)
        } else {
            registerSensors()
        }
        checkNodes()
    }

    private fun checkNodes() {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.w("Wear", "No hay nodos conectados. ¿Están emparejados los emuladores?")
                    tvStatus.text = getString(R.string.no_connection)
                    tvStatus.setTextColor(Color.RED)
                } else {
                    Log.d("Wear", "Nodos conectados: ${nodes.size}")
                    tvStatus.text = getString(R.string.connected)
                    tvStatus.setTextColor(Color.GREEN)
                }
            }
    }

    private fun registerSensors() {
        heartRateSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensorName: String
        val valX: Float
        val valY: Float
        val valZ: Float
        
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                sensorName = "Ritmo Cardiaco"
                valX = event.values[0]
                valY = 0f
                valZ = 0f
                tvHeartRate.text = getString(R.string.hr_label, valX.toString())
            }
            Sensor.TYPE_ACCELEROMETER -> {
                sensorName = "Acelerómetro"
                valX = event.values[0]
                valY = event.values[1]
                valZ = event.values[2]
                tvAccel.text = getString(R.string.accel_label, String.format(Locale.getDefault(), "%.2f, %.2f, %.2f", valX, valY, valZ))
            }
            Sensor.TYPE_GYROSCOPE -> {
                sensorName = "Giroscopio"
                valX = event.values[0]
                valY = event.values[1]
                valZ = event.values[2]
                tvGyro.text = getString(R.string.gyro_label, String.format(Locale.getDefault(), "%.2f, %.2f, %.2f", valX, valY, valZ))
            }
            else -> return
        }
        
        sendDataToPhone(sensorName, valX, valY, valZ)
    }

    private fun sendDataToPhone(sensorName: String, x: Float, y: Float, z: Float) {
        val dataClient = Wearable.getDataClient(this)
        val putDataMapReq = PutDataMapRequest.create("/sensor_data")
        putDataMapReq.dataMap.putString("sensor_name", sensorName)
        putDataMapReq.dataMap.putFloat("value_x", x)
        putDataMapReq.dataMap.putFloat("value_y", y)
        putDataMapReq.dataMap.putFloat("value_z", z)
        putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
        
        // Sincronización extra para asegurar que los datos fluyan
        val putDataReq = putDataMapReq.asPutDataRequest()
        putDataReq.setUrgent()
        
        Log.d("Wear", "Intentando enviar datos de $sensorName al celular...")
        dataClient.putDataItem(putDataReq)
            .addOnSuccessListener { 
                Log.d("Wear", "¡ÉXITO! Datos de $sensorName enviados al celular") 
                handler.post {
                    tvStatus.text = "Enviando: $sensorName"
                    tvStatus.setTextColor(Color.GREEN)
                }
            }
            .addOnFailureListener { e -> 
                Log.e("Wear", "ERROR: No se pudo enviar datos al celular", e)
                handler.post {
                    tvStatus.text = "Error de Envío"
                    tvStatus.setTextColor(Color.RED)
                }
            }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
            == PackageManager.PERMISSION_GRANTED) {
            registerSensors()
        }
        handler.post(checkNodesRunnable)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(checkNodesRunnable)
    }
}

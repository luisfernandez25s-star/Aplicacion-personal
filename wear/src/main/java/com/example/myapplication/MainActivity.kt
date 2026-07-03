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
    
    // Variables para guardar las últimas lecturas
    private var lastHR = 0f
    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f
    private var lastGyroX = 0f
    private var lastGyroY = 0f
    private var lastGyroZ = 0f

    private val handler = Handler(Looper.getMainLooper())
    
    // Tarea para enviar datos cada 10 segundos
    private val sendDataRunnable = object : Runnable {
        override fun run() {
            sendAllDataToPhone()
            handler.postDelayed(this, 10000) // 10 segundos
        }
    }

    private val checkNodesRunnable = object : Runnable {
        override fun run() {
            checkNodes()
            handler.postDelayed(this, 5000) // Reintentar cada 5 segundos
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Wear", "MainActivity iniciada")
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)
        tvHeartRate = findViewById(R.id.tv_heart_rate)
        tvAccel = findViewById(R.id.tv_accel)
        tvGyro = findViewById(R.id.tv_gyro)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // Verificar sensores disponibles para depuración
        if (heartRateSensor == null) Log.w("Wear", "Sensor de Ritmo Cardiaco NO disponible")
        if (accelerometer == null) Log.w("Wear", "Acelerómetro NO disponible")
        if (gyroscope == null) Log.w("Wear", "Giroscopio NO disponible")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d("Wear", "Solicitando permisos de sensores")
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
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                lastHR = event.values[0]
                tvHeartRate.text = getString(R.string.hr_label, lastHR.toString())
            }
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccelX = event.values[0]
                lastAccelY = event.values[1]
                lastAccelZ = event.values[2]
                tvAccel.text = getString(R.string.accel_label, String.format(Locale.getDefault(), "%.2f, %.2f, %.2f", lastAccelX, lastAccelY, lastAccelZ))
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroX = event.values[0]
                lastGyroY = event.values[1]
                lastGyroZ = event.values[2]
                tvGyro.text = getString(R.string.gyro_label, String.format(Locale.getDefault(), "%.2f, %.2f, %.2f", lastGyroX, lastGyroY, lastGyroZ))
            }
        }
    }

    private fun sendAllDataToPhone() {
        // Enviamos las lecturas actuales (las 3 juntas para asegurar que lleguen)
        sendSingleSensorToPhone("Ritmo Cardiaco", lastHR, 0f, 0f)
        sendSingleSensorToPhone("Acelerómetro", lastAccelX, lastAccelY, lastAccelZ)
        sendSingleSensorToPhone("Giroscopio", lastGyroX, lastGyroY, lastGyroZ)
    }

    private fun sendSingleSensorToPhone(sensorName: String, x: Float, y: Float, z: Float) {
        val dataClient = Wearable.getDataClient(this)
        // RUTA ÚNICA PARA FORZAR AL CELULAR A RECIBIR
        val uniquePath = "/sensor_data/${sensorName.replace(" ", "_")}"
        val putDataMapReq = PutDataMapRequest.create(uniquePath)
        
        putDataMapReq.dataMap.putString("sensor_name", sensorName)
        putDataMapReq.dataMap.putFloat("value_x", x)
        putDataMapReq.dataMap.putFloat("value_y", y)
        putDataMapReq.dataMap.putFloat("value_z", z)
        putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
        putDataMapReq.dataMap.putLong("force_update", System.nanoTime())
        
        val putDataReq = putDataMapReq.asPutDataRequest()
        putDataReq.setUrgent()
        
        Log.d("Wear", "Enviando $sensorName al celular...")
        dataClient.putDataItem(putDataReq)
            .addOnSuccessListener { 
                Log.d("Wear", "Éxito: $sensorName enviado")
                handler.post {
                    tvStatus.text = "Sincronizado: ${System.currentTimeMillis() % 10000}"
                    tvStatus.setTextColor(Color.GREEN)
                }
            }
            .addOnFailureListener { e -> 
                Log.e("Wear", "Fallo al enviar $sensorName", e)
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
        handler.post(sendDataRunnable) // Iniciar bucle de 10 segundos
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(checkNodesRunnable)
        handler.removeCallbacks(sendDataRunnable) // Detener bucle
    }
}

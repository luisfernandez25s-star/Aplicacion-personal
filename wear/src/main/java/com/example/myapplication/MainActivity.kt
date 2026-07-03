package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.service.SensorService
import com.google.android.gms.wearable.Wearable
import java.util.Locale

class MainActivity : Activity(), SensorEventListener {

    private lateinit var tvStatus: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var tvAccel: TextView
    private lateinit var tvGyro: TextView
    private lateinit var btnSync: android.widget.Button
    private lateinit var sensorManager: SensorManager

    private val handler = Handler(Looper.getMainLooper())

    private val checkNodesRunnable = object : Runnable {
        override fun run() {
            checkNodes()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)
        tvHeartRate = findViewById(R.id.tv_heart_rate)
        tvAccel = findViewById(R.id.tv_accel)
        tvGyro = findViewById(R.id.tv_gyro)
        btnSync = findViewById(R.id.btn_sync)
        
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        btnSync.setOnClickListener {
            startSensorService()
        }

        // Solicitar permisos y registrar sensores
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 1)
        } else {
            registerSensors()
        }
        checkNodes()
    }

    private fun registerSensors() {
        val sensors = listOf(
            sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE),
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        )
        sensors.forEach { sensor ->
            sensor?.let { 
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                Log.d("Wear", "Sensor registrado: ${it.name}")
            } ?: Log.e("Wear", "Sensor no disponible")
        }
        startSensorService()
    }

    private fun startSensorService() {
        val serviceIntent = Intent(this, SensorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> tvHeartRate.text = "❤️ ${event.values[0].toInt()} BPM"
            Sensor.TYPE_ACCELEROMETER -> tvAccel.text = String.format(Locale.getDefault(), "XYZ: %.1f, %.1f, %.1f", event.values[0], event.values[1], event.values[2])
            Sensor.TYPE_GYROSCOPE -> tvGyro.text = String.format(Locale.getDefault(), "G: %.1f, %.1f, %.1f", event.values[0], event.values[1], event.values[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            registerSensors()
        }
    }

    private fun checkNodes() {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                tvStatus.text = "BUSCANDO CELULAR..."
                tvStatus.setTextColor(Color.WHITE)
            } else {
                tvStatus.text = "MONITOR ACTIVO"
                tvStatus.setTextColor(Color.WHITE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerSensors()
        handler.post(checkNodesRunnable)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(checkNodesRunnable)
    }
}

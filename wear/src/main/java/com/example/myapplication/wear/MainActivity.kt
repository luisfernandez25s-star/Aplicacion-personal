package com.example.myapplication.wear

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.myapplication.wear.data.sensor.AccelerometerSensor
import com.example.myapplication.wear.data.sensor.GyroscopeSensor
import com.example.myapplication.wear.data.sensor.HeartRateSensor
import com.example.myapplication.wear.service.WearSensorService
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var tvAccel: TextView
    private lateinit var tvGyro: TextView
    private lateinit var btnSync: android.widget.Button
    private lateinit var swSimulate: android.widget.Switch

    @Inject lateinit var heartRateSensor: HeartRateSensor
    @Inject lateinit var accelerometerSensor: AccelerometerSensor
    @Inject lateinit var gyroscopeSensor: GyroscopeSensor

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            Timber.i("Permisos concedidos por el usuario")
            Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
        } else {
            Timber.w("El usuario rechazó algunos permisos")
            Toast.makeText(this, "Faltan permisos para sensores", Toast.LENGTH_LONG).show()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var sensorJobs = mutableListOf<Job>()

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
        swSimulate = findViewById(R.id.sw_simulate)

        // Detección de emulador (solo visual, no arranca sensores)
        if (Build.PRODUCT.contains("sdk") || Build.MODEL.contains("Emulator") || Build.FINGERPRINT.contains("generic")) {
            swSimulate.isChecked = true
        }

        btnSync.setOnClickListener {
            if (checkPermissions()) {
                startSensors()
                Toast.makeText(this, "Iniciando captura...", Toast.LENGTH_SHORT).show()
            } else {
                requestRequiredPermissions()
            }
        }

        // Al iniciar, PEDIMOS permisos si faltan, pero NO arrancamos sensores
        handler.postDelayed({
            if (!checkPermissions()) {
                requestRequiredPermissions()
            }
        }, 1000)

        checkNodes()
    }

    private fun checkPermissions(): Boolean {
        val bodySensors = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
        val activityRec = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        return bodySensors && activityRec && notifications
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BODY_SENSORS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startSensors() {
        // Arrancamos el servicio de fondo
        val serviceIntent = Intent(this, WearSensorService::class.java).apply {
            putExtra("simulate", swSimulate.isChecked)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        // Arrancamos la UI local
        heartRateSensor.start(forceMock = swSimulate.isChecked)
        accelerometerSensor.start(forceMock = swSimulate.isChecked)
        gyroscopeSensor.start(forceMock = swSimulate.isChecked)
        
        observeSensors()
    }

    private fun observeSensors() {
        sensorJobs.forEach { it.cancel() }
        sensorJobs.clear()

        heartRateSensor.dataFlow.onEach { values ->
            val bpm = values.firstOrNull()?.toInt() ?: 0
            tvHeartRate.text = if (bpm > 0) "❤️ $bpm BPM" else "❤️ BUSCANDO..."
        }.launchIn(scope).also { sensorJobs.add(it) }

        accelerometerSensor.dataFlow.onEach { values ->
            if (values.size >= 3) {
                tvAccel.text = String.format(Locale.getDefault(), "XYZ: %.1f, %.1f, %.1f", values[0], values[1], values[2])
            }
        }.launchIn(scope).also { sensorJobs.add(it) }

        gyroscopeSensor.dataFlow.onEach { values ->
            if (values.size >= 3) {
                tvGyro.text = String.format(Locale.getDefault(), "G: %.1f, %.1f, %.1f", values[0], values[1], values[2])
            }
        }.launchIn(scope).also { sensorJobs.add(it) }
    }

    private fun checkNodes() {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                tvStatus.text = "SIN CONEXIÓN"
                tvStatus.setTextColor(android.graphics.Color.RED)
            } else {
                Wearable.getCapabilityClient(this)
                    .getCapability("phone_app", com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE)
                    .addOnSuccessListener { capabilityInfo ->
                        if (capabilityInfo.nodes.isEmpty()) {
                            tvStatus.text = "CELULAR SIN APP"
                            tvStatus.setTextColor(android.graphics.Color.YELLOW)
                        } else {
                            tvStatus.text = "CONECTADO A APP"
                            tvStatus.setTextColor(android.graphics.Color.GREEN)
                        }
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(checkNodesRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(checkNodesRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext.cancelChildren()
        heartRateSensor.stop()
        accelerometerSensor.stop()
        gyroscopeSensor.stop()
    }
}

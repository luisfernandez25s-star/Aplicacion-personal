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
        Timber.d("Permission result: $result")
        val allGranted = result.all { it.value }
        if (allGranted) {
            // After body sensors are granted, we might need to ask for background separately on API 33+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.d("Requesting background sensors separately...")
                requestBackgroundPermissionLauncher.launch(Manifest.permission.BODY_SENSORS_BACKGROUND)
            } else {
                startSensors()
            }
        } else {
            val denied = result.filter { !it.value }.keys
            Timber.w("Permissions denied: $denied")
            android.widget.Toast.makeText(this, "Permisos necesarios: $denied", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private val requestBackgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.d("Background sensor permission granted: $isGranted")
        startSensors()
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

        swSimulate.setOnCheckedChangeListener { _, isChecked ->
            restartSensors(isChecked)
        }

        btnSync.setOnClickListener {
            if (checkPermissions()) {
                startSensorService()
                restartSensors(swSimulate.isChecked)
                android.widget.Toast.makeText(this, "Sincronizando datos...", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                requestRequiredPermissions()
            }
        }

        requestRequiredPermissions() // Attempt immediately

        // Delay second attempt slightly to ensure window is ready/focused
        handler.postDelayed({
            if (!checkPermissions()) {
                requestRequiredPermissions()
            }
        }, 2000)

        checkNodes()
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            Timber.i("Missing permissions: $notGranted. Requesting...")
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS_BACKGROUND) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.i("Requesting background sensors...")
            requestBackgroundPermissionLauncher.launch(Manifest.permission.BODY_SENSORS_BACKGROUND)
        } else {
            Timber.i("All permissions granted.")
            startSensors()
        }
    }

    private fun checkPermissions(): Boolean {
        val bodySensors = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
        val activityRec = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        
        var allGranted = bodySensors && activityRec
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val backgroundSensors = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS_BACKGROUND) == PackageManager.PERMISSION_GRANTED
            allGranted = allGranted && backgroundSensors
        }
        
        return allGranted
    }

    private fun startSensors() {
        if (!checkPermissions()) {
            Timber.w("startSensors: Missing permissions")
            requestRequiredPermissions()
            return
        }

        Timber.d("startSensors: Initializing sensors in UI")
        heartRateSensor.start()
        accelerometerSensor.start()
        gyroscopeSensor.start()
        
        observeSensors()
    }

    private fun restartSensors(simulate: Boolean) {
        Timber.d("restartSensors: simulate=$simulate")
        heartRateSensor.stop()
        accelerometerSensor.stop()
        gyroscopeSensor.stop()

        if (simulate) {
            heartRateSensor.start(forceMock = true)
            accelerometerSensor.start(forceMock = true)
            gyroscopeSensor.start(forceMock = true)
            observeSensors()
        } else {
            startSensors()
        }
    }

    private fun observeSensors() {
        sensorJobs.forEach { it.cancel() }
        sensorJobs.clear()

        heartRateSensor.dataFlow.onEach { values ->
            if (values.isNotEmpty()) {
                val bpm = values[0].toInt()
                tvHeartRate.text = if (bpm > 0) "❤️ $bpm BPM" else "❤️ BUSCANDO..."
                Timber.d("Heart Rate updated: $bpm")
            } else {
                tvHeartRate.text = "❤️ INICIANDO..."
            }
        }.launchIn(scope).also { sensorJobs.add(it) }

        accelerometerSensor.dataFlow.onEach { values ->
            if (values.size >= 3) {
                tvAccel.text = String.format(Locale.getDefault(), "XYZ: %.1f, %.1f, %.1f", values[0], values[1], values[2])
            } else {
                tvAccel.text = "XYZ: BUSCANDO..."
            }
        }.launchIn(scope).also { sensorJobs.add(it) }

        gyroscopeSensor.dataFlow.onEach { values ->
            if (values.size >= 3) {
                tvGyro.text = String.format(Locale.getDefault(), "G: %.1f, %.1f, %.1f", values[0], values[1], values[2])
            } else {
                tvGyro.text = "G: BUSCANDO..."
            }
        }.launchIn(scope).also { sensorJobs.add(it) }
    }

    private fun startSensorService() {
        val serviceIntent = Intent(this, WearSensorService::class.java).apply {
            putExtra("simulate", swSimulate.isChecked)
        }
        startForegroundService(serviceIntent)
    }

    private fun checkNodes() {
        // First check for all connected nodes
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                tvStatus.text = "SIN CONEXIÓN (BT?)"
                tvStatus.setTextColor(android.graphics.Color.RED)
            } else {
                // Then check if any of those nodes have the phone app capability
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
        }.addOnFailureListener {
            Timber.e(it, "Error checking nodes")
            tvStatus.text = "ERROR DE RED"
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

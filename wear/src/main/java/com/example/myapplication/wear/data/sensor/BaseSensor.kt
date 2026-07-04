package com.example.myapplication.wear.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

abstract class BaseSensor(
    context: Context,
    private val sensorType: Int,
    private var sensorDelay: Int = SensorManager.SENSOR_DELAY_NORMAL
) : SensorEventListener {

    protected val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(sensorType)

    protected val _dataFlow = MutableStateFlow<FloatArray>(floatArrayOf())
    val dataFlow: StateFlow<FloatArray> = _dataFlow.asStateFlow()

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow: StateFlow<String?> = _errorFlow.asStateFlow()

    private var mockJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start(forceMock: Boolean = false) {
        // Stop any existing registration first to avoid duplicates
        stop()

        if (forceMock) {
            Timber.i("Starting forced mock data for sensor $sensorType")
            startMockData()
            return
        }

        if (sensor == null) {
            val errorMsg = "Sensor type $sensorType not available."
            _errorFlow.value = errorMsg
            Timber.w(errorMsg)
            return
        }
        
        // Reset data flow
        _dataFlow.value = when (sensorType) {
            Sensor.TYPE_HEART_RATE -> floatArrayOf(0f)
            else -> floatArrayOf(0f, 0f, 0f)
        }

        if (sensorType == Sensor.TYPE_HEART_RATE) {
            sensorDelay = SensorManager.SENSOR_DELAY_FASTEST
        }

        val registered = sensorManager.registerListener(this, sensor, sensorDelay)
        if (!registered) {
            val errorMsg = "Failed to register listener for sensor type $sensorType."
            _errorFlow.value = errorMsg
            Timber.e(errorMsg)
        } else {
            Timber.d("Sensor type $sensorType started successfully.")
        }
    }

    private fun startMockData() {
        mockJob?.cancel()
        mockJob = scope.launch {
            while (isActive) {
                val mockValues = when (sensorType) {
                    Sensor.TYPE_HEART_RATE -> floatArrayOf((60..100).random().toFloat() + (Math.random().toFloat() * 5f))
                    Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE -> floatArrayOf(
                        (-10..10).random().toFloat() + Math.random().toFloat(),
                        (-10..10).random().toFloat() + Math.random().toFloat(),
                        (-10..10).random().toFloat() + Math.random().toFloat()
                    )
                    else -> floatArrayOf(0f)
                }
                _dataFlow.value = mockValues
                delay(1000)
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        mockJob?.cancel()
        Timber.d("Sensor type $sensorType stopped")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == sensorType) {
            // Cancel watchdog if we receive real data
            if (mockJob?.isActive == true) {
                mockJob?.cancel()
                Timber.d("Real data received for sensor $sensorType, cancelling watchdog.")
            }
            _dataFlow.value = event.values.clone()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }
}

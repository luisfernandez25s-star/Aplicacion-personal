package com.example.myapplication.wear.data.model

import com.google.gson.annotations.SerializedName

data class SensorData(
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("heartRate") val heartRate: Float,
    @SerializedName("accelerometer") val accelerometer: Vector3,
    @SerializedName("gyroscope") val gyroscope: Vector3,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("watchName") val watchName: String? = null,
    @SerializedName("battery") val battery: Int? = null
)

data class Vector3(
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float,
    @SerializedName("z") val z: Float
)

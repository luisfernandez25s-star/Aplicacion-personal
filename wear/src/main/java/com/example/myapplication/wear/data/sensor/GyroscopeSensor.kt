package com.example.myapplication.wear.data.sensor

import android.content.Context
import android.hardware.Sensor
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class GyroscopeSensor @Inject constructor(
    @ApplicationContext context: Context
) : BaseSensor(context, Sensor.TYPE_GYROSCOPE)

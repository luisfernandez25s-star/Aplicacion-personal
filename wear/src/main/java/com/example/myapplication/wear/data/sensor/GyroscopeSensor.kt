package com.example.myapplication.wear.data.sensor

import android.content.Context
import android.hardware.Sensor
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class GyroscopeSensor @Inject constructor(
    @ApplicationContext context: Context
) : BaseSensor(context, Sensor.TYPE_GYROSCOPE)

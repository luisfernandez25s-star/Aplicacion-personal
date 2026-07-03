package com.example.myapplication.data.remote

import com.example.myapplication.data.model.SensorData
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface SensorApi {
    @POST
    suspend fun sendSensorData(
        @Url url: String,
        @Body data: SensorData
    ): Response<ResponseBody>
    
    @POST
    suspend fun sendSensorDataBatch(
        @Url url: String,
        @Body data: List<SensorData>
    ): Response<ResponseBody>
}

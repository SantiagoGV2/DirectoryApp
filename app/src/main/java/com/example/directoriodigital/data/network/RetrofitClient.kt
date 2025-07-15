package com.example.directoriodigital.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Usamos 'lazy' para que la instancia de Retrofit se cree solo una vez cuando se necesite
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.20.41:8080/api/") // Tu URL base
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Exponemos el servicio de la API para que otros lo usen
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
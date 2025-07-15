package com.example.directoriodigital.data.model

import com.google.gson.annotations.SerializedName

data class Carpeta(
    @SerializedName("carId") val id: Int,
    @SerializedName("carNombre") val nombre: String,
    @SerializedName("carColor") val color: String?
)
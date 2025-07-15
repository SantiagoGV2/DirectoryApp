package com.example.directoriodigital.data.network

import com.example.directoriodigital.data.model.Carpeta
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

data class FolderRequest(
    @SerializedName("carNombre") val nombre: String,
    @SerializedName("carColor") val hexcolor: String
)
interface ApiService {
    // --- Métodos para Carpetas ---
    @POST("carpetaAG")
    suspend fun createFolder(@Body folderRequest: FolderRequest): Response<Void>

    @GET("carpeta")
    suspend fun getFolders(): List<Carpeta> // Cambiado a suspend para usar con coroutines

    // --- Métodos para Datos de Contacto ---
    @Multipart
    @POST("DatoAG")
    suspend fun addContactData(
        @Part("datNombre") nombre: RequestBody,
        @Part("datProfesion") profesion: RequestBody,
        @Part("datEmail") email: RequestBody,
        @Part("datDireccion") direccion: RequestBody,
        @Part("datTelefono") telefono: RequestBody,
        @Part("carpetaId") carpetaId: RequestBody,
        @Part pdfFile: MultipartBody.Part?
    ): Response<ResponseBody>
}
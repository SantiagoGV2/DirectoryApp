package com.example.directoriodigital.data

import android.app.Application
import android.net.Uri
import com.example.directoriodigital.data.model.ContactInfo
import com.example.directoriodigital.data.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ContactRepository(private val application: Application) {

    // Esta es la función principal que el ViewModel llamará
    suspend fun sendContactData(contact: ContactInfo, folderId: Int, pdfFileUri: Uri?) {
        // 1. Preparamos los datos de texto como RequestBody
        val nombre = contact.name.toTextRequestBody()
        val profesion = contact.profession.toTextRequestBody()
        val email = contact.email.toTextRequestBody()
        val direccion = contact.address.toTextRequestBody()
        val telefono = contact.phone.toTextRequestBody()
        val carpetaId = folderId.toString().toTextRequestBody()

        // 2. Preparamos el archivo PDF como MultipartBody.Part, si existe
        val pdfPart = pdfFileUri?.let { createPdfPart(it) }

        // 3. Hacemos la llamada a la API a través de Retrofit
        val response = RetrofitClient.apiService.addContactData(
            nombre = nombre,
            profesion = profesion,
            email = email,
            direccion = direccion,
            telefono = telefono,
            carpetaId = carpetaId,
            pdfFile = pdfPart
        )

        if (!response.isSuccessful) {
            // Si la respuesta no es exitosa, lanzamos una excepción para que el ViewModel la capture
            throw Exception("Error del servidor: ${response.code()}")
        }
    }

    // --- Funciones de Ayuda ---

    // Convierte un String a RequestBody de tipo "text/plain"
    private fun String?.toTextRequestBody(): RequestBody {
        return (this ?: "").toRequestBody("text/plain".toMediaTypeOrNull())
    }

    // Convierte una Uri de PDF a un MultipartBody.Part
    private fun createPdfPart(uri: Uri): MultipartBody.Part? {
        val file = getFileFromUri(uri) ?: return null
        val requestFile = RequestBody.create("application/pdf".toMediaTypeOrNull(), file)
        return MultipartBody.Part.createFormData("datPdf", file.name, requestFile)
    }

    // Esta lógica la movimos del Fragment aquí. Necesita el contexto.
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = application.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("temp_pdf", ".pdf", application.cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
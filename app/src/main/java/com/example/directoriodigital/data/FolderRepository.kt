package com.example.directoriodigital.data

import com.example.directoriodigital.data.network.FolderRequest
import com.example.directoriodigital.data.network.RetrofitClient
import com.example.directoriodigital.data.model.Carpeta

class FolderRepository {
    // Usamos 'suspend' porque es una operación de red asíncrona
    suspend fun createFolderInApi(name: String, color: String) {
        val folderRequest = FolderRequest(nombre = name, hexcolor = color)
        // Llama al cliente de Retrofit para ejecutar la petición
        RetrofitClient.apiService.createFolder(folderRequest)
    }

    // ✅ NUEVA FUNCIÓN AÑADIDA
    // Obtiene la lista de carpetas desde la API
    suspend fun getFoldersFromApi(): List<Carpeta> {
        return RetrofitClient.apiService.getFolders()
    }
}
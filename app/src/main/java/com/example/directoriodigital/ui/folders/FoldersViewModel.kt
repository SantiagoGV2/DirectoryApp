package com.example.directoriodigital.ui.folders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.directoriodigital.data.FolderRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.directoriodigital.data.model.Carpeta
import android.util.Log
// Define el modelo de datos fuera del Fragmento


class FoldersViewModel : ViewModel() {

    private val repository = FolderRepository()

    // LiveData para que el Fragmento observe la lista de carpetas
    private val _folders = MutableLiveData<List<Carpeta>>()
    val folders: LiveData<List<Carpeta>> = _folders

    init {
        // Al iniciar, carga las carpetas (simulado por ahora)
        fetchFolders()
    }

    fun fetchFolders() {
        // ✅ Reemplazamos los datos simulados con la llamada real a la API
        viewModelScope.launch {
            try {
                _folders.value = repository.getFoldersFromApi()
            } catch (e: Exception) {
                Log.e("FoldersViewModel", "Error al obtener carpetas", e)
                _folders.value = emptyList() // En caso de error, muestra una lista vacía
            }
        }
    }

    fun createFolder(folderName: String, colorHex: String) {
        viewModelScope.launch {
            try {
                repository.createFolderInApi(folderName, colorHex)
                // ✅ Después de crear, volvemos a cargar la lista para que aparezca la nueva carpeta
                fetchFolders()
            } catch (e: Exception) {
                Log.e("FoldersViewModel", "Error al crear carpeta", e)
            }
        }
    }
}
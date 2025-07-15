package com.example.directoriodigital.ui.home

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.directoriodigital.data.ContactRepository
import com.example.directoriodigital.data.FolderRepository
import com.example.directoriodigital.data.model.Carpeta
import com.example.directoriodigital.data.model.ContactInfo
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch

// Un estado para saber el progreso del envío de datos
sealed class SubmissionState {
    object Idle : SubmissionState()
    object Loading : SubmissionState()
    data class Success(val message: String) : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val folderRepository = FolderRepository() // Instancia del repositorio
    private val contactRepository = ContactRepository(application)
    // --- LiveData para que el Fragment observe los cambios ---

    private val _folders = MutableLiveData<List<Carpeta>>()
    val folders: LiveData<List<Carpeta>> = _folders

    private val _extractedContactInfo = MutableLiveData<ContactInfo>()
    val extractedContactInfo: LiveData<ContactInfo> = _extractedContactInfo

    private val _submissionState = MutableLiveData<SubmissionState>(SubmissionState.Idle)
    val submissionState: LiveData<SubmissionState> = _submissionState

    init {
        // Carga las carpetas al iniciar
        loadFolders()
    }

    fun loadFolders() {
        viewModelScope.launch {
            try {
                _folders.value = folderRepository.getFoldersFromApi()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error al cargar carpetas", e)
                _folders.value = emptyList()
            }
        }
    }

    // --- Lógica de Negocio ---

    fun processImageWithOCR(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                if (text.isNotBlank()) {
                    _extractedContactInfo.value = extractContactInfo(text)
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeViewModel", "Error en OCR", e)
            }
    }

    fun sendData(contact: ContactInfo, folderId: Int, pdfFileUri: Uri?) {
        _submissionState.value = SubmissionState.Loading
        viewModelScope.launch {
            try {
                contactRepository.sendContactData(contact, folderId, pdfFileUri)
                _submissionState.value = SubmissionState.Success("Datos enviados con éxito")
            } catch (e: Exception) {
                _submissionState.value = SubmissionState.Error("Fallo en el envío: ${e.message}")
            }
        }
    }

    fun onSubmissionComplete() {
        _submissionState.value = SubmissionState.Idle
    }

    private fun extractContactInfo(text: String): ContactInfo {
        // Palabras que no deben formar parte del nombre
        val excludedWords = listOf(
            "Ingeniero", "Ingeniera", "Doctor", "Doctora", "Licenciado", "Licenciada",
            "Marketing", "Administración", "Finanzas", "Comercio", "Logística", "de", "y"
        )

        val stopWordsPattern = excludedWords.joinToString("|") { Regex.escape(it) }

        // 1. Regex para nombres (hasta 4 palabras que no sean profesiones)
        val nameRegex = Regex(
            """\b([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+(?:\s(?!$stopWordsPattern)[A-ZÁÉÍÓÚÑ][a-záéíóúñ]+){1,3})\b""",
            RegexOption.IGNORE_CASE
        )

        // 2. Regex para profesiones
        val professionRegex = Regex(
            """(?i)(Ingeniero(a)?(\s+de\s+Software)?|Administraci[óo]n\s+de\s+Empresas|Finanzas\s+y\s+Comercio\s+Exterior|Marketing(\s+y\s+Log[íi]stica)?|Negocios\s+Internacionales)"""
        )

        // 3. Regex para correos
        val emailRegex = Regex("""\b[\w.%+-]+@uniempresarial\.edu\.co\b""")
        val generalEmailRegex = Regex("""\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}\b""")

        // 4. Teléfonos
        val phoneRegex = Regex("""\b(?:\+?\d{1,3}\s?)?(?:\(\d{2,3}\)\s?)?[\d\s-]{7,}\b""")

        // 5. Direcciones
        val addressRegex = Regex("""\b(Calle|Av\.?|Avenida|Carrera|Cra\.?|Pasaje|Plaza|Diagonal|Transversal)\s+[\w\s#\d-]+""")

        // Limpieza del texto
        val cleanText = text.replace("\n", " ").replace(Regex("\\s{2,}"), " ")

        // Extracción del nombre (máximo 4 palabras, sin profesiones)
        val nameMatches = nameRegex.findAll(cleanText).map { it.value.trim() }.toList()
        val nameCandidate = nameMatches.firstOrNull() ?: "No encontrado"
        val nameWords = nameCandidate.split(" ")
            .takeWhile { it !in excludedWords }
            .take(4) // Limitar a 4 palabras
        val name = if (nameWords.size >= 2) nameWords.joinToString(" ") else nameCandidate

        // Extracción de profesión
        val professionMatches = professionRegex.findAll(cleanText).map { it.value.trim() }.toList()
        val profession = professionMatches.maxByOrNull { it.length } ?: "No encontrada"

        // Extracción de correo
        val uniEmails = emailRegex.findAll(cleanText).map { it.value }.toList()
        val generalEmails = generalEmailRegex.findAll(cleanText).map { it.value }.toList()
        val email = uniEmails.firstOrNull() ?: generalEmails.firstOrNull() ?: "No encontrado"

        // Teléfono y dirección
        val phone = phoneRegex.find(cleanText)?.value ?: "No encontrado"
        val address = addressRegex.find(cleanText)?.value ?: "No encontrada"

        // Debug opcional
        Log.d("OCR_Debug", "Texto limpio: $cleanText")
        Log.d("OCR_Debug", "Nombre: $name")
        Log.d("OCR_Debug", "Profesión: $profession")
        Log.d("OCR_Debug", "Correo: $email")
        Log.d("OCR_Debug", "Teléfono: $phone")
        Log.d("OCR_Debug", "Dirección: $address")

        return ContactInfo(name, profession, email, phone, address)
    }
}
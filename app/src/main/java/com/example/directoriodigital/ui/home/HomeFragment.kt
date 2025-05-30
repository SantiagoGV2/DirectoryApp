package com.example.directoriodigital.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.directoriodigital.databinding.FragmentHomeBinding
import com.google.android.gms.vision.text.TextRecognizer
import com.google.gson.annotations.SerializedName
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val CAMERA_REQUEST_CODE = 101
    private val REQUEST_CODE_PICK_PDF = 102
    private var selectedPdfUri: Uri? = null
    private var carpetas: List<Carpeta> = emptyList()  // Añadido para guardar la lista de carpetas

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)



        // Configurar botón de captura de imagen
        binding.btnCaptureImage.setOnClickListener {
            checkCameraPermission()
        }

        // Configurar botón para enviar datos al servidor
        binding.btnSendData.setOnClickListener {
            sendDataToServer()
        }

        // Configurar botón para subir archivo PDF
        binding.btnUploadPdf.setOnClickListener {
            openFilePicker()
        }

        cargarCarpetasEnSpinner()

        binding.btnPreviewPdf.setOnClickListener {
            if (selectedPdfUri != null) {
                previewPdf(selectedPdfUri!!)
            } else {
                Toast.makeText(requireContext(), "No se ha seleccionado un archivo PDF", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    //cambio
    private fun processImage(bitmap: Bitmap?, listener: OnTextExtractedListener) {
        if (bitmap == null) {
            Log.e("OCRProcessor", "No se recibió una imagen válida")
            return
        }

        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { text ->
                val extractedText: String = text.text
                Log.d("OCRProcessor", "Texto extraído: $extractedText")

                if (extractedText.isNotBlank()) {
                    val contactInfo: ContactInfo = extractContactInfo(extractedText)
                    listener.onTextExtracted(contactInfo)
                } else {
                    Log.e("OCRProcessor", "No se detectó texto en la imagen")
                    Toast.makeText(requireContext(), "No se detectó texto en la imagen", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("OCRProcessor", "Error al procesar la imagen", e)
            }
    }
//nuevo
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



//nuevo
    interface OnTextExtractedListener {
        fun onTextExtracted(contactInfo: ContactInfo?)
    }


    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf" // Filtrar solo archivos PDF
        startActivityForResult(intent, REQUEST_CODE_PICK_PDF)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, 100)
    }


//cambio
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
        val bitmap = data?.extras?.get("data") as? Bitmap

        if (bitmap == null) {
            Log.e("HomeFragment", "No se recibió imagen desde la cámara")
            Toast.makeText(requireContext(), "Error al capturar imagen", Toast.LENGTH_SHORT).show()
            return
        }

        processImage(bitmap, object : OnTextExtractedListener {
            override fun onTextExtracted(contactInfo: ContactInfo?) {
                if (contactInfo != null) {
                    binding.etNombre.setText(contactInfo.name.takeIf { it != "No encontrado" } ?: "")
                    binding.etProfesion.setText(contactInfo.profession.takeIf { it != "No encontrada" } ?: "")
                    binding.etCorreo.setText(contactInfo.email.takeIf { it != "No encontrado" } ?: "")
                    binding.etTelefono.setText(contactInfo.phone.takeIf { it != "No encontrado" } ?: "")
                    binding.etDireccion.setText(contactInfo.address.takeIf { it != "No encontrada" } ?: "")
                } else {
                    Log.e("HomeFragment", "No se pudo extraer información de la imagen")
                }
            }
        })
    } else if (requestCode == REQUEST_CODE_PICK_PDF && resultCode == Activity.RESULT_OK) {
        selectedPdfUri = data?.data
        selectedPdfUri?.let { uri ->
            val pdfName = uri.lastPathSegment ?: "Archivo PDF"
            binding.tvPdfName.text = "Archivo seleccionado: $pdfName"
        } ?: run {
            binding.tvPdfName.text = "Error al seleccionar el archivo"
        }
        Toast.makeText(requireContext(), "PDF seleccionado: $selectedPdfUri", Toast.LENGTH_SHORT).show()
    }
}

    private fun previewPdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No hay aplicaciones disponibles para previsualizar el PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarCarpetasEnSpinner() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.20.41:8080/api/") // Cambia la IP y puerto de tu backend
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        service.listarCarpetas().enqueue(object : retrofit2.Callback<List<Carpeta>> {
            override fun onResponse(call: retrofit2.Call<List<Carpeta>>, response: retrofit2.Response<List<Carpeta>>) {
                if (response.isSuccessful) {
                    carpetas = response.body() ?: emptyList()  // Guardamos las carpetas en la variable
                    val nombresCarpetas = carpetas.map { it.nombre }  // Solo el nombre de la carpeta

                    val adapter = ArrayAdapter(
                        requireContext(),
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        nombresCarpetas
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerCarpetas.adapter = adapter

                    binding.spinnerCarpetas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val carpetaSeleccionada = carpetas[position]
                            val carpetaId = carpetaSeleccionada.id
                            Toast.makeText(requireContext(), "Seleccionaste la carpeta con ID: $carpetaId", Toast.LENGTH_SHORT).show()
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // No hacer nada
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Error al cargar carpetas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<List<Carpeta>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
//cambio
    private fun sendDataToServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.20.41:8080/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        val nombre = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etNombre.text.toString())
        val profesion = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etProfesion.text.toString())
        val email = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etCorreo.text.toString())
        val direccion = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etDireccion.text.toString())
        val telefono = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etTelefono.text.toString())

        val selectedPosition = binding.spinnerCarpetas.selectedItemPosition
        if (selectedPosition < 0 || selectedPosition >= carpetas.size) {
            Toast.makeText(requireContext(), "Por favor selecciona una carpeta", Toast.LENGTH_SHORT).show()
            return
        }
        val carpetaId = RequestBody.create("text/plain".toMediaTypeOrNull(), carpetas[selectedPosition].id.toString())

        val pdfPart: MultipartBody.Part? = selectedPdfUri?.let { uri ->
            val file = getFileFromUri(uri)
            file?.let {
                val requestFile = RequestBody.create("application/pdf".toMediaTypeOrNull(), it)
                MultipartBody.Part.createFormData("datPdf", it.name, requestFile)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.agregarDato(nombre, profesion,email, direccion, telefono, carpetaId, pdfPart)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Datos enviados con éxito", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Error al enviar los datos", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("temp_pdf", ".pdf", requireContext().cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return tempFile
    }

}

data class Carpeta(
    @SerializedName("carId") val id: Int,
    @SerializedName("carNombre") val nombre: String,
    @SerializedName("carColor") val color: String?
)
    interface ApiService {
        @Multipart
        @POST("DatoAG")
        suspend fun agregarDato(
            @Part("datNombre") nombre: RequestBody,
            @Part("datProfesion") profesion: RequestBody,
            @Part("datEmail") email: RequestBody,
            @Part("datDireccion") direccion: RequestBody,
            @Part("datTelefono") telefono: RequestBody,
            @Part("carpetaId") carpetaId: RequestBody,
            @Part pdfFile: MultipartBody.Part?
        ): Response<ResponseBody>
        @GET("carpeta")
        fun listarCarpetas(): retrofit2.Call<List<Carpeta>>
}


package com.example.directoriodigital.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.directoriodigital.databinding.FragmentHomeBinding
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import java.io.File

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val CAMERA_REQUEST_CODE = 101
    private val REQUEST_CODE_PICK_PDF = 102
    private var textRecognizer: TextRecognizer? = null
    private var selectedPdfUri: Uri? = null
    private var carpetas: List<Carpeta> = emptyList()  // Añadido para guardar la lista de carpetas

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        textRecognizer = TextRecognizer.Builder(requireContext()).build()

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
        textRecognizer?.release()
        _binding = null
    }

    private fun processImage(bitmap: Bitmap) {
        val recognizer = textRecognizer
        if (recognizer != null && recognizer.isOperational) {
            val frame = Frame.Builder().setBitmap(bitmap).build()
            val textBlocks = recognizer.detect(frame)
            val stringBuilder = StringBuilder()

            for (i in 0 until textBlocks.size()) {
                val textBlock = textBlocks.valueAt(i)
                stringBuilder.append(textBlock.value).append("\n")
            }

            val extractedText = stringBuilder.toString()
            if (extractedText.isNotEmpty()) {
                val lines = extractedText.split("\n")
                binding.etNombre.setText(lines.getOrNull(0) ?: "")
                binding.etProfesion.setText(lines.getOrNull(1) ?: "")
                binding.etCorreo.setText(lines.getOrNull(2) ?: "")
                binding.etTelefono.setText(lines.getOrNull(3) ?: "")
                binding.etDireccion.setText(lines.getOrNull(4) ?: "")
            } else {
                Toast.makeText(requireContext(), "No se detectó texto en la imagen", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "El reconocimiento de texto no está disponible", Toast.LENGTH_SHORT).show()
        }
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



    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                processImage(it)
            }
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

    private fun sendDataToServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.20.41:8080/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        val nombre = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etNombre.text.toString())
        val email = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etCorreo.text.toString())
        val direccion = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etDireccion.text.toString())
        val profesion = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etProfesion.text.toString())
        val telefono = RequestBody.create("text/plain".toMediaTypeOrNull(), binding.etTelefono.text.toString())

        // Obtener el ID de la carpeta seleccionada
        val selectedPosition = binding.spinnerCarpetas.selectedItemPosition
        if (selectedPosition < 0 || selectedPosition >= carpetas.size) {
            Toast.makeText(requireContext(), "Por favor selecciona una carpeta", Toast.LENGTH_SHORT).show()
            return
        }
        val carpetaId = RequestBody.create("text/plain".toMediaTypeOrNull(), carpetas[selectedPosition].id.toString())

        var pdfPart: MultipartBody.Part? = null
        selectedPdfUri?.let { uri ->
            val file = getFileFromUri(uri) ?: return@let
            val requestFile = RequestBody.create("application/pdf".toMediaTypeOrNull(), file)
            pdfPart = MultipartBody.Part.createFormData("datPdf", file.name, requestFile)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.agregarDato(nombre, email, direccion, profesion, telefono, carpetaId, pdfPart)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Datos enviados con éxito", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Error en el envío", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
) {
    val hexcolor: String?
    get() = color
}
    interface ApiService {
        @Multipart
        @POST("DatoAG")
        suspend fun agregarDato(
            @Part("datNombre") nombre: RequestBody,
            @Part("datEmail") email: RequestBody,
            @Part("datDireccion") direccion: RequestBody,
            @Part("datProfesion") profesion: RequestBody,
            @Part("datTelefono") telefono: RequestBody,
            @Part("carpetaId") carpetaId: RequestBody,
            @Part pdfFile: MultipartBody.Part?
        ): Response<ResponseBody>
        @GET("carpeta")
        fun listarCarpetas(): retrofit2.Call<List<Carpeta>>
}


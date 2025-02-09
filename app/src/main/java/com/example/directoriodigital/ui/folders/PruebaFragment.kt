package com.example.directoriodigital.ui.folders

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.directoriodigital.R
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

class PruebaFragment : Fragment() {

    private lateinit var btnSelectPdf: Button
    private lateinit var btnUploadPdf: Button
    private lateinit var btnViewPdf: Button
    private var selectedPdfUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_prueba, container, false)
        btnSelectPdf = view.findViewById(R.id.btnSelectPdf)
        btnUploadPdf = view.findViewById(R.id.btnUploadPdf)
        btnViewPdf = view.findViewById(R.id.btnViewPdf)

        btnSelectPdf.setOnClickListener { selectPdf() }
        btnUploadPdf.setOnClickListener { uploadPdf() }
        btnViewPdf.setOnClickListener { viewPdf() }

        return view
    }

    fun getFileFromUri(context: Context, uri: Uri): File? {
        val fileName = getFileName(context, uri) ?: return null
        val tempFile = File(context.cacheDir, fileName)
        tempFile.outputStream().use { outputStream ->
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return name
    }

    private fun selectPdf() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivityForResult(intent, PDF_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PDF_PICK_CODE && resultCode == Activity.RESULT_OK) {
            selectedPdfUri = data?.data
            Toast.makeText(context, "PDF seleccionado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadPdf() {
        if (selectedPdfUri == null) {
            Toast.makeText(context, "Seleccione un PDF primero", Toast.LENGTH_SHORT).show()
            return
        }

        val file = getFileFromUri(requireContext(), selectedPdfUri!!)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Error al obtener el archivo", Toast.LENGTH_SHORT).show()
            return
        }

        val requestFile = RequestBody.create("application/pdf".toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val retrofit = RetrofitClient.getInstance()
        val apiService = retrofit.create(PdfApiService::class.java)

        apiService.uploadPdf(body).enqueue(object : retrofit2.Callback<Void> {
            override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "PDF subido con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al subir el PDF", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Upload Error", t.message ?: "Error desconocido")
            }
        })
    }


    private fun viewPdf() {
        if (selectedPdfUri != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(selectedPdfUri, "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        } else {
            Toast.makeText(context, "Seleccione un PDF primero", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val PDF_PICK_CODE = 100
    }
}

// Retrofit Interface
interface PdfApiService {
    @Multipart
    @POST("upload")
    fun uploadPdf(@Part pdf: MultipartBody.Part): Call<Void>
}

// Retrofit Client
object RetrofitClient {
    private const val BASE_URL = "http://192.168.20.41:8080/api/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    fun getInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
}

package com.example.directoriodigital.ui.folders

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.directoriodigital.R
import com.example.directoriodigital.databinding.FragmentFoldersBinding
import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    // Color predeterminado para la carpeta
    private var selectedColor: Int = Color.WHITE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textFolders
        textView.text = "Gestiona tus carpetas"

        // Botón para cambiar color
        val btnChangeColor = root.findViewById<Button>(R.id.btn_change_color)
        val cardExample = root.findViewById<View>(R.id.card_example)

        btnChangeColor.setOnClickListener {
            openNativeColorPicker { color ->
                selectedColor = color
                cardExample.setBackgroundColor(color)
            }
        }

        // Botón para guardar carpeta y redirigir
        val btnCreateFile = root.findViewById<Button>(R.id.btn_create_file)
        btnCreateFile.setOnClickListener {
            saveFolderToDatabase("Carpeta Ejemplo", selectedColor)
        }


        return root
    }

    private fun openNativeColorPicker(onColorSelected: (Int) -> Unit) {
        val colors = arrayOf("#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Selecciona un color")
        builder.setItems(colors) { _, which ->
            onColorSelected(Color.parseColor(colors[which]))
        }
        builder.create().show()
    }


    private fun saveFolderToDatabase(name: String, color: Int) {
        val nombre = binding.editFolderName.text.toString()
        if (nombre.isBlank()) {
            Toast.makeText(requireContext(), "El nombre de la carpeta no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }
        // Convertir el color a hexadecimal
        val hexColor = String.format("#%06X", 0xFFFFFF and color)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.20.41:8080/api/") // Cambia la IP y puerto de tu backend
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        val datos = Datos(nombre, hexColor)

        service.sendDatos(datos).enqueue(object : retrofit2.Callback<Void> {
            override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Datos enviados con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error al enviar los datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Aquí iría la lógica para guardar los datos en la base de datos.
        // Por ejemplo, una llamada a un ViewModel o Repositorio.
        println("Guardando carpeta: Nombre = $name, Color = $hexColor")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class Datos(
        @SerializedName("carNombre") val nombre: String,
        @SerializedName("carColor") val hexcolor: String,
    )

    interface ApiService {
        @POST("carpetaAG")
        fun sendDatos(@Body datos: Datos): retrofit2.Call<Void>
    }
}

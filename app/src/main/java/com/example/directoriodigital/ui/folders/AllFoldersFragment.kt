package com.example.directoriodigital.ui.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.directoriodigital.databinding.FragmentAllfoldersBinding
import retrofit2.Call
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.directoriodigital.ui.home.Carpeta
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory



class AllFoldersFragment : Fragment() {

    private var _binding: FragmentAllfoldersBinding? = null
    private val binding get() = _binding!!

    private lateinit var carpetaAdapter: CarpetaAdapter
    private lateinit var apiService: FoldersFragment.ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllfoldersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Configurar RecyclerView
        carpetaAdapter = CarpetaAdapter(emptyList())
        binding.recyclerViewCarpetas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCarpetas.adapter = carpetaAdapter

        // Inicializar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.20.41:8080/api/") // Cambia la IP y puerto de tu backend
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(FoldersFragment.ApiService::class.java)

        // Llamar a la API
        obtenerCarpetas()

        return root
    }

    private fun obtenerCarpetas() {
        apiService.getCarpetas().enqueue(object : Callback<List<Carpeta>> {
            override fun onResponse(call: Call<List<Carpeta>>, response: Response<List<Carpeta>>) {
                if (response.isSuccessful) {
                    val carpetas = response.body() ?: emptyList()
                    carpetaAdapter.updateData(carpetas)
                } else {
                    Toast.makeText(requireContext(), "Error al obtener carpetas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Carpeta>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}

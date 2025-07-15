package com.example.directoriodigital.ui.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.directoriodigital.databinding.FragmentFoldersBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.EditText
import com.example.directoriodigital.databinding.DialogCreateFolderBinding


class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    // Inicializamos el ViewModel
    private val viewModel: FoldersViewModel by viewModels()
    private lateinit var folderAdapter: FolderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializa el adapter
        folderAdapter = FolderAdapter()

        // 2. Configura el RecyclerView
        binding.recyclerViewFolders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = folderAdapter
        }

        // Observa los cambios en la lista de carpetas desde el ViewModel
        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            // El método submitList es del ListAdapter y maneja todo automáticamente
            folderAdapter.submitList(folders)
        }

        // Configura el clic del botón flotante
        binding.fabAddFolder.setOnClickListener {
            showCreateFolderDialog()
        }
    }

    private fun showCreateFolderDialog() {
        val dialogBinding = DialogCreateFolderBinding.inflate(LayoutInflater.from(requireContext()))

        // Lista de colores y los contenedores correspondientes
        val colorContainers = listOf(
            dialogBinding.colorBlueContainer,
            dialogBinding.colorGreenContainer,
            dialogBinding.colorYellowContainer,
            dialogBinding.colorRedContainer,
            dialogBinding.colorPurpleContainer
        )
        val colorHexValues = listOf("#2979FF", "#4CAF50", "#FFC107", "#F44336", "#9C27B0")

        var selectedColorHex = colorHexValues[0]
        var selectedContainer: View = dialogBinding.colorBlueContainer
        selectedContainer.isSelected = true

        // Asignar un listener a cada contenedor de color
        colorContainers.forEachIndexed { index, container ->
            container.setOnClickListener {
                // Quitar la selección del contenedor anterior
                selectedContainer.isSelected = false

                // Actualizar el nuevo contenedor y color seleccionados
                selectedContainer = it
                selectedContainer.isSelected = true
                selectedColorHex = colorHexValues[index]
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva Carpeta")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Crear") { _, _ ->
                val name = dialogBinding.etFolderName.text.toString()
                if (name.isNotBlank()) {
                    viewModel.createFolder(name, selectedColorHex)
                }
            }
            .show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.directoriodigital.ui.home

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
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.directoriodigital.R
import com.example.directoriodigital.data.model.ContactInfo
import com.example.directoriodigital.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private var selectedPdfUri: Uri? = null
    private var selectedFolderId: Int? = null

    // --- Nueva forma de manejar resultados de actividades ---
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let { viewModel.processImageWithOCR(it) }
        }
    }

    private val selectPdfLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            binding.btnUploadPdf.text = "PDF SELECCIONADO" // Feedback visual
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        // Cada vez que el fragmento se vuelve visible, recarga las carpetas
        viewModel.loadFolders()
    }

    private fun setupListeners() {
        binding.btnCaptureImage.setOnClickListener {
            checkCameraPermission()
        }

        binding.btnUploadPdf.setOnClickListener {
            selectPdfLauncher.launch("application/pdf")
        }

        binding.btnSendData.setOnClickListener {
            if (selectedFolderId == null) {
                Toast.makeText(requireContext(), "Por favor, selecciona una carpeta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val contactInfo = ContactInfo(
                name = binding.etNombre.text.toString(),
                profession = binding.etProfesion.text.toString(),
                email = binding.etCorreo.text.toString(),
                phone = binding.etTelefono.text.toString(),
                address = binding.etDireccion.text.toString()
            )

            viewModel.sendData(contactInfo, selectedFolderId!!, selectedPdfUri)
        }
    }

    private fun setupObservers() {
        // Observa la info extraída por OCR y la pone en los campos de texto
        viewModel.extractedContactInfo.observe(viewLifecycleOwner) { contactInfo ->
            binding.etNombre.setText(contactInfo.name)
            binding.etProfesion.setText(contactInfo.profession)
            binding.etCorreo.setText(contactInfo.email)
            binding.etTelefono.setText(contactInfo.phone)
            binding.etDireccion.setText(contactInfo.address)
        }

        // Observa la lista de carpetas y las pone en el menú desplegable
        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            val folderNames = folders.map { it.nombre }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, folderNames)
            binding.autoCompleteCarpetas.setAdapter(adapter)
            binding.autoCompleteCarpetas.setOnItemClickListener { _, _, position, _ ->
                selectedFolderId = folders[position].id
            }
        }

        // Observa el estado del envío para mostrar un feedback al usuario
        viewModel.submissionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SubmissionState.Loading -> {
                    binding.btnSendData.isEnabled = false
                    binding.btnSendData.text = "Enviando..."
                }
                is SubmissionState.Success -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    clearForm() // Limpia el formulario
                    viewModel.onSubmissionComplete() // Reinicia el estado
                }
                is SubmissionState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.onSubmissionComplete() // Reinicia el estado también en caso de error
                }
                is SubmissionState.Idle -> {
                    binding.btnSendData.isEnabled = true
                    binding.btnSendData.text = "Enviar Datos"
                }
            }
        }
    }

    // ✅ CAMBIO 3: Nueva función para limpiar todos los campos
    private fun clearForm() {
        binding.etNombre.text?.clear()
        binding.etProfesion.text?.clear()
        binding.etCorreo.text?.clear()
        binding.etTelefono.text?.clear()
        binding.etDireccion.text?.clear()
        binding.autoCompleteCarpetas.text.clear()
        binding.btnUploadPdf.text = "Adjuntar PDF" // Restaura el texto del botón
        selectedPdfUri = null
        selectedFolderId = null
        binding.etNombre.requestFocus() // Pone el foco en el primer campo
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(cameraIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
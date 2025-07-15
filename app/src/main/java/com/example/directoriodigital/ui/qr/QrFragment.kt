package com.example.directoriodigital.ui.qr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.directoriodigital.databinding.FragmentQrBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QrFragment : Fragment() {

    private var _binding: FragmentQrBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QrViewModel by viewModels()

    // ✅ Nueva forma de manejar el resultado del escáner. Reemplaza onActivityResult.
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        // El ViewModel se encarga de procesar el resultado
        viewModel.onScanResult(result.contents)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // El botón ahora inicia el nuevo 'launcher'
        binding.btnEscaner.setOnClickListener {
            initScanner()
        }

        // El Fragment observa los cambios del ViewModel
        viewModel.scannedResult.observe(viewLifecycleOwner) { content ->
            handleScannedContent(content)
        }
    }

    private fun initScanner() {
        // Configura las opciones del escáner
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Escanea el código QR")
            setBeepEnabled(true)
            setOrientationLocked(true)
        }
        // Inicia el escáner usando el nuevo launcher
        barcodeLauncher.launch(options)
    }

    private fun handleScannedContent(content: String) {
        if (content.startsWith("http://") || content.startsWith("https://")) {
            // Es una URL, la abrimos en el navegador
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
                startActivity(intent)
            } catch (e: Exception) {
                binding.textQr.text = "URL inválida: $content"
            }
        } else {
            // No es una URL, mostramos el texto
            binding.textQr.text = "Resultado: \n$content"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
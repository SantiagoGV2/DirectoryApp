package com.example.directoriodigital.ui.qr

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.directoriodigital.databinding.FragmentQrBinding
import com.google.zxing.integration.android.IntentIntegrator


class QrFragment : Fragment() {
    private var _binding: FragmentQrBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        _binding = FragmentQrBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.textQr


        binding.btnEscaner
        binding.btnEscaner.setOnClickListener { initScanner() }
        return root
    }

    private fun initScanner() {
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Escanee el QR")
        integrator.setBeepEnabled(true)
        integrator.initiateScan() // Inicia el escaneo
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val scannedContent = result.contents
                if (scannedContent.startsWith("http://") || scannedContent.startsWith("https://")) {
                    // Es una URL, redirige al navegador
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(scannedContent))
                    startActivity(intent)
                } else {
                    // No es una URL, muestra el contenido en el TextView
                    binding.textQr.text = "Código escaneado: $scannedContent"
                }
            } else {
                // El escaneo fue cancelado
                binding.textQr.text = "Escaneo cancelado"
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
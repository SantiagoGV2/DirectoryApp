package com.example.directoriodigital.ui.qr

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class QrViewModel : ViewModel() {
    // LiveData para comunicar el resultado del escaneo a la UI
    private val _scannedResult = MutableLiveData<String>()
    val scannedResult: LiveData<String> = _scannedResult

    fun onScanResult(content: String?) {
        _scannedResult.value = content ?: "Escaneo cancelado"
    }
}
package com.example.directoriodigital.ui.home

data class ContactInfo(
    var name: String? = null,
    var profession: String? = null,
    var email: String? = null,
    var phone: String? = null,
    var address: String? = null

) {


    override fun toString(): String {
        return "Nombre: ${name ?: "N/A"}\n" +
                "Profesión: ${profession ?: "N/A"}"+
                "Correo: ${email ?: "N/A"}\n" +
                "Teléfono: ${phone ?: "N/A"}\n" +
                "Dirección: ${address ?: "N/A"}\n"

    }
}

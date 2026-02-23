package com.example.bookmark.ui.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    // Crea un archivo privado en el móvil llamado "MiSesion"
    private val prefs: SharedPreferences = context.getSharedPreferences("MiSesion", Context.MODE_PRIVATE)

    // Escribe el correo en el bloc de notas
    fun guardarCorreoSesion(correo: String) {
        prefs.edit().putString("correo_usuario", correo).apply()
    }

    // Lee el correo (devuelve null si nadie ha iniciado sesión)
    fun obtenerCorreoSesion(): String? {
        return prefs.getString("correo_usuario", null)
    }

    // Borra la sesión (para el botón de Cerrar Sesión en el futuro)
    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}
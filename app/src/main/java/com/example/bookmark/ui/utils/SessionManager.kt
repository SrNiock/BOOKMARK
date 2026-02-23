package com.example.bookmark.ui.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    // Creamos nuestro "bloc de notas" interno llamado "AppConfig"
    private val prefs: SharedPreferences = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

    // Guardar el correo cuando inician sesión o se registran
    fun guardarSesion(correo: String) {
        prefs.edit().putString("CORREO_USUARIO", correo).apply()
    }

    // Leer el correo (si devuelve null, significa que nadie ha iniciado sesión)
    fun obtenerCorreoSesion(): String? {
        return prefs.getString("CORREO_USUARIO", null)
    }

    // Para cuando hagamos el botón de "Cerrar sesión"
    fun cerrarSesion() {
        prefs.edit().remove("CORREO_USUARIO").apply()
    }
}
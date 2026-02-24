package com.example.bookmark.ui.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    // Crea un archivo privado en el móvil llamado "MiSesion"
    private val prefs: SharedPreferences = context.getSharedPreferences("MiSesion", Context.MODE_PRIVATE)

    // --- NUEVO: GUARDA Y LEE EL ID DEL USUARIO ---
    fun guardarIdSesion(id: Long) {
        prefs.edit().putLong("id_usuario", id).apply()
    }

    fun obtenerIdSesion(): Long? {
        // SharedPreferences pide un valor por defecto si no lo encuentra. Usamos -1L.
        val id = prefs.getLong("id_usuario", -1L)
        return if (id != -1L) id else null
    }

    // --- MANTENEMOS EL CORREO (Por si quieres mostrarlo en el Perfil) ---
    fun guardarCorreoSesion(correo: String) {
        prefs.edit().putString("correo_usuario", correo).apply()
    }

    fun obtenerCorreoSesion(): String? {
        return prefs.getString("correo_usuario", null)
    }

    // Borra la sesión completa (tanto ID como correo)
    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}
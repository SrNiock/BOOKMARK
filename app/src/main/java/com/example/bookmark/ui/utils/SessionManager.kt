package com.example.bookmark.ui.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MiSesion", Context.MODE_PRIVATE)

    fun guardarIdSesion(id: Long) {
        prefs.edit().putLong("id_usuario", id).apply()
    }

    fun obtenerIdSesion(): Long? {
        val id = prefs.getLong("id_usuario", -1L)
        return if (id != -1L) id else null
    }

    fun guardarCorreoSesion(correo: String) {
        prefs.edit().putString("correo_usuario", correo).apply()
    }

    fun obtenerCorreoSesion(): String? {
        return prefs.getString("correo_usuario", null)
    }


    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}
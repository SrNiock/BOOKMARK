package com.example.bookmark.ui.supaBase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val id: Long? = null,
    val nombre: String,
    val apellidos: String,
    val correoElectronico: String,
    @SerialName("contraseña")
    val contrasena: String,
    val fotoPerfil: String? = null,
    val fotoBanner: String? = null,
    val nickname: String,
    // 👇 AÑADE ESTA LÍNEA 👇
    val descripcion: String? = null
)

@Serializable
data class MiLibro(
    val id: Int? = null,
    val correo_usuario: String,
    val book_key: String,
    val titulo: String,
    val autor: String?,
    val cover_id: Int?,
    val estado: String, // "deseado", "leyendo", "terminado"
    val progreso_porcentaje: Int = 0
)
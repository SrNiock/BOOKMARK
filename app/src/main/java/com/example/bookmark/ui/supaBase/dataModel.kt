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
    val id_usuario: Long,
    @SerialName("book_key") val bookKey: String, // 👇 ¡AQUÍ ESTÁ EL CAMBIO! Tod0 en minúscula
    val titulo: String,
    val autor: String? = null,
    val cover_id: Int? = null,
    val estado: String,
    val progreso_porcentaje: Int = 0
)
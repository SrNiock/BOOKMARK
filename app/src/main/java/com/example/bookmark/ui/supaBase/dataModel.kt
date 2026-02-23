package com.example.bookmark.ui.supaBase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val id: Long? = null, // Dejamos que Supabase lo genere automáticamente
    val nombre: String,
    val apellidos: String,
    val correoElectronico: String,

    // TRUCO: Le decimos que en la BD se llama "contraseña" con ñ, pero en Kotlin usamos "contrasena"
    @SerialName("contraseña")
    val contrasena: String,

    val perfilPublico: Boolean,
    val fotoPerfil: String? = null,
    val fotoBanner: String? = null,
    val nickname: String
)
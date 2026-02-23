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
    // Bórralo de aquí ---> val perfilPublico: Boolean
    val fotoPerfil: String? = null,
    val fotoBanner: String? = null,
    val nickname: String
)
package com.example.bookmark.ui.supaBase

import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val id: Long? = null, // int8 en Supabase es Long en Kotlin
    val nombre: String,
    val apellidos: String,
    val correoElectronico: String,
    val perfilPublico: Boolean,
    val fotoPerfil: String? = null // Puede ser nulo si no tienen foto
    // Nota: He omitido 'contraseña' por seguridad, no es buena práctica traerla al cliente
)
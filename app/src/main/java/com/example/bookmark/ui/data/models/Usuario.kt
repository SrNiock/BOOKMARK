package com.example.bookmark.ui.data.models


import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val id: Long,
    val nombre: String,
    val apellidos: String,
    val correoElectronico: String,
    // La contraseña la pongo aquí por si la necesitas para verificar, 
    // pero OJO: en producción las contraseñas no se guardan en texto plano.
    // Usaremos esto solo para fines educativos por ahora.
    val contraseña: String,
    val perfilPublico: Boolean,
    val nickname: String
)
package com.example.bookmark.ui.supaBase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LikePublicacion(
    val usuario_id: Long,
    val publicacion_id: Long
)

@Serializable
data class Comentario(
    val id: Long? = null,
    val usuario_id: Long,
    val publicacion_id: Long,
    val texto: String
)

@Serializable
data class ComentarioFeed(
    val id: Long? = null,
    val texto: String,
    val usuario_id: Long,
    val publicacion_id: Long,
    @SerialName("Usuarios")
    val usuario: UsuarioPublicacion? = null
)

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
    val descripcion: String? = null
)

@Serializable
data class MiLibro(
    val id: Int? = null,
    val id_usuario: Long,
    @SerialName("book_key") val bookKey: String,
    val titulo: String,
    val autor: String? = null,
    val cover_id: Int? = null,
    val estado: String,
    val progreso_porcentaje: Int = 0,
    val paginas_totales: Int? = null
)

@Serializable
data class Favorito(
    val id: Int? = null,
    val usuario_id: Long,
    val libro_id: Int
)

@Serializable
data class SeguidorRelacion(
    val seguidor_id: Long,
    val seguido_id: Long
)

@Serializable
data class Publicacion(
    val id: Long? = null,
    val usuario_id: Long,
    val book_key: String,
    val titulo_libro: String,
    val cover_id: Int? = null,
    val texto: String,
    val calificacion: Int
)

@Serializable
data class UsuarioPublicacion(
    val nickname: String,
    val fotoPerfil: String? = null
)

@Serializable
data class PublicacionFeed(
    val id: Long? = null,
    val usuario_id: Long,
    val book_key: String,
    val titulo_libro: String,
    val cover_id: Int? = null,
    val texto: String,
    val calificacion: Int,
    @SerialName("Usuarios")
    val usuario: UsuarioPublicacion? = null
)

@Serializable
data class PublicacionGuardada(
    val usuario_id: Long,
    val publicacion_id: Long
)
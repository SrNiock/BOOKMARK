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

// Este modelo pequeñito es para unir los datos del usuario a la publicación
@Serializable
data class UsuarioPublicacion(
    val nickname: String,
    val fotoPerfil: String? = null
)

// Este es el modelo del Feed que junta la reseña con el autor
@Serializable
data class PublicacionFeed(
    val id: Long? = null,
    val usuario_id: Long,
    val book_key: String,
    val titulo_libro: String,
    val cover_id: Int? = null,
    val texto: String,
    val calificacion: Int,
    @SerialName("Usuarios") // 👈 IMPORTANTE: Esto le dice a Supabase que busque en tu tabla "Usuarios"
    val usuario: UsuarioPublicacion? = null
)
package com.example.bookmark.ui.supaBase

import com.example.bookmark.ui.supaBase.SupabaseClient.client
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable


class AuthRepository {

    private val tablaUsuarios = client.from("Usuarios")
    private val tablaBiblioteca = client.from("mislibros")
    private val tablaFavoritos = client.from("favoritos")

    //LOGIN
    suspend fun login(correo: String, contrasena: String): Result<Usuario> {
        return withContext(Dispatchers.IO) {
            try {
                val usuariosEncontrados = tablaUsuarios.select {
                    filter {
                        eq("correoElectronico", correo)
                        eq("contraseña", contrasena)
                    }
                }.decodeList<Usuario>()

                if (usuariosEncontrados.isNotEmpty()) {
                    Result.success(usuariosEncontrados.first())
                } else {
                    Result.failure(Exception("Correo o contraseña incorrectos"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // REGISTRAR
    suspend fun registrar(usuario: Usuario): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                tablaUsuarios.insert(usuario)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // OBTENER DATOS DEL USUARIO
    suspend fun obtenerUsuario(correo: String): Result<Usuario> {
        return withContext(Dispatchers.IO) {
            try {
                val usuario = tablaUsuarios.select {
                    filter { eq("correoElectronico", correo) }
                }.decodeSingle<Usuario>()
                Result.success(usuario)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // SUBIR IMAGEN A STORAGE
    suspend fun subirImagenStorage(rutaArchivo: String, bytes: ByteArray): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val bucket = client.storage.from("perfiles")
                bucket.upload(rutaArchivo, bytes) { upsert = true }
                val urlPublica = bucket.publicUrl(rutaArchivo)
                Result.success(urlPublica)
            } catch (e: Exception) {
                println("ERROR STORAGE: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // ACTUALIZAR DATO EN TABLA USUARIOS
    suspend fun actualizarFotoTabla(correo: String, columna: String, valor: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                tablaUsuarios.update({ set(columna, valor) }) {
                    filter { eq("correoElectronico", correo) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    // OBTENER UN LIBRO CONCRETO
    suspend fun obtenerLibroDeBiblioteca(idUsuario: Long, bookKey: String): Result<MiLibro> {
        return withContext(Dispatchers.IO) {
            try {
                val lista = tablaBiblioteca.select {
                    filter {
                        eq("id_usuario", idUsuario)

                        eq("book_key", bookKey)
                    }
                }.decodeList<MiLibro>()

                if (lista.isNotEmpty()) {
                    Result.success(lista.first())
                } else {
                    Result.failure(Exception("Libro no encontrado en biblioteca"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // OBTENER LIBROS POR ESTADO
    suspend fun obtenerLibrosPorEstado(idUsuario: Long, estado: String): Result<List<MiLibro>> {
        return withContext(Dispatchers.IO) {
            try {
                val lista = tablaBiblioteca.select {
                    filter {
                        eq("id_usuario", idUsuario)
                        eq("estado", estado)
                    }
                }.decodeList<MiLibro>()
                Result.success(lista)
            } catch (e: Exception) {
                println("Error Supabase Biblioteca: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // GUARDAR O ACTUALIZAR LIBRO
    suspend fun actualizarLibroEnBiblioteca(libro: MiLibro): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                tablaBiblioteca.upsert(libro)
                Result.success(Unit)
            } catch (e: Exception) {
                println("Error al guardar libro: ${e.message}")
                Result.failure(e)
            }
        }
    }
    // AGREGAR NUEVO LIBRO
    suspend fun agregarLibroABiblioteca(libro: MiLibro): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Usamos insert en lugar de upsert para garantizar la creación de la fila
                tablaBiblioteca.insert(libro)
                Result.success(Unit)
            } catch (e: Exception) {
                println("Error al insertar libro: ${e.message}")
                Result.failure(e)
            }
        }
    }
    // ACTUALIZAR PROGRESO
    suspend fun actualizarProgreso(idLibro: Int, nuevoProgreso: Int, nuevoEstado: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                tablaBiblioteca.update({
                    set("progreso_porcentaje", nuevoProgreso)
                    set("estado", nuevoEstado)
                }) {
                    filter { eq("id", idLibro) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    suspend fun eliminarDeFavoritos(idUsuario: Long, idLibro: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("favoritos").delete {
                    filter {
                        eq("usuario_id", idUsuario)
                        eq("libro_id", idLibro)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun contarFavoritos(idUsuario: Long): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val conteo = client.from("favoritos")
                    .select { filter { eq("usuario_id", idUsuario) } }
                    .decodeList<Favorito>()
                    .size.toLong()
                Result.success(conteo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun agregarAFavoritos(idUsuario: Long, idLibro: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val nuevoFav = Favorito(usuario_id = idUsuario, libro_id = idLibro)
                client.from("favoritos").insert(nuevoFav)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun eliminarLibroDeBiblioteca(idLibro: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("mislibros").delete {
                    filter { eq("id", idLibro) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun obtenerFavoritos(idUsuario: Long): Result<List<MiLibro>> {
        return withContext(Dispatchers.IO) {
            try {
                val filasFavoritos = client.from("favoritos")
                    .select { filter { eq("usuario_id", idUsuario) } }
                    .decodeList<Favorito>()

                val idsLibros = filasFavoritos.map { it.libro_id }
                if (idsLibros.isEmpty()) return@withContext Result.success(emptyList())

                val libros = client.from("mislibros")
                    .select { filter { isIn("id", idsLibros) } }
                    .decodeList<MiLibro>()

                Result.success(libros)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // BÚSQUEDA DE USUARIOS

    suspend fun buscarUsuarios(query: String): Result<List<Usuario>> {
        return withContext(Dispatchers.IO) {
            try {
                val usuariosEncontrados = client.from("Usuarios")
                    .select { filter { ilike("nickname", "%$query%") } }
                    .decodeList<Usuario>()
                Result.success(usuariosEncontrados)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun obtenerUsuarioPorId(idUsuario: Long): Result<Usuario> {
        return withContext(Dispatchers.IO) {
            try {
                val usuarios = client.from("Usuarios")
                    .select { filter { eq("id", idUsuario) } }
                    .decodeList<Usuario>()

                if (usuarios.isNotEmpty()) Result.success(usuarios.first())
                else Result.failure(Exception("Usuario no encontrado"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // SEGUIDORES

    suspend fun comprobarSiSigue(idSeguidor: Long, idSeguido: Long): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val relacion = client.from("seguidores")
                    .select { filter { eq("seguidor_id", idSeguidor); eq("seguido_id", idSeguido) } }
                    .decodeList<SeguidorRelacion>()
                Result.success(relacion.isNotEmpty())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun seguirUsuario(idSeguidor: Long, idSeguido: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("seguidores").insert(SeguidorRelacion(seguidor_id = idSeguidor, seguido_id = idSeguido))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun dejarDeSeguirUsuario(idSeguidor: Long, idSeguido: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("seguidores").delete {
                    filter { eq("seguidor_id", idSeguidor); eq("seguido_id", idSeguido) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun contarSeguidores(idUsuario: Long): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val count = client.from("seguidores")
                    .select { filter { eq("seguido_id", idUsuario) } }
                    .decodeList<SeguidorRelacion>().size.toLong()
                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun contarSeguidos(idUsuario: Long): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val count = client.from("seguidores")
                    .select { filter { eq("seguidor_id", idUsuario) } }
                    .decodeList<SeguidorRelacion>().size.toLong()
                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun obtenerIdsSeguidos(idUsuario: Long): Result<List<Long>> {
        return withContext(Dispatchers.IO) {
            try {
                val relaciones = client.from("seguidores")
                    .select { filter { eq("seguidor_id", idUsuario) } }
                    .decodeList<SeguidorRelacion>()
                Result.success(relaciones.map { it.seguido_id })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // PUBLICACIONES
    suspend fun crearPublicacion(publicacion: Publicacion): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("publicaciones").insert(publicacion)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun obtenerFeedPublicaciones(): Result<List<PublicacionFeed>> {
        return withContext(Dispatchers.IO) {
            try {
                val lista = client.from("publicaciones")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, Usuarios(*)"))
                    .decodeList<PublicacionFeed>()
                Result.success(lista.reversed())
            } catch (e: Exception) {
                println("ERROR AL CARGAR FEED: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // LIKES
    suspend fun darLike(idUsuario: Long, idPublicacion: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("likes_publicaciones").insert(LikePublicacion(idUsuario, idPublicacion))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun quitarLike(idUsuario: Long, idPublicacion: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("likes_publicaciones").delete {
                    filter { eq("usuario_id", idUsuario); eq("publicacion_id", idPublicacion) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun comprobarSiDioLike(idUsuario: Long, idPublicacion: Long): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val likes = client.from("likes_publicaciones").select {
                    filter { eq("usuario_id", idUsuario); eq("publicacion_id", idPublicacion) }
                }.decodeList<LikePublicacion>()
                Result.success(likes.isNotEmpty())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun contarLikes(idPublicacion: Long): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val likes = client.from("likes_publicaciones").select {
                    filter { eq("publicacion_id", idPublicacion) }
                }.decodeList<LikePublicacion>()
                Result.success(likes.size.toLong())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // COMENTARIOS
    suspend fun agregarComentario(comentario: Comentario): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("comentarios").insert(comentario)
                Result.success(Unit)
            } catch (e: Exception) {
                println("ERROR AL COMENTAR: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun obtenerComentarios(idPublicacion: Long): Result<List<ComentarioFeed>> {
        return withContext(Dispatchers.IO) {
            try {
                val lista = client.from("comentarios")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, Usuarios(*)")) {
                        filter { eq("publicacion_id", idPublicacion) }
                    }.decodeList<ComentarioFeed>()
                Result.success(lista)
            } catch (e: Exception) {
                println("ERROR AL LEER COMENTARIOS: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // PUBLICACIONES GUARDADAS
    suspend fun guardarPublicacion(idUsuario: Long, idPublicacion: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("publicaciones_guardadas").insert(PublicacionGuardada(idUsuario, idPublicacion))
                Result.success(Unit)
            } catch (e: Exception) {
                println("ERROR AL GUARDAR PUBLICACIÓN: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun eliminarPublicacionGuardada(idUsuario: Long, idPublicacion: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("publicaciones_guardadas").delete {
                    filter { eq("usuario_id", idUsuario); eq("publicacion_id", idPublicacion) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                println("ERROR AL ELIMINAR GUARDADO: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun comprobarSiGuardado(idUsuario: Long, idPublicacion: Long): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val rows = client.from("publicaciones_guardadas").select {
                    filter { eq("usuario_id", idUsuario); eq("publicacion_id", idPublicacion) }
                }.decodeList<PublicacionGuardada>()
                Result.success(rows.isNotEmpty())
            } catch (e: Exception) {
                println("ERROR AL COMPROBAR GUARDADO: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // RECOMENDACIONES
    suspend fun obtenerDatosParaRecomendaciones(idUsuario: Long): Result<DatosRecomendacion> {
        return withContext(Dispatchers.IO) {
            try {
                val biblioteca = tablaBiblioteca.select {
                    filter { eq("id_usuario", idUsuario) }
                }.decodeList<MiLibro>()

                val leyendo    = biblioteca.filter { it.estado == "leyendo" }
                val deseados   = biblioteca.filter { it.estado == "deseado" }
                val terminados = biblioteca.filter { it.estado == "terminado" }

                val autoresPesados = mutableListOf<String>()
                leyendo.mapNotNull { it.autor }.forEach { repeat(3) { _ -> autoresPesados.add(it) } }
                deseados.mapNotNull { it.autor }.forEach { repeat(2) { _ -> autoresPesados.add(it) } }
                terminados.mapNotNull { it.autor }.forEach { autoresPesados.add(it) }

                val autoresOrdenados = autoresPesados
                    .groupingBy { it }.eachCount().entries
                    .sortedByDescending { it.value }
                    .map { it.key }.distinct()

                val titulosActivos = (leyendo + deseados).mapNotNull { it.titulo }.distinct()
                val todasLasKeys   = biblioteca.map { it.bookKey }

                Result.success(DatosRecomendacion(autoresOrdenados, titulosActivos, todasLasKeys))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun obtenerAutoresPreferidos(idUsuario: Long): Result<List<String>> {
        return obtenerDatosParaRecomendaciones(idUsuario).map { it.autores }
    }

    suspend fun obtenerKeysEnBiblioteca(idUsuario: Long): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val lista = tablaBiblioteca.select {
                    filter { eq("id_usuario", idUsuario) }
                }.decodeList<MiLibro>()
                Result.success(lista.map { it.bookKey })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

data class DatosRecomendacion(
    val autores: List<String>,
    val titulosActivos: List<String>,
    val keysEnBiblioteca: List<String>
)
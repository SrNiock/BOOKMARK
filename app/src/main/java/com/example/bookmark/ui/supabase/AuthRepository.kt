package com.example.bookmark.ui.supaBase

import com.example.bookmark.ui.supaBase.SupabaseClient.client
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable


class AuthRepository {

    private val tablaUsuarios = client.from("Usuarios")
    private val tablaBiblioteca = client.from("mislibros") // Nueva tabla de biblioteca
    private val tablaFavoritos = client.from("favoritos")

    // --- 1. LOGIN ---
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

    // --- 2. REGISTRAR ---
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

    // --- 3. OBTENER DATOS DEL USUARIO (Para Perfil y TopBar) ---
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

    // --- 4. SUBIR IMAGEN A STORAGE (Perfil y Banner) ---
    suspend fun subirImagenStorage(rutaArchivo: String, bytes: ByteArray): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val bucket = client.storage.from("perfiles")
                bucket.upload(rutaArchivo, bytes) {
                    upsert = true
                }
                val urlPublica = bucket.publicUrl(rutaArchivo)
                Result.success(urlPublica)
            } catch (e: Exception) {
                println("ERROR STORAGE: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // --- 5. ACTUALIZAR DATO EN TABLA USUARIOS (Fotos o Descripción) ---
    suspend fun actualizarFotoTabla(correo: String, columna: String, valor: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                tablaUsuarios.update({
                    set(columna, valor)
                }) {
                    filter { eq("correoElectronico", correo) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- 6. BIBLIOTECA: OBTENER LIBROS POR ESTADO ---
    suspend fun obtenerLibrosPorEstado(idUsuario: Long, estado: String): Result<List<MiLibro>> {
        return withContext(Dispatchers.IO) {
            try {
                val lista = tablaBiblioteca.select {
                    filter {
                        eq("id_usuario", idUsuario) // Ahora filtramos por la nueva columna
                        eq("estado", estado)
                    }
                }.decodeList<MiLibro>()
                Result.success(lista)
            } catch (e: Exception) {
                println("❌ Error Supabase Biblioteca: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // --- 7. BIBLIOTECA: GUARDAR O ACTUALIZAR LIBRO (Upsert) ---
    // Esta función sirve para añadir de "Search" o cambiar de estado en "Biblioteca"
    suspend fun actualizarLibroEnBiblioteca(libro: MiLibro): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Upsert inserta si no existe el book_key para ese usuario, o actualiza si ya existe
                tablaBiblioteca.upsert(libro)
                Result.success(Unit)
            } catch (e: Exception) {
                println("❌ Error al guardar libro: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // --- 8. BIBLIOTECA: ACTUALIZAR SOLO EL PROGRESO ---
    suspend fun actualizarProgreso(
        idLibro: Int,
        nuevoProgreso: Int,
        nuevoEstado: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                tablaBiblioteca.update({
                    set("progreso_porcentaje", nuevoProgreso) // <-- Nombre exacto en Supabase
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

    // 2. Añade un libro a la tabla favoritos
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

    // 3. Elimina un libro de tu biblioteca (mislibros)
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
                // 1. Fíjate que ahora usamos select { ... } con LLAVES
                val filasFavoritos = client.from("favoritos")
                    .select {
                        filter { eq("usuario_id", idUsuario) }
                    }.decodeList<Favorito>()

                val idsLibros = filasFavoritos.map { it.libro_id }

                if (idsLibros.isEmpty()) return@withContext Result.success(emptyList())

                // 2. Buscamos los datos reales en 'mislibros'
                val libros = client.from("mislibros")
                    .select {
                        // Usamos 'isIn' en lugar de `in` para evitar conflictos en Kotlin
                        filter { isIn("id", idsLibros) }
                    }.decodeList<MiLibro>()

                Result.success(libros)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    // Busca usuarios por su nickname (ignorando mayúsculas y minúsculas)
    suspend fun buscarUsuarios(query: String): Result<List<Usuario>> {
        return withContext(Dispatchers.IO) {
            try {
                // 👇 AQUÍ ESTÁ EL CAMBIO: Pon la U mayúscula ("Usuarios")
                val usuariosEncontrados = client.from("Usuarios")
                    .select {
                        filter {
                            ilike("nickname", "%$query%")
                        }
                    }.decodeList<Usuario>()

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

                if (usuarios.isNotEmpty()) {
                    Result.success(usuarios.first())
                } else {
                    Result.failure(Exception("Usuario no encontrado"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    suspend fun comprobarSiSigue(idSeguidor: Long, idSeguido: Long): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val relacion = client.from("seguidores")
                    .select {
                        filter {
                            eq("seguidor_id", idSeguidor)
                            eq("seguido_id", idSeguido)
                        }
                    }.decodeList<SeguidorRelacion>()

                // Si la lista no está vacía, es que ya lo sigue
                Result.success(relacion.isNotEmpty())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // 2. Empieza a seguir a un usuario
    suspend fun seguirUsuario(idSeguidor: Long, idSeguido: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val nuevaRelacion = SeguidorRelacion(seguidor_id = idSeguidor, seguido_id = idSeguido)
                client.from("seguidores").insert(nuevaRelacion)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // 3. Deja de seguir a un usuario
    suspend fun dejarDeSeguirUsuario(idSeguidor: Long, idSeguido: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("seguidores").delete {
                    filter {
                        eq("seguidor_id", idSeguidor)
                        eq("seguido_id", idSeguido)
                    }
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

    // Cuenta a cuánta gente sigue este usuario
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
                    .select {
                        // Al pedir "*, Usuarios(*)", Supabase hace la magia de unir las dos tablas
                        // usando la Foreign Key de usuario_id que creaste.
                        // Nota: Dependiendo de tu versión del SDK, a veces basta solo con .select()
                    }
                    .decodeList<PublicacionFeed>()

                // Le damos la vuelta a la lista para que las más nuevas salgan arriba
                Result.success(lista.reversed())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
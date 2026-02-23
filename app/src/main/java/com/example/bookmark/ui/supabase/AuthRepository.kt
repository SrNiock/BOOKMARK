package com.example.bookmark.ui.supaBase

import com.example.bookmark.ui.supaBase.SupabaseClient.client
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {

    private val tablaUsuarios = client.from("Usuarios")
    private val tablaBiblioteca = client.from("MisLibros") // Nueva tabla de biblioteca

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
    suspend fun obtenerLibrosPorEstado(correo: String, estado: String): Result<List<MiLibro>> {
        return withContext(Dispatchers.IO) {
            try {
                val lista = tablaBiblioteca.select {
                    filter {
                        eq("correo_usuario", correo)
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
    suspend fun actualizarProgreso(idLibro: Int, nuevoProgreso: Int, nuevoEstado: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                tablaBiblioteca.update({
                    set("progreso_percentage", nuevoProgreso)
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

    // --- 9. BIBLIOTECA: ELIMINAR LIBRO ---
    suspend fun eliminarLibro(idLibro: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                tablaBiblioteca.delete {
                    filter { eq("id", idLibro) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
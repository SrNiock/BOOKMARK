package com.example.bookmark.ui.supaBase

import com.example.bookmark.ui.supaBase.SupabaseClient
import com.example.bookmark.ui.supaBase.Usuario
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage // Extensión necesaria para Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {

    private val tablaUsuarios = SupabaseClient.client.from("Usuarios")

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

    // --- 3. OBTENER DATOS DEL USUARIO (Para el Perfil) ---
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

    // --- 4. SUBIR IMAGEN A STORAGE ---
    suspend fun subirImagenStorage(rutaArchivo: String, bytes: ByteArray): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val bucket = SupabaseClient.client.storage.from("perfiles")

                // Si esto falla por falta de permisos (RLS), saltará al catch
                bucket.upload(rutaArchivo, bytes) {
                    upsert = true
                }

                val urlPublica = bucket.publicUrl(rutaArchivo)
                Result.success(urlPublica)
            } catch (e: Exception) {
                // Si hay error, imprimimos en consola para que lo veas en Logcat
                println("ERROR STORAGE: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // --- 5. ACTUALIZAR FOTO EN LA TABLA ---
    suspend fun actualizarFotoTabla(correo: String, columna: String, urlFoto: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                tablaUsuarios.update({
                    set(columna, urlFoto)
                }) {
                    filter { eq("correoElectronico", correo) }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
package com.example.bookmark.ui.supabase


import com.example.bookmark.ui.supabase.SupabaseClient
import com.example.bookmark.ui.data.models.Usuario
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {

    // Cambia "usuarios" por el nombre real de tu tabla si es diferente
    private val tablaUsuarios = SupabaseClient.client.from("Usuarios")

    suspend fun login(correo: String, contrasena: String): Result<Usuario> {
        return withContext(Dispatchers.IO) {
            try {
                // Buscamos un usuario que tenga ese correo Y esa contraseña
                val usuariosEncontrados = tablaUsuarios.select {
                    filter {
                        eq("correoElectronico", correo)
                        eq("contraseña", contrasena)
                    }
                }.decodeList<Usuario>()

                if (usuariosEncontrados.isNotEmpty()) {
                    // Login exitoso, devolvemos el primer usuario encontrado
                    Result.success(usuariosEncontrados.first())
                } else {
                    // No se encontró coincidencia
                    Result.failure(Exception("Correo o contraseña incorrectos"))
                }
            } catch (e: Exception) {
                // Error de red, base de datos caída, etc.
                Result.failure(e)
            }
        }
    }
}
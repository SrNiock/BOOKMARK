@file:OptIn(SupabaseInternal::class)

package com.example.bookmark.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.BuildConfig
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan_tennert.supabase.createSupabaseClient
import io.github.jan_tennert.supabase.postgrest.Postgrest
import io.github.jan_tennert.supabase.postgrest.from
import io.ktor.websocket.WebSocketDeflateExtension.Companion.install
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch

// 1. Modelo de datos (Debe coincidir con tu tabla de Supabase)
@Serializable
data class Usuario(
    val id: Long? = null,
    val nombre: String,
    val apellidos: String,
    val correoElectronico: String,
    val perfilPublico: Boolean,
    val fotoPerfil: String? = null
)

class MainActivity : ComponentActivity() {

    // 2. Inicialización del cliente de Supabase usando tus llaves secretas
    private val supabase = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ListaUsuariosScreen()
                }
            }
        }
    }

    @Composable
    fun ListaUsuariosScreen() {
        // Estado para almacenar la lista de usuarios
        var listaUsuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
        var cargando by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()

        // 3. Cargar datos al iniciar la pantalla
        LaunchedEffect(Unit) {
            scope.launch {
                try {
                    val resultado = supabase.from("usuarios")
                        .select()
                        .decodeList<Usuario>()
                    listaUsuarios = resultado
                } catch (e: Exception) {
                    Log.e("SUPABASE_ERROR", "Error: ${e.message}")
                } finally {
                    cargando = false
                }
            }
        }

        // 4. Interfaz de usuario
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Usuarios en Supabase",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (cargando) {
                CircularProgressIndicator()
            } else if (listaUsuarios.isEmpty()) {
                Text("No se encontraron usuarios.")
            } else {
                LazyColumn {
                    items(listaUsuarios) { usuario ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "${usuario.nombre} ${usuario.apellidos}", style = MaterialTheme.typography.titleMedium)
                                Text(text = usuario.correoElectronico, style = MaterialTheme.typography.bodySmall)
                                if (usuario.perfilPublico) {
                                    Text(text = "Perfil Público", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
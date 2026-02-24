package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.bookmark.ui.navigation.Screen
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.supaBase.MiLibro
import com.example.bookmark.ui.supaBase.Usuario
import com.example.bookmark.ui.utils.SessionManager
import kotlinx.coroutines.launch

@Composable
fun ExternalUserScreen(userId: Long, navController: NavHostController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val authRepository = remember { AuthRepository() }
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // ID del usuario que está usando la app
    val miIdActual = sessionManager.obtenerIdSesion() ?: 0L

    // Estados
    var usuarioVisualizado by remember { mutableStateOf<Usuario?>(null) }
    var listaFavoritos by remember { mutableStateOf<List<MiLibro>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    // Estados para el seguimiento
    var isSiguiendo by remember { mutableStateOf(false) }
    var procesandoSeguimiento by remember { mutableStateOf(false) }

    // Carga de datos inicial
    LaunchedEffect(userId) {
        // 1. Cargamos los datos del usuario
        authRepository.obtenerUsuarioPorId(userId).onSuccess { user ->
            usuarioVisualizado = user
        }
        // 2. Cargamos sus libros favoritos reales
        authRepository.obtenerFavoritos(userId).onSuccess { favs ->
            listaFavoritos = favs.take(4)
        }
        // 3. Comprobamos si el usuario actual ya lo sigue
        if (miIdActual != 0L) {
            authRepository.comprobarSiSigue(idSeguidor = miIdActual, idSeguido = userId).onSuccess { loSigue ->
                isSiguiendo = loSigue
            }
        }
        cargando = false
    }

    if (cargando) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val usuario = usuarioVisualizado
    if (usuario == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text("Usuario no encontrado", color = Color.Gray)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // --- SECCIÓN 1: BANNER, FOTO Y BOTÓN VOLVER ---
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            // El Banner
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (!usuario.fotoBanner.isNullOrEmpty()) {
                    AsyncImage(model = usuario.fotoBanner, contentDescription = "Banner", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }

            // Botón de Volver atrás
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(top = 40.dp, start = 8.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }

            // La Foto de Perfil
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 24.dp, y = 10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!usuario.fotoPerfil.isNullOrEmpty()) {
                    AsyncImage(model = usuario.fotoPerfil, contentDescription = "Perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.SupervisedUserCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(50.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN 2: INFO Y BOTÓN SEGUIR ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "@${usuario.nickname}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

                if (!usuario.descripcion.isNullOrEmpty()) {
                    Text(text = usuario.descripcion, fontSize = 14.sp, color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))
                }
            }

            // Botón Seguir (AHORA FUNCIONAL CON LA BASE DE DATOS)
            if (miIdActual != userId && miIdActual != 0L) {
                Button(
                    onClick = {
                        if (procesandoSeguimiento) return@Button // Evita dobles clics
                        procesandoSeguimiento = true

                        coroutineScope.launch {
                            if (isSiguiendo) {
                                // Dejar de seguir
                                authRepository.dejarDeSeguirUsuario(miIdActual, userId).onSuccess {
                                    isSiguiendo = false
                                }
                            } else {
                                // Seguir
                                authRepository.seguirUsuario(miIdActual, userId).onSuccess {
                                    isSiguiendo = true
                                }
                            }
                            procesandoSeguimiento = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSiguiendo) Color.DarkGray else MaterialTheme.colorScheme.primary,
                        contentColor = if (isSiguiendo) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    if (procesandoSeguimiento) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text = if (isSiguiendo) "Siguiendo" else "Seguir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECCIÓN 3: AMISTADES ---
        Text("Amistades de ${usuario.nickname}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text("Esta lista es privada o no tiene amigos añadidos.", color = Color.Gray, modifier = Modifier.padding(16.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECCIÓN 4: FAVORITOS ---
        Text("Favoritos de ${usuario.nickname}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { index ->
                val libro = listaFavoritos.getOrNull(index)
                if (libro != null) {
                    AsyncImage(
                        model = "https://covers.openlibrary.org/b/id/${libro.cover_id}-L.jpg",
                        contentDescription = libro.titulo,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                val rutaSegura = Uri.encode(libro.bookKey)
                                navController.navigate(Screen.BookDetail(bookKey = rutaSegura))
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    BookPlaceholderBox(modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}
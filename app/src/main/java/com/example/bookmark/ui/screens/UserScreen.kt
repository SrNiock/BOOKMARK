package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import coil.compose.AsyncImage
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.utils.SessionManager
import kotlinx.coroutines.launch
import kotlin.onFailure

@Composable
fun UserScreen() {
    val scrollState = rememberScrollState()

    // --- HERRAMIENTAS ---
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }
    val correoActual = sessionManager.obtenerCorreoSesion() ?: ""
    val coroutineScope = rememberCoroutineScope()

    // --- ESTADOS DE LA PANTALLA ---
    var profileUrl by remember { mutableStateOf<String?>(null) }
    var bannerUrl by remember { mutableStateOf<String?>(null) }
    var nicknameUsuario by remember { mutableStateOf("Cargando...") }

    // 🔥 Nuevos estados para la descripción
    var descripcionUsuario by remember { mutableStateOf("") }
    var editandoDescripcion by remember { mutableStateOf(false) }

    var estaSubiendoPerfil by remember { mutableStateOf(false) }
    var estaSubiendoBanner by remember { mutableStateOf(false) }

    // --- CARGA INICIAL (Leer BD al entrar) ---
    LaunchedEffect(correoActual) {
        if (correoActual.isNotEmpty()) {
            val resultado = authRepository.obtenerUsuario(correoActual)

            resultado.onSuccess { usuario ->
                nicknameUsuario = usuario.nickname
                profileUrl = usuario.fotoPerfil
                bannerUrl = usuario.fotoBanner
                // 🔥 Cargamos la descripción (si es nula, ponemos texto vacío)
                descripcionUsuario = usuario.descripcion ?: ""
            }.onFailure {
                nicknameUsuario = "Error de conexión"
            }
        } else {
            nicknameUsuario = "Usuario no identificado"
        }
    }

    // --- LANZADORES DE LA GALERÍA ---
    // 1. Para el Banner
    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                estaSubiendoBanner = true
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val nombreArchivo = "banner_$correoActual.jpg"
                        authRepository.subirImagenStorage(nombreArchivo, bytes).onSuccess { urlSubida ->
                            authRepository.actualizarFotoTabla(correoActual, "fotoBanner", urlSubida).onSuccess {
                                bannerUrl = urlSubida
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("❌ ERROR INTERNO BANNER: ${e.message}")
                } finally {
                    estaSubiendoBanner = false
                }
            }
        }
    }

    // 2. Para la Foto de Perfil
    val profilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                estaSubiendoPerfil = true
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val nombreArchivo = "perfil_$correoActual.jpg"
                        authRepository.subirImagenStorage(nombreArchivo, bytes).onSuccess { urlSubida ->
                            authRepository.actualizarFotoTabla(correoActual, "fotoPerfil", urlSubida).onSuccess {
                                profileUrl = urlSubida
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("❌ ERROR INTERNO PERFIL: ${e.message}")
                } finally {
                    estaSubiendoPerfil = false
                }
            }
        }
    }

    // Colores
    val backgroundColor = Color(0xFF121212)
    val surfaceColor = Color(0xFF1E1E1E)
    val accentColor = Color(0xFF00E5FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(scrollState)
    ) {
        // --- SECCIÓN 1: BANNER Y FOTO DE PERFIL ---
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp)
        ) {
            // 1.1 El Banner (Fondo)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.DarkGray)
                    .clickable {
                        bannerPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!bannerUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = bannerUrl,
                        contentDescription = "Banner del usuario",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Toca para añadir un Banner", color = Color.White)
                }

                if (estaSubiendoBanner) {
                    CircularProgressIndicator(color = accentColor)
                }
            }

            // 1.2 La Foto de Perfil (Superpuesta)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 24.dp, y = 10.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .border(3.dp, accentColor, CircleShape)
                    .clickable {
                        profilePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!profileUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = profileUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Foto", color = Color.White)
                }

                if (estaSubiendoPerfil) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN 2: INFO DEL USUARIO ---
        Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            Text(
                text = nicknameUsuario,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // 🔥 Lógica de la Descripción Editable
            if (editandoDescripcion) {
                OutlinedTextField(
                    value = descripcionUsuario,
                    onValueChange = { descripcionUsuario = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    placeholder = { Text("Escribe algo sobre ti...") },
                    trailingIcon = {
                        IconButton(onClick = {
                            editandoDescripcion = false // Salimos del modo edición
                            // Guardamos en Supabase reusando la función de actualizar celdas
                            coroutineScope.launch {
                                authRepository.actualizarFotoTabla(correoActual, "descripcion", descripcionUsuario)
                            }
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = "Guardar", tint = accentColor)
                        }
                    }
                )
            } else {
                Text(
                    text = if (descripcionUsuario.isEmpty()) "Añade una descripción sobre ti... ✏️" else descripcionUsuario,
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { editandoDescripcion = true } // Al tocar, cambia a modo edición
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECCIÓN 3: AMISTADES ---
        Text(
            text = "Amistades", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Text(
                text = "Aún no tienes amigos añadidos. ¡Pronto podrás buscarlos aquí!",
                color = Color.Gray, modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECCIÓN 4: LOS 4 LIBROS FAVORITOS ---
        Text(
            text = "Mis 4 Favoritos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BookPlaceholderBox(modifier = Modifier.weight(1f))
            BookPlaceholderBox(modifier = Modifier.weight(1f))
            BookPlaceholderBox(modifier = Modifier.weight(1f))
            BookPlaceholderBox(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun BookPlaceholderBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("Libro", color = Color.Gray, fontSize = 12.sp)
    }
}
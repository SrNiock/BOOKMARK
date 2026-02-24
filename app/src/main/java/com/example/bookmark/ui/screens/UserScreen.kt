package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.example.bookmark.ui.supaBase.MiLibro
import com.example.bookmark.ui.utils.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
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

    var descripcionUsuario by remember { mutableStateOf("") }
    var editandoDescripcion by remember { mutableStateOf(false) }

    var estaSubiendoPerfil by remember { mutableStateOf(false) }
    var estaSubiendoBanner by remember { mutableStateOf(false) }

    val idActual = sessionManager.obtenerIdSesion() ?: 0L
    var listaFavoritos by remember { mutableStateOf<List<MiLibro>>(emptyList()) }

    // 👇 NUEVOS ESTADOS PARA LOS CONTADORES
    var totalSeguidores by remember { mutableStateOf(0L) }
    var totalSeguidos by remember { mutableStateOf(0L) }

    // --- CARGA INICIAL (Leer BD al entrar) ---
    LaunchedEffect(correoActual) {
        if (correoActual.isNotEmpty()) {
            val resultado = authRepository.obtenerUsuario(correoActual)

            resultado.onSuccess { usuario ->
                nicknameUsuario = usuario.nickname
                profileUrl = usuario.fotoPerfil
                bannerUrl = usuario.fotoBanner
                descripcionUsuario = usuario.descripcion ?: ""

                // Carga de favoritos
                authRepository.obtenerFavoritos(idActual).onSuccess { favs ->
                    listaFavoritos = favs.take(4)
                }

                // 👇 CARGA DE SEGUIDORES Y SEGUIDOS
                authRepository.contarSeguidores(idActual).onSuccess { totalSeguidores = it }
                authRepository.contarSeguidos(idActual).onSuccess { totalSeguidos = it }

            }.onFailure {
                nicknameUsuario = "Error de conexión"
            }
        } else {
            nicknameUsuario = "Usuario no identificado"
        }
    }

    // --- LANZADORES DE LA GALERÍA ---
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

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color.DarkGray,
        cursorColor = MaterialTheme.colorScheme.primary,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // --- SECCIÓN 1: BANNER Y FOTO DE PERFIL ---
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        bannerPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!bannerUrl.isNullOrEmpty()) {
                    AsyncImage(model = bannerUrl, contentDescription = "Banner", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text("Toca para añadir un Banner", color = Color.Gray)
                }
                if (estaSubiendoBanner) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 24.dp, y = 10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable {
                        profilePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!profileUrl.isNullOrEmpty()) {
                    AsyncImage(model = profileUrl, contentDescription = "Perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text("Foto", color = Color.Gray)
                }
                if (estaSubiendoPerfil) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN 2: INFO DEL USUARIO ---
        Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            Text(text = nicknameUsuario, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            if (editandoDescripcion) {
                OutlinedTextField(
                    value = descripcionUsuario,
                    onValueChange = { descripcionUsuario = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = textFieldColors,
                    placeholder = { Text("Escribe algo sobre ti...", color = Color.Gray) },
                    trailingIcon = {
                        IconButton(onClick = {
                            editandoDescripcion = false
                            coroutineScope.launch {
                                authRepository.actualizarFotoTabla(correoActual, "descripcion", descripcionUsuario)
                            }
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            } else {
                Text(
                    text = if (descripcionUsuario.isEmpty()) "Añade una descripción sobre ti... ✏️" else descripcionUsuario,
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 8.dp).clickable { editandoDescripcion = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN 3: SEGUIDORES Y SEGUIDOS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = totalSeguidores.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(text = "Seguidores", fontSize = 14.sp, color = Color.Gray)
            }

            VerticalDivider(color = Color.DarkGray, modifier = Modifier.height(30.dp).width(1.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = totalSeguidos.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(text = "Seguidos", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECCIÓN 4: LOS 4 LIBROS FAVORITOS ---
        Text(
            text = "Mis 4 Favoritos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(4) { index ->
                val libro = listaFavoritos.getOrNull(index)

                if (libro != null) {
                    var mostrarMenuFav by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.weight(1f)) {
                        AsyncImage(
                            model = "https://covers.openlibrary.org/b/id/${libro.cover_id}-L.jpg",
                            contentDescription = libro.titulo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .combinedClickable(
                                    onClick = { /* Navegar a detalles si quieres */ },
                                    onLongClick = { mostrarMenuFav = true }
                                ),
                            contentScale = ContentScale.Crop
                        )

                        DropdownMenu(
                            expanded = mostrarMenuFav,
                            onDismissRequest = { mostrarMenuFav = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Eliminar de favoritos", color = Color.Red) },
                                onClick = {
                                    mostrarMenuFav = false
                                    coroutineScope.launch {
                                        libro.id?.let { idLibro ->
                                            authRepository.eliminarDeFavoritos(idActual, idLibro).onSuccess {
                                                listaFavoritos = listaFavoritos.filter { it.id != idLibro }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    BookPlaceholderBox(modifier = Modifier.weight(1f))
                }
            }
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
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("Libro", color = Color.Gray, fontSize = 12.sp)
    }
}
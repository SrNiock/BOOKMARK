package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.bookmark.ui.navigation.Screen
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.utils.SessionManager
import com.example.bookmark.ui.supaBase.MiLibro
import com.example.bookmark.ui.supaBase.Publicacion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibliotecaScreen(navController: NavHostController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    val idActual = sessionManager.obtenerIdSesion() ?: -1L

    val snackbarHostState = remember { SnackbarHostState() }

    var tabIndex by remember { mutableStateOf(0) }
    val pestañas = listOf("Leyendo", "Biblioteca", "Pendientes")

    var listaLibros by remember { mutableStateOf<List<MiLibro>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    // Estados para el progreso
    var mostrarDialogo by remember { mutableStateOf(false) }
    var libroSeleccionado by remember { mutableStateOf<MiLibro?>(null) }
    var paginaInput by remember { mutableStateOf("") }

    // 👇 ESTADOS NUEVOS PARA LA RESEÑA
    var mostrarDialogoResena by remember { mutableStateOf(false) }
    var textoResena by remember { mutableStateOf("") }
    var calificacionResena by remember { mutableStateOf(5) } // Por defecto 5 estrellas

    // Función para refrescar datos
    fun cargarDatos() {
        cargando = true
        val estadoBuscado = when(tabIndex) {
            0 -> "leyendo"
            1 -> "terminado"
            else -> "deseado"
        }
        coroutineScope.launch {
            val resultado = authRepository.obtenerLibrosPorEstado(idActual, estadoBuscado)
            listaLibros = resultado.getOrNull() ?: emptyList()
            cargando = false
        }
    }

    LaunchedEffect(tabIndex) { cargarDatos() }

    // --- ACCIONES DEL MENÚ ---
    val onFavorito: (MiLibro) -> Unit = { libro ->
        if (libro.id != null) {
            coroutineScope.launch {
                val conteo = authRepository.contarFavoritos(idActual).getOrDefault(0L)
                if (conteo >= 4) {
                    snackbarHostState.showSnackbar("No se pueden añadir más de 4 favoritos")
                } else {
                    authRepository.agregarAFavoritos(idActual, libro.id).onSuccess {
                        snackbarHostState.showSnackbar("Añadido a favoritos ❤️")
                    }.onFailure {
                        snackbarHostState.showSnackbar("El libro ya está en favoritos")
                    }
                }
            }
        }
    }

    val onEliminar: (MiLibro) -> Unit = { libro ->
        if (libro.id != null) {
            coroutineScope.launch {
                authRepository.eliminarLibroDeBiblioteca(libro.id).onSuccess {
                    cargarDatos()
                    snackbarHostState.showSnackbar("Libro eliminado de tu biblioteca")
                }
            }
        }
    }

    val onMoverALeyendo: (MiLibro) -> Unit = { libro ->
        coroutineScope.launch {
            val libroActualizado = libro.copy(estado = "leyendo")
            authRepository.actualizarLibroEnBiblioteca(libroActualizado).onSuccess {
                cargarDatos()
                snackbarHostState.showSnackbar("Movido a Leyendo")
            }
        }
    }

    // --- DIÁLOGO 1: ACTUALIZAR PÁGINAS ---
    if (mostrarDialogo && libroSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Actualizar Progreso", color = Color.White) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                Column {
                    Text("Páginas totales: 300 (Simulado)", color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = paginaInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) paginaInput = it },
                        label = { Text("¿En qué página estás?") },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    // 👇 BOTÓN NUEVO: Salta a la ventana de reseña
                    TextButton(onClick = {
                        mostrarDialogo = false
                        mostrarDialogoResena = true // Abrimos el nuevo diálogo
                    }) {
                        Text("¡Ya lo he terminado!", color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(onClick = {
                        val paginasLeidas = paginaInput.toIntOrNull() ?: 0
                        val nuevoPorcentaje = (paginasLeidas * 100) / 300
                        listaLibros = listaLibros.map {
                            if (it.id == libroSeleccionado!!.id) {
                                it.copy(progreso_porcentaje = if(nuevoPorcentaje > 100) 100 else nuevoPorcentaje)
                            } else it
                        }
                        mostrarDialogo = false
                    }) { Text("Calcular") }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    // --- DIÁLOGO 2: ESCRIBIR RESEÑA AL TERMINAR ---
    if (mostrarDialogoResena && libroSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoResena = false },
            title = { Text("Reseña: ${libroSeleccionado!!.titulo}", color = Color.White) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Selector de Estrellas
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Estrella $i",
                                // Amarillo si está seleccionada, gris oscuro si no
                                tint = if (i <= calificacionResena) Color(0xFFFFD700) else Color.DarkGray,
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(4.dp)
                                    .clickable { calificacionResena = i }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = textoResena,
                        onValueChange = { textoResena = it },
                        label = { Text("¿Qué te ha parecido el libro?", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        // 1. Crear y subir la publicación a Supabase
                        val nuevaPublicacion = Publicacion(
                            usuario_id = idActual,
                            book_key = libroSeleccionado!!.bookKey,
                            titulo_libro = libroSeleccionado!!.titulo,
                            cover_id = libroSeleccionado!!.cover_id,
                            texto = textoResena,
                            calificacion = calificacionResena
                        )
                        authRepository.crearPublicacion(nuevaPublicacion)

                        // 2. Mover el libro a "terminados" y ponerle el 100%
                        val libroActualizado = libroSeleccionado!!.copy(
                            estado = "terminado",
                            progreso_porcentaje = 100
                        )
                        authRepository.actualizarLibroEnBiblioteca(libroActualizado)

                        // 3. Limpiar variables y recargar pantalla
                        mostrarDialogoResena = false
                        textoResena = ""
                        calificacionResena = 5
                        cargarDatos()
                        snackbarHostState.showSnackbar("¡Enhorabuena! Libro terminado y reseña publicada 🎉")
                    }
                }) { Text("Publicar y Terminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoResena = false }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Text("Mi Biblioteca", modifier = Modifier.padding(20.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold)

            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                pestañas.forEachIndexed { index, titulo ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = {
                            Text(titulo, color = if(tabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    )
                }
            }

            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            } else {
                if (tabIndex == 0) {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(listaLibros) { libro ->
                            LibroBibliotecaItem(
                                libro = libro,
                                tabIndex = tabIndex,
                                onClick = {
                                    libroSeleccionado = libro
                                    paginaInput = ""
                                    mostrarDialogo = true
                                },
                                onEliminar = { onEliminar(libro) },
                                onFavorito = { onFavorito(libro) },
                                onMoverALeyendo = { onMoverALeyendo(libro) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(listaLibros) { libro ->
                            LibroGridItem(
                                libro = libro,
                                tabIndex = tabIndex,
                                navController = navController,
                                onEliminar = { onEliminar(libro) },
                                onFavorito = { onFavorito(libro) },
                                onMoverALeyendo = { onMoverALeyendo(libro) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- ITEM LEYENDO ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibroBibliotecaItem(
    libro: MiLibro,
    tabIndex: Int,
    onClick: () -> Unit,
    onEliminar: () -> Unit,
    onFavorito: () -> Unit,
    onMoverALeyendo: () -> Unit
) {
    var mostrarMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { mostrarMenu = true }
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                AsyncImage(
                    model = "https://covers.openlibrary.org/b/id/${libro.cover_id}-L.jpg",
                    contentDescription = null,
                    modifier = Modifier.size(60.dp, 90.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                    Text(text = libro.titulo, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = libro.autor ?: "Desconocido", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { libro.progreso_porcentaje / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${libro.progreso_porcentaje}%", fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
            if (tabIndex == 2) {
                DropdownMenuItem(text = { Text("Empezar a leer") }, onClick = { mostrarMenu = false; onMoverALeyendo() })
            } else {
                DropdownMenuItem(text = { Text("Añadir a favoritos ❤️") }, onClick = { mostrarMenu = false; onFavorito() })
            }
            DropdownMenuItem(text = { Text("Eliminar libro", color = Color.Red) }, onClick = { mostrarMenu = false; onEliminar() })
        }
    }
}

// --- ITEM CUADRÍCULA ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibroGridItem(
    libro: MiLibro,
    tabIndex: Int,
    navController: NavHostController,
    onEliminar: () -> Unit,
    onFavorito: () -> Unit,
    onMoverALeyendo: () -> Unit
) {
    var mostrarMenu by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        val rutaSegura = Uri.encode(libro.bookKey)
                        navController.navigate(Screen.BookDetail(bookKey = rutaSegura))
                    },
                    onLongClick = { mostrarMenu = true }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = "https://covers.openlibrary.org/b/id/${libro.cover_id}-L.jpg",
                contentDescription = null,
                modifier = Modifier.height(140.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Text(text = libro.titulo, fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }

        DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
            if (tabIndex == 2) {
                DropdownMenuItem(text = { Text("Empezar a leer") }, onClick = { mostrarMenu = false; onMoverALeyendo() })
            } else {
                DropdownMenuItem(text = { Text("Añadir a favoritos ❤️") }, onClick = { mostrarMenu = false; onFavorito() })
            }
            DropdownMenuItem(text = { Text("Eliminar libro", color = Color.Red) }, onClick = { mostrarMenu = false; onEliminar() })
        }
    }
}
package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.bookmark.ui.navigation.Screen // Importante para la navegación segura
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.utils.SessionManager
import com.example.bookmark.ui.supaBase.MiLibro
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

    var mostrarDialogo by remember { mutableStateOf(false) }
    var libroSeleccionado by remember { mutableStateOf<MiLibro?>(null) }
    var paginaInput by remember { mutableStateOf("") }

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

    // --- ACCIONES DEL MENÚ DESPLEGABLE ---
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
                    cargarDatos() // Refrescamos la lista
                    snackbarHostState.showSnackbar("Libro eliminado de tu biblioteca")
                }
            }
        }
    }

    // Acción: Mover de Pendientes a Leyendo
    val onMoverALeyendo: (MiLibro) -> Unit = { libro ->
        coroutineScope.launch {
            val libroActualizado = libro.copy(estado = "leyendo")
            authRepository.actualizarLibroEnBiblioteca(libroActualizado).onSuccess {
                cargarDatos() // Refrescamos para que desaparezca de pendientes
                snackbarHostState.showSnackbar("Movido a Leyendo")
            }.onFailure {
                snackbarHostState.showSnackbar("Error al mover el libro")
            }
        }
    }

    // --- DIÁLOGO DE ACTUALIZAR PÁGINAS (Local) ---
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
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar", color = Color.Gray) }
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (tabIndex == 0) {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(listaLibros) { libro ->
                            LibroBibliotecaItem(
                                libro = libro,
                                tabIndex = tabIndex,
                                onClick = {
                                    // En Leyendo, el clic abre el popup de páginas
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

// --- ITEM LEYENDO (Abre popup de progreso) ---
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
                    onClick = onClick, // Llama a la función que abre el popup
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

// --- ITEM CUADRÍCULA (Navega a detalles) ---
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
                        // Codificamos la URL para que no crashee y navegamos usando la clase Screen
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
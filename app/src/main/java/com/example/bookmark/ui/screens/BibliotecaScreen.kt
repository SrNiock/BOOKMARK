package com.example.bookmark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.utils.SessionManager
import com.example.bookmark.ui.supaBase.MiLibro

@Composable
fun BibliotecaScreen(navController: NavHostController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }
    val idActual = sessionManager.obtenerIdSesion() ?: -1L

    var tabIndex by remember { mutableStateOf(0) }
    val pestañas = listOf("Leyendo", "Biblioteca", "Pendientes")

    // Lista de libros que manejaremos en LOCAL
    var listaLibros by remember { mutableStateOf<List<MiLibro>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    // --- ESTADOS PARA EL DIÁLOGO (POPUP) ---
    var mostrarDialogo by remember { mutableStateOf(false) }
    var libroSeleccionado by remember { mutableStateOf<MiLibro?>(null) }
    var paginaInput by remember { mutableStateOf("") }

    // Carga inicial desde Supabase (solo al cambiar de pestaña)
    LaunchedEffect(tabIndex) {
        cargando = true
        val estadoBuscado = when(tabIndex) {
            0 -> "leyendo"
            1 -> "terminado"
            else -> "deseado"
        }
        val resultado = authRepository.obtenerLibrosPorEstado(idUsuario = idActual, estado = estadoBuscado)
        if (resultado.isSuccess) {
            listaLibros = resultado.getOrNull() ?: emptyList()
        }
        cargando = false
    }

    // --- VENTANA EMERGENTE (DIALOG) ---
    if (mostrarDialogo && libroSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Actualizar Progreso (Local)", color = Color.White) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                Column {
                    Text("Libro: ${libroSeleccionado!!.titulo}", color = Color.Gray, fontSize = 14.sp)
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
                    // CÁLCULO FALSO: Usamos siempre 300 páginas
                    val nuevoPorcentaje = (paginasLeidas * 100) / 300

                    // ACTUALIZACIÓN SOLO LOCAL: No llamamos a Supabase
                    listaLibros = listaLibros.map {
                        if (it.id == libroSeleccionado!!.id) {
                            it.copy(progreso_porcentaje = if(nuevoPorcentaje > 100) 100 else nuevoPorcentaje)
                        } else it
                    }

                    mostrarDialogo = false
                }) {
                    Text("Calcular")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text("Mi Biblioteca", modifier = Modifier.padding(20.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold)

        TabRow(selectedTabIndex = tabIndex, containerColor = Color.Transparent) {
            pestañas.forEachIndexed { index, titulo ->
                Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(titulo) })
            }
        }

        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            if (tabIndex == 0) {
                // PESTAÑA LEYENDO
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(listaLibros) { libro ->
                        LibroBibliotecaItem(
                            libro = libro,
                            onClick = {
                                libroSeleccionado = libro
                                paginaInput = ""
                                mostrarDialogo = true
                            }
                        )
                    }
                }
            } else {
                // PESTAÑAS CUADRÍCULA
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(listaLibros) { libro ->
                        LibroGridItem(libro, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun LibroBibliotecaItem(libro: MiLibro, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = "https://covers.openlibrary.org/b/id/${libro.cover_id}-L.jpg",
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp, 90.dp) // Tamaño fijo para que no se descuadre
                    .clip(RoundedCornerShape(8.dp)),
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
}

@Composable
fun LibroGridItem(libro: MiLibro, navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("book_detail/${libro.bookKey}") }, // Usamos bookKey como en tu clase
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = "https://covers.openlibrary.org/b/id/${libro.cover_id}-L.jpg",
            contentDescription = null,
            modifier = Modifier
                .height(140.dp) // Altura fija para que la cuadrícula sea perfecta
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = libro.titulo,
            fontSize = 11.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
            lineHeight = 14.sp
        )
    }
}
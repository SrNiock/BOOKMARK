package com.example.bookmark.ui.screens

import androidx.compose.foundation.background
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
import coil.compose.AsyncImage
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.utils.SessionManager
import com.example.bookmark.ui.supaBase.MiLibro

@Composable
fun BibliotecaScreen() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    val idActual = sessionManager.obtenerIdSesion() ?: -1L

    var tabIndex by remember { mutableStateOf(0) }
    val pestañas = listOf("Leyendo", "Biblioteca", "Pendientes")

    var listaLibros by remember { mutableStateOf<List<MiLibro>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    // Carga de datos según la pestaña seleccionada
    LaunchedEffect(tabIndex) {
        cargando = true

        val estadoBuscado = when(tabIndex) {
            0 -> "leyendo"
            1 -> "terminado"
            else -> "deseado"
        }

        val resultado = authRepository.obtenerLibrosPorEstado(idUsuario = idActual, estadoBuscado)

        if (resultado.isSuccess) {
            listaLibros = resultado.getOrNull() ?: emptyList()
            cargando = false
        } else {
            println("Error cargando biblioteca: ${resultado.exceptionOrNull()?.message}")
            cargando = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Mi Biblioteca",
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 16.dp),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Selector de Pestañas
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
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
                        Text(
                            titulo,
                            color = if(tabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray,
                            fontWeight = if(tabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (listaLibros.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay libros en esta sección", color = Color.Gray)
            }
        } else {
            // 👇 LÓGICA DE DISTRIBUCIÓN SEGÚN LA PESTAÑA 👇
            if (tabIndex == 0) {
                // PESTAÑA LEYENDO: Lista vertical normal para ver la barra de progreso
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(listaLibros) { libro ->
                        LibroBibliotecaItem(libro)
                    }
                }
            } else {
                // PESTAÑAS BIBLIOTECA Y PENDIENTES: Cuadrícula de 3 columnas
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), // Forzamos 3 columnas
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp) // Más espacio vertical para el texto
                ) {
                    items(listaLibros) { libro ->
                        LibroGridItem(libro)
                    }
                }
            }
        }
    }
}

// COMPONENTE NUEVO: Para la cuadrícula de 3x3
@Composable
fun LibroGridItem(libro: MiLibro) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Portada
        AsyncImage(
            model = "https://covers.openlibrary.org/b/id/${libro.cover_id}-L.jpg",
            contentDescription = libro.titulo,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.65f) // Proporción clásica de libro para que todos midan igual
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Título debajo
        Text(
            text = libro.titulo,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp, // Letra pequeñita para que quepa bien
            fontWeight = FontWeight.SemiBold,
            maxLines = 2, // Máximo 2 líneas de título
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// COMPONENTE ANTIGUO: Para la lista hacia abajo de "Leyendo"
@Composable
fun LibroBibliotecaItem(libro: MiLibro) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = "https://covers.openlibrary.org/b/id/${libro.cover_id}-L.jpg",
                contentDescription = null,
                modifier = Modifier
                    .size(70.dp, 105.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(
                    text = libro.titulo,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = libro.autor ?: "Autor desconocido",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                if (libro.estado == "leyendo") {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { libro.progreso_porcentaje / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.DarkGray
                    )
                    Text(
                        text = "${libro.progreso_porcentaje}% leído",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
package com.example.bookmark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.utils.SessionManager
import com.example.bookmark.ui.supaBase.MiLibro // Asegúrate de que el paquete sea correcto

@Composable
fun BibliotecaScreen() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }
    val correoActual = sessionManager.obtenerCorreoSesion() ?: ""
    val coroutineScope = rememberCoroutineScope()

    var tabIndex by remember { mutableStateOf(0) }
    val pestañas = listOf("Pendientes", "Leyendo", "Terminados")

    var listaLibros by remember { mutableStateOf<List<MiLibro>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    // Carga de datos según la pestaña seleccionada
    LaunchedEffect(tabIndex) {
        cargando = true
        val estadoBuscado = when(tabIndex) {
            0 -> "deseado"
            1 -> "leyendo"
            else -> "terminado"
        }

        // Forzamos el tipo de resultado para que no haya dudas
        val resultado = authRepository.obtenerLibrosPorEstado(correoActual, estadoBuscado)

        resultado.onSuccess { librosRecibidos ->
            listaLibros = librosRecibidos
            cargando = false
        }.onFailure { error ->
            println("Error cargando biblioteca: ${error.message}")
            cargando = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        Text(
            text = "Mi Biblioteca",
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 16.dp),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Selector de Pestañas
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF00E5FF),
            divider = {}, // Quitamos la línea molesta de abajo
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                    color = Color(0xFF00E5FF)
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
                            color = if(tabIndex == index) Color.White else Color.Gray,
                            fontWeight = if(tabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00E5FF))
            }
        } else if (listaLibros.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay libros en esta sección", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(listaLibros) { libro ->
                    LibroBibliotecaItem(libro)
                }
            }
        }
    }
}

@Composable
fun LibroBibliotecaItem(libro: MiLibro) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
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
                    color = Color.White,
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
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = Color(0xFFC2415E),
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
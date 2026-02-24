package com.example.bookmark.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bookmark.data.remote.BookUiState
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.supaBase.MiLibro
import com.example.bookmark.ui.utils.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookKey: String,
    viewModel: BookViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val idActual = sessionManager.obtenerIdSesion() ?: 0L

    // Estado para controlar el menú de añadir libro
    var menuAbierto by remember { mutableStateOf(false) }

    // Buscamos el libro en el estado actual del ViewModel
    val book = (viewModel.state.value as? BookUiState.Success)?.books?.find { it.key == bookKey }

    // Lógica reutilizable para guardar el libro en el estado que elijamos del menú
    val guardarLibro = { estadoElegido: String ->
        if (book != null) {
            coroutineScope.launch {
                val miLibro = MiLibro(
                    id_usuario = idActual, // <--- USAMOS EL ID AQUÍ
                    bookKey = book.key,    // Ojo con el nombre exacto de tu Data Class (bookKey o book_key)
                    titulo = book.title,
                    autor = book.authorNames?.firstOrNull(),
                    cover_id = book.coverId,
                    estado = estadoElegido,
                    progreso_porcentaje = 0
                )
                authRepository.actualizarLibroEnBiblioteca(miLibro).onSuccess {
                    val nombreLista = when(estadoElegido) {
                        "deseado" -> "Wishlist"
                        "leyendo" -> "Leyendo"
                        else -> "Terminados"
                    }
                    Toast.makeText(context, "Añadido a $nombreLista", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Detalles del libro", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (book == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se pudo cargar la información", color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Portada del Libro
                AsyncImage(
                    model = "https://covers.openlibrary.org/b/id/${book.coverId}-L.jpg",
                    contentDescription = book.title,
                    modifier = Modifier
                        .height(320.dp)
                        .width(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shadow(12.dp, RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Título y Autor
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = book.authorNames?.joinToString(", ") ?: "Autor desconocido",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary, // Naranja
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 3. Ficha Técnica con Botón de Añadir Múltiple
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface) // Gris oscuro
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoDetailColumn("Publicado", "${book.firstPublishYear ?: "---"}")

                    VerticalDivider(color = Color.DarkGray, modifier = Modifier.height(40.dp).width(1.dp))

                    // 👇 AQUÍ HEMOS CAMBIADO IDIOMA POR PÁGINAS 👇
                    InfoDetailColumn("Páginas", "${book.numeroPaginas ?: "---"}")

                    VerticalDivider(color = Color.DarkGray, modifier = Modifier.height(40.dp).width(1.dp))

                    // EL BOTÓN CON MENÚ (se queda exactamente igual)
                    Box {
                        IconButton(onClick = { menuAbierto = true }) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Añadir a...",
                                tint = MaterialTheme.colorScheme.primary, // Icono Naranja
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Menú Desplegable (Dropdown)
                        DropdownMenu(
                            expanded = menuAbierto,
                            onDismissRequest = { menuAbierto = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Añadir a Wishlist", color = MaterialTheme.colorScheme.onBackground) },
                                onClick = {
                                    menuAbierto = false
                                    guardarLibro("deseado")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Añadir a Leyendo", color = MaterialTheme.colorScheme.onBackground) },
                                onClick = {
                                    menuAbierto = false
                                    guardarLibro("leyendo")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Añadir a Terminados", color = MaterialTheme.colorScheme.onBackground) },
                                onClick = {
                                    menuAbierto = false
                                    guardarLibro("terminado")
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 4. Sinopsis o Descripción
                Text(
                    text = "Sobre este libro",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Explora esta increíble obra de ${book.authorNames?.firstOrNull() ?: "este autor"}. " +
                            "Publicado originalmente en ${book.firstPublishYear}, este libro sigue siendo una " +
                            "referencia imprescindible en su género. Añádelo a tu biblioteca para seguir tu progreso de lectura.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))
                // (Hemos eliminado el botón gigante de la parte inferior como pediste)
            }
        }
    }
}

@Composable
fun InfoDetailColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
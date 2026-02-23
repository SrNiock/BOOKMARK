package com.example.bookmark.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

    // Buscamos el libro en el estado actual del ViewModel
    val book = (viewModel.state.value as? BookUiState.Success)?.books?.find { it.key == bookKey }

    Scaffold(
        containerColor = Color(0xFF0F0F0F), // Negro mate
        topBar = {
            TopAppBar(
                title = { Text("Detalles del libro", color = Color.White, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
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
                // 1. Portada del Libro (Grande y con sombra)
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = book.authorNames?.joinToString(", ") ?: "Autor desconocido",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF00E5FF), // Color cyan de tu app
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 3. Ficha Técnica (Año e Idioma)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoDetailColumn("Publicado", "${book.firstPublishYear ?: "---"}")
                    VerticalDivider(color = Color.DarkGray, modifier = Modifier.height(40.dp).width(1.dp))
                    InfoDetailColumn("Idioma", book.language?.firstOrNull()?.uppercase() ?: "N/A")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 4. Sinopsis o Descripción
                Text(
                    text = "Sobre este libro",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
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

                // 5. Botón de Acción Principal
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val miLibro = MiLibro(
                                correo_usuario = sessionManager.obtenerCorreoSesion() ?: "",
                                book_key = book.key,
                                titulo = book.title,
                                autor = book.authorNames?.firstOrNull(),
                                cover_id = book.coverId,
                                estado = "deseado",
                                progreso_porcentaje = 0
                            )
                            authRepository.actualizarLibroEnBiblioteca(miLibro).onSuccess {
                                Toast.makeText(context, "¡Añadido a Pendientes!", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "QUIERO LEERLO",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun InfoDetailColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
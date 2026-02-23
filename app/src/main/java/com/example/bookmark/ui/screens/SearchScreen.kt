package com.example.bookmark.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.bookmark.R
import com.example.bookmark.data.remote.BookUiState
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.data.remote.dto.Book
import com.example.bookmark.ui.navigation.Screen
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.supaBase.MiLibro
import com.example.bookmark.ui.utils.SessionManager
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(bookViewModel: BookViewModel, navController: NavHostController) {
    // Fondo oscuro para toda la pantalla
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        BookScreen(bookViewModel, navController)
    }
}

@Composable
fun BookScreen(viewModel: BookViewModel, navController: NavHostController) {
    val state by viewModel.state
    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- BUSCADOR ---
        OutlinedTextField(
            value = searchText,
            onValueChange = { newText ->
                searchText = newText
                viewModel.onQueryChanged(newText)
            },
            label = { Text("Buscar libros...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color.DarkGray,
                cursorColor = Color(0xFF00E5FF)
            ),
            trailingIcon = {
                IconButton(onClick = { viewModel.searchBooks(searchText) }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray)
                }
            }
        )

        // --- GESTIÓN DE ESTADOS ---
        when (state) {
            is BookUiState.Loading -> LoadingView()
            is BookUiState.Error -> ErrorView((state as BookUiState.Error).message)
            is BookUiState.Success -> BookList((state as BookUiState.Success).books, viewModel, navController)
        }
    }
}

@Composable
fun BookList(books: List<Book>, viewModel: BookViewModel, navController: NavHostController) {
    val isLastPage by viewModel.isLastPage

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // Espacio para la Bottom Bar
    ) {
        itemsIndexed(books) { index, book ->
            BookCard(book, navController)

            // Paginación infinita
            if (index >= books.size - 3 && !isLastPage) {
                LaunchedEffect(books.size) {
                    viewModel.searchBooks(query = "", isNextPage = true)
                }
            }
        }

        if (!isLastPage && books.isNotEmpty()) {
            item {
                repeat(3) { SkeletonBookCard() }
            }
        } else if (isLastPage && books.isNotEmpty()) {
            item {
                Text(
                    text = "Has llegado al final de los resultados",
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BookCard(book: Book, navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val sessionManager = remember { SessionManager(context) }
    val correoActual = sessionManager.obtenerCorreoSesion() ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Navega a la pantalla de detalles usando la KEY única del libro
                navController.navigate(Screen.BookDetail(book.key))
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // --- PORTADA ---
            val coverUrl = book.coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
            Box(
                modifier = Modifier
                    .size(70.dp, 105.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.librologo)
                    )
                } else {
                    Icon(Icons.Default.Book, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- TEXTO ---
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.authorNames?.joinToString(", ") ?: "Autor desconocido",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // --- BOTÓN AÑADIR ---
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val miLibro = MiLibro(
                            correo_usuario = correoActual,
                            book_key = book.key,
                            titulo = book.title,
                            autor = book.authorNames?.firstOrNull(),
                            cover_id = book.coverId,
                            estado = "deseado",
                            progreso_porcentaje = 0
                        )
                        authRepository.actualizarLibroEnBiblioteca(miLibro).onSuccess {
                            Toast.makeText(context, "Añadido a Pendientes", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Añadir",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF00E5FF))
    }
}

@Composable
fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = Color(0xFFC2415E), modifier = Modifier.padding(20.dp), textAlign = TextAlign.Center)
    }
}

@Composable
fun SkeletonBookCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(70.dp, 105.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray.copy(alpha = 0.5f)))
            Spacer(Modifier.width(16.dp))
            Column {
                Box(modifier = Modifier.size(150.dp, 20.dp).background(Color.DarkGray.copy(alpha = 0.5f)))
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.size(100.dp, 15.dp).background(Color.DarkGray.copy(alpha = 0.5f)))
            }
        }
    }
}
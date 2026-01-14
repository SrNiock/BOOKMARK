package com.example.bookmark.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.bookmark.R
import com.example.bookmark.data.remote.BookUiState
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.data.remote.dto.Book

@Composable
fun SearchScreen(bookViewModel: BookViewModel) {
    Text(
        text = "Pantalla BUSQUEDAAAA!")

        BookScreen(bookViewModel)
}

@Composable
fun BookScreen(viewModel: BookViewModel) {
    val state = viewModel.state.value
    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Buscador
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Buscar libros...") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { viewModel.searchBooks(searchText) }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                }
            }
        )

        // Gestión de estados (Aquí se usan tus vistas de carga y error)
        when (state) {
            is BookUiState.Loading -> LoadingView()
            is BookUiState.Error -> ErrorView(state.message)
            is BookUiState.Success -> BookList(state.books)
        }
    }
}

// --- VISTAS DE ESTADO (Las que preguntabas) ---

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = Color.Red, modifier = Modifier.padding(20.dp))
    }
}

// --- LISTA Y CARDS ---

@Composable
fun BookList(books: List<Book>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        items(books) { book ->
            BookCard(book)
        }
    }
}

@Composable
fun BookCard(book: Book) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Construcción de la URL de la portada
            val coverUrl = book.coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
                ?: "https://via.placeholder.com/150"

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true) // Hace que la imagen aparezca con un suave degradado
                    .diskCachePolicy(CachePolicy.ENABLED) // Fuerza el guardado en memoria interna
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(80.dp, 120.dp),
                contentScale = ContentScale.Crop,
                // Muestra una imagen gris mientras descarga para que el usuario no vea un hueco blanco
                //placeholder = painterResource(R.drawable.placeholder_gray),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = book.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = book.authorNames?.joinToString(", ") ?: "Autor desconocido",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
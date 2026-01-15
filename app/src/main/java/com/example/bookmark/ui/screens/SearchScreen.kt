package com.example.bookmark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
            onValueChange = { newText ->
                searchText = newText // Actualiza lo que el usuario ve inmediatamente
                viewModel.onQueryChanged(newText) // Avisa al ViewModel para que empiece a contar
            },
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
            is BookUiState.Success -> BookList(state.books,viewModel)
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
fun BookList(books: List<Book>, viewModel: BookViewModel) {
    val isLastPage by viewModel.isLastPage
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        itemsIndexed(books) { index, book ->
            BookCard(book)

            if (index >= books.size - 3 && !isLastPage) {
                LaunchedEffect(books.size) {
                    viewModel.searchBooks(query = "", isNextPage = true)
                }
            }
        }

        // 2. EL CAMBIO CLAVE:
        // Solo añadimos el item de carga si NO hemos llegado al final
        if (!isLastPage && books.isNotEmpty()) {
            item {
                repeat(3) {
                    SkeletonBookCard()
                }
            }
        } else if (isLastPage && books.isNotEmpty()) {
            // OPCIONAL: Puedes poner un mensaje de que ya no hay más libros
            item {
                Text(
                    text = "Has llegado al final de la biblioteca",
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
fun SkeletonBookCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(80.dp, 120.dp).background(Color.Gray.copy(alpha = 0.2f)))
            Spacer(Modifier.width(16.dp))
            Column {
                Box(modifier = Modifier.size(150.dp, 20.dp).background(Color.Gray.copy(alpha = 0.2f)))
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.size(100.dp, 15.dp).background(Color.Gray.copy(alpha = 0.2f)))
            }
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
            Box(
                modifier = Modifier
                    .size(80.dp, 120.dp) // Forzamos a que el espacio sea siempre igual
                    .background(Color.LightGray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {

            if(coverUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true) // Hace que la imagen aparezca con un suave degradado
                        .diskCachePolicy(CachePolicy.ENABLED) // Fuerza el guardado en memoria interna
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    // Muestra una imagen gris mientras descarga para que el usuario no vea un hueco blanco
                    fallback = painterResource(R.drawable.librologo),
                    error = painterResource(R.drawable.librologo)
                )
            }else{
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Book, // Icono de un libro
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(40.dp)

                    )
                    Text(
                        text = "Sin Portada",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
            }
            }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = book.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = book.authorNames?.joinToString(", ") ?: "Autor desconocido",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis                )
            }
        }
    }
}
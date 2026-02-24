package com.example.bookmark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
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
import com.example.bookmark.ui.supaBase.Usuario
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(bookViewModel: BookViewModel, navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BookScreen(bookViewModel, navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(viewModel: BookViewModel, navController: NavHostController) {
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    // Estados de la barra de búsqueda
    var searchText by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf("Libros") } // Puede ser "Libros" o "Usuarios"
    var isMenuExpanded by remember { mutableStateOf(false) }

    // Estados para la búsqueda de Usuarios
    var userList by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var isSearchingUsers by remember { mutableStateOf(false) }
    var userSearchError by remember { mutableStateOf<String?>(null) }

    // Obtenemos el estado de los libros
    val bookState by viewModel.state

    // Función para buscar usuarios
    fun buscarUsuariosDB(query: String) {
        if (query.trim().isEmpty()) {
            userList = emptyList()
            return
        }
        isSearchingUsers = true
        userSearchError = null
        coroutineScope.launch {
            authRepository.buscarUsuarios(query).onSuccess { usuarios ->
                userList = usuarios
            }.onFailure { error ->
                userSearchError = "Error DB: ${error.message}"
                println("❌ ERROR EXACTO: ${error.message}")
            }
            isSearchingUsers = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- BARRA DE BÚSQUEDA ---
        OutlinedTextField(
            value = searchText,
            onValueChange = { newText ->
                searchText = newText
                // Búsqueda automática dependiendo del modo
                if (searchMode == "Libros") {
                    viewModel.onQueryChanged(newText)
                } else if (newText.length >= 3) {
                    // Si es usuario, esperamos a 3 letras
                    buscarUsuariosDB(newText)
                } else {
                    userList = emptyList()
                }
            },
            label = { Text("Buscar $searchMode...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.DarkGray,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            leadingIcon = {
                // Icono dinámico según el modo
                Icon(
                    imageVector = if (searchMode == "Libros") Icons.Default.Book else Icons.Default.Person,
                    contentDescription = "Modo",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botón de la lupa
                    IconButton(onClick = {
                        if (searchMode == "Libros") viewModel.searchBooks(searchText)
                        else buscarUsuariosDB(searchText)
                    }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray)
                    }

                    // Botón de los tres puntos (Menú)
                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray)
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Buscar Libros", color = if(searchMode == "Libros") MaterialTheme.colorScheme.primary else Color.LightGray) },
                                onClick = {
                                    searchMode = "Libros"
                                    isMenuExpanded = false
                                    searchText = "" // Limpiamos al cambiar
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Buscar Usuarios", color = if(searchMode == "Usuarios") MaterialTheme.colorScheme.primary else Color.LightGray) },
                                onClick = {
                                    searchMode = "Usuarios"
                                    isMenuExpanded = false
                                    searchText = "" // Limpiamos al cambiar
                                }
                            )
                        }
                    }
                }
            }
        )

        // --- RESULTADOS DE LA BÚSQUEDA ---
        if (searchMode == "Libros") {
            // Pintamos los libros usando tu lógica actual
            when (val currentState = bookState) {
                is BookUiState.Loading -> LoadingView()
                is BookUiState.Error -> ErrorView(currentState.message)
                is BookUiState.Success -> BookList(currentState.books, viewModel, navController)
            }
        } else {
            // Pintamos los Usuarios
            if (isSearchingUsers) {
                LoadingView()
            } else if (userSearchError != null) {
                ErrorView(userSearchError!!)
            } else if (userList.isEmpty() && searchText.isNotEmpty()) {
                ErrorView("No se encontraron usuarios")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(userList) { usuario ->
                        UserCard(
                            usuario = usuario,
                            navController = navController, // Aquí le pasamos el controlador
                            onSeguirClick = { /* TODO en el siguiente paso */ }
                        )
                    }
                }
            }
        }
    }
}

// --- NUEVO COMPONENTE: TARJETA DE USUARIO ---
@Composable
fun UserCard(
    usuario: Usuario,
    navController: NavHostController, // Ahora la función lo recibe correctamente
    onSeguirClick: () -> Unit
) {
    var isSiguiendo by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                // 👇 MAGIA AQUÍ: Navegamos pasándole el ID del usuario
                usuario.id?.let { idUsuario ->
                    navController.navigate(Screen.ExternalProfile(userId = idUsuario))
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Foto de Perfil redonda
            AsyncImage(
                model = usuario.fotoPerfil ?: R.drawable.librologo, // Usa tu icono por defecto si no tiene
                contentDescription = "Foto de ${usuario.nickname}",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Información del usuario
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${usuario.nickname}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!usuario.descripcion.isNullOrEmpty()) {
                    Text(
                        text = usuario.descripcion,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Botón de Seguir
            Button(
                onClick = { isSiguiendo = !isSiguiendo }, // Cambiamos estado visual por ahora
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSiguiendo) Color.DarkGray else MaterialTheme.colorScheme.primary,
                    contentColor = if (isSiguiendo) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = if (isSiguiendo) "Siguiendo" else "Seguir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Navega a la pantalla de detalles usando la KEY única del libro
                navController.navigate(Screen.BookDetail(book.key))
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Gris oscuro
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
                    .background(Color.DarkGray), // Placeholder mientras carga
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface, // Blanco/claro del tema
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
        }
    }
}

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) // Naranja
    }
}

@Composable
fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Usamos el color de error de nuestro tema
        Text(text = message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp), textAlign = TextAlign.Center)
    }
}

@Composable
fun SkeletonBookCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
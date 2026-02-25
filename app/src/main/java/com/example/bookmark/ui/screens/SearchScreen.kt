package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.runtime.derivedStateOf
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.data.remote.dto.Book
import com.example.bookmark.ui.navigation.Screen
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.supaBase.Usuario
import com.example.bookmark.ui.utils.SessionManager
import kotlinx.coroutines.launch


@Composable
fun SearchScreen(bookViewModel: BookViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }

    var usuarioActualId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        val correo = sessionManager.obtenerCorreoSesion() ?: ""
        if (correo.isNotEmpty()) {
            authRepository.obtenerUsuario(correo).onSuccess { usuario ->
                usuarioActualId = usuario.id
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BookScreen(
            viewModel = bookViewModel,
            navController = navController,
            usuarioActualId = usuarioActualId
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    viewModel: BookViewModel,
    navController: NavHostController,
    usuarioActualId: Long?
) {
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    var searchText by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf("Libros") }
    var isMenuExpanded by remember { mutableStateOf(false) }

    var userList by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var isSearchingUsers by remember { mutableStateOf(false) }
    var userSearchError by remember { mutableStateOf<String?>(null) }

    val searchState by viewModel.searchState

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
                userSearchError = "Error al buscar usuarios"
            }
            isSearchingUsers = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchMode == "Libros") "Buscar" else "Personas",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModeChip(
                        label = "Libros",
                        icon = Icons.Default.Book,
                        selected = searchMode == "Libros",
                        onClick = {
                            searchMode = "Libros"
                            searchText = ""
                            userList = emptyList()
                        }
                    )
                    ModeChip(
                        label = "Usuarios",
                        icon = Icons.Default.Person,
                        selected = searchMode == "Usuarios",
                        onClick = {
                            searchMode = "Usuarios"
                            searchText = ""
                            userList = emptyList()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                BasicTextField_Compat(
                    value = searchText,
                    onValueChange = { newText ->
                        searchText = newText
                        if (searchMode == "Libros") {
                            viewModel.onQueryChanged(newText)
                        } else if (newText.length >= 3) {
                            buscarUsuariosDB(newText)
                        } else {
                            userList = emptyList()
                        }
                    },
                    placeholder = "¿Qué ${if (searchMode == "Libros") "libro" else "usuario"} buscas?",
                    modifier = Modifier.weight(1f)
                )

                if (searchText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchText = ""
                            userList = emptyList()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            "✕",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (searchMode == "Libros") {
            if (searchText.isEmpty()) {
                val recomendadosState by viewModel.recommendadosState
                when (val state = recomendadosState) {
                    is BookUiState.Loading -> LoadingView()
                    is BookUiState.Error -> {}
                    is BookUiState.Success -> {
                        if (state.books.isNotEmpty()) {
                            RecomendadosSearchView(
                                books = state.books,
                                navController = navController
                            )
                        } else {
                            EmptySearchPrompt(searchMode)
                        }
                    }
                }
            } else {
                when (val currentState = searchState) {
                    is BookUiState.Loading -> LoadingView()
                    is BookUiState.Error -> ErrorView(currentState.message)
                    is BookUiState.Success -> BookList(currentState.books, viewModel, navController)
                }
            }
        } else {
            when {
                isSearchingUsers -> LoadingView()
                userSearchError != null -> ErrorView(userSearchError!!)
                userList.isEmpty() && searchText.isNotEmpty() -> ErrorView("No se encontraron usuarios")
                userList.isEmpty() -> EmptySearchPrompt(searchMode)
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = userList,
                            key = { usuario -> usuario.id ?: usuario.hashCode() }
                        ) { usuario ->
                            UserCard(
                                usuario = usuario,
                                navController = navController,
                                usuarioActualId = usuarioActualId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun BasicTextField_Compat(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            cursorBrush = Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
            )
        )
    }
}

@Composable
fun EmptySearchPrompt(mode: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (mode == "Libros") "📚" else "🔍",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (mode == "Libros") "Busca tu próxima lectura" else "Encuentra a tus amigos lectores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (mode == "Libros") "Escribe el título o el autor" else "Busca por nombre de usuario",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UserCard(
    usuario: Usuario,
    navController: NavHostController,
    usuarioActualId: Long?
) {
    val authRepository = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()

    var isSiguiendo by remember { mutableStateOf(false) }
    var cargandoSeguir by remember { mutableStateOf(false) }

    LaunchedEffect(usuarioActualId, usuario.id) {
        if (usuarioActualId != null && usuario.id != null) {
            authRepository.comprobarSiSigue(usuarioActualId, usuario.id).onSuccess {
                isSiguiendo = it
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                usuario.id?.let { navController.navigate(Screen.ExternalProfile(userId = it)) }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!usuario.fotoPerfil.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(usuario.fotoPerfil)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${usuario.nickname}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!usuario.descripcion.isNullOrEmpty()) {
                    Text(
                        text = usuario.descripcion,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (usuarioActualId != null && usuarioActualId != usuario.id) {
                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSiguiendo) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                        .border(
                            width = if (isSiguiendo) 1.dp else 0.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(enabled = !cargandoSeguir) {
                            val nuevoEstado = !isSiguiendo
                            isSiguiendo = nuevoEstado
                            cargandoSeguir = true
                            coroutineScope.launch {
                                val res = if (nuevoEstado)
                                    authRepository.seguirUsuario(usuarioActualId, usuario.id!!)
                                else
                                    authRepository.dejarDeSeguirUsuario(usuarioActualId, usuario.id!!)
                                if (res.isFailure) isSiguiendo = !nuevoEstado
                                cargandoSeguir = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (cargandoSeguir) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = if (isSiguiendo) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                        )
                    } else {
                        Text(
                            text = if (isSiguiendo) "Siguiendo" else "Seguir",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSiguiendo) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecomendadosSearchView(books: List<Book>, navController: NavHostController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header_recomendados") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Basado en tu biblioteca",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        items(
            items = books,
            key = { book -> book.key ?: book.hashCode() }
        ) { book ->
            RecomendadoBookCard(book = book, navController = navController)
        }
    }
}

@Composable
fun RecomendadoBookCard(book: Book, navController: NavHostController) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val keySegura = android.net.Uri.encode(book.key ?: "")
                if (keySegura.isNotEmpty()) {
                    navController.navigate(Screen.BookDetail(bookKey = keySegura))
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coverUrl = book.coverId?.let {
                "https://covers.openlibrary.org/b/id/${it}-M.jpg"
            }
            Box(
                modifier = Modifier
                    .size(52.dp, 78.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.librologo)
                    )
                } else {
                    Icon(Icons.Default.Book, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.authorNames?.firstOrNull() ?: "Autor desconocido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.firstPublishYear != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${book.firstPublishYear}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun BookList(
    books: List<Book>,
    viewModel: BookViewModel,
    navController: NavHostController
) {
    val isLastPage by viewModel.isLastPage
    val listState = rememberLazyListState()

    val deberiaCargarMas by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - 3 && totalItems > 0
        }
    }

    LaunchedEffect(deberiaCargarMas) {
        if (deberiaCargarMas && !isLastPage) {
            viewModel.searchBooks(query = "", isNextPage = true)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = books,
            key = { _, book -> book.key ?: book.hashCode() }
        ) { _, book ->
            BookCard(book, navController)
        }

        if (!isLastPage && books.isNotEmpty()) {
            item(key = "skeletons") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { SkeletonBookCard() }
                }
            }
        } else if (isLastPage && books.isNotEmpty()) {
            item(key = "fin_resultados") {
                Text(
                    text = "Has llegado al final de los resultados",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
            .clickable {
                val keySegura = Uri.encode(book.key ?: "")
                if (keySegura.isNotEmpty()) {
                    navController.navigate(Screen.BookDetail(bookKey = keySegura))
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coverUrl = book.coverId?.let {
                "https://covers.openlibrary.org/b/id/$it-M.jpg"
            }
            Box(
                modifier = Modifier
                    .size(60.dp, 90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.authorNames?.joinToString(", ") ?: "Autor desconocido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.firstPublishYear != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${book.firstPublishYear}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
    }
}

@Composable
fun ErrorView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(20.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SkeletonBookCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp, 90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(160.dp, 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )
                Box(
                    modifier = Modifier
                        .size(100.dp, 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )
                Box(
                    modifier = Modifier
                        .size(60.dp, 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha * 0.7f))
                )
            }
        }
    }
}
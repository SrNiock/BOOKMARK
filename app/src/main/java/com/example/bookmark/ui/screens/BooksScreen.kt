package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.bookmark.data.remote.BookUiState
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.data.remote.dto.Book
import com.example.bookmark.ui.navigation.Screen
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.supaBase.PublicacionFeed
import com.example.bookmark.ui.utils.SessionManager
import kotlin.math.absoluteValue

// Necesitamos pasarle el navController para que los clics del Feed funcionen
@Composable
fun BooksScreen(viewModel: BookViewModel, navController: NavHostController, onLogout: () -> Unit) {
    val uiState by viewModel.state

    // 👇 ESTADOS PARA EL FEED SOCIAL
    val authRepository = remember { AuthRepository() }
    var publicaciones by remember { mutableStateOf<List<PublicacionFeed>>(emptyList()) }
    var cargandoFeed by remember { mutableStateOf(true) }

    // Descargamos el feed al abrir la app
    LaunchedEffect(Unit) {
        authRepository.obtenerFeedPublicaciones().onSuccess {
            publicaciones = it
        }
        cargandoFeed = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Fondo negro del tema
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NoteReaderTopBar(onLogout = onLogout)

            when (uiState) {
                is BookUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is BookUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error al cargar libros", color = Color.Red) }
                is BookUiState.Success -> {
                    val books = (uiState as BookUiState.Success).books
                    // Le pasamos los libros, el feed y el navController a la función que dibuja todo
                    BookContent(books, publicaciones, cargandoFeed, navController)
                }
            }
        }
    }
}

@Composable
fun NoteReaderTopBar(onLogout: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }
    val correoActual = sessionManager.obtenerCorreoSesion() ?: ""

    var fotoPerfilUrl by remember { mutableStateOf<String?>(null) }
    var menuAbierto by remember { mutableStateOf(false) }

    LaunchedEffect(correoActual) {
        if (correoActual.isNotEmpty()) {
            authRepository.obtenerUsuario(correoActual).onSuccess { usuario ->
                fotoPerfilUrl = usuario.fotoPerfil
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "BookMark",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Box {
            Surface(
                onClick = { menuAbierto = true },
                color = Color.Transparent,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (!fotoPerfilUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = fotoPerfilUrl,
                            contentDescription = "Mi perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("U", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }

            DropdownMenu(
                expanded = menuAbierto,
                onDismissRequest = { menuAbierto = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.onBackground) },
                    onClick = {
                        menuAbierto = false
                        sessionManager.cerrarSesion()
                        onLogout()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red)
                    }
                )
            }
        }
    }
}

@Composable
fun BookContent(
    books: List<Book>,
    publicaciones: List<PublicacionFeed>,
    cargandoFeed: Boolean,
    navController: NavHostController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // --- PARTE 1: EL CARRUSEL ---
        item {
            SectionHeader(title = "For you")
            Spacer(modifier = Modifier.height(16.dp))
            BookCarouselRow(books = books) // Le pasamos todos los libros, sin hacer drop(1)
        }

        // --- PARTE 2: EL FEED SOCIAL ---
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(title = "Comunidad")
        }

        if (cargandoFeed) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (publicaciones.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Aún no hay reseñas. ¡Termina un libro para ser el primero!", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            // Pintamos las reseñas una debajo de otra
            items(publicaciones) { publicacion ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PublicacionCard(publicacion = publicacion, navController = navController)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCarouselRow(books: List<Book>) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemWidth = 140.dp
    val spacing = 16.dp

    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(horizontal = (itemWidth / 2) + spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier.fillMaxWidth().height(280.dp)
    ) {
        itemsIndexed(books) { index, book ->
            val scale by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val viewportCenter = layoutInfo.viewportEndOffset / 2f
                    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

                    if (itemInfo != null) {
                        val itemCenter = itemInfo.offset + itemInfo.size / 2f
                        val distanceFromCenter = (viewportCenter - itemCenter).absoluteValue
                        val normalizedDistance = (distanceFromCenter / (viewportCenter * 0.8f)).coerceIn(0f, 1f)

                        // Pequeño truco matemático para escalar
                        1f - (normalizedDistance * 0.15f)
                    } else {
                        0.85f
                    }
                }
            }

            val alphaScale = ((1f - scale) / 0.15f).coerceIn(0f, 1f)
            val alpha = 1f - (alphaScale * 0.4f)

            BookHorizontalItem(
                book = book,
                scale = scale,
                alpha = alpha,
                itemWidth = itemWidth
            )
        }
    }
}

@Composable
fun BookHorizontalItem(
    book: Book,
    scale: Float,
    alpha: Float,
    itemWidth: Dp
) {
    Column(
        modifier = Modifier
            .width(itemWidth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BookCover(
            coverId = book.coverId,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .shadow(
                    elevation = if (scale > 0.95f) 12.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false
                )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = book.authorNames?.firstOrNull() ?: "Unknown",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BookCover(coverId: Int?, modifier: Modifier) {
    val url = if (coverId != null) "https://covers.openlibrary.org/b/id/$coverId-L.jpg" else null

    AsyncImage(
        model = url,
        contentDescription = "Book Cover",
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        contentScale = ContentScale.Crop,
        error = painterResource(id = android.R.drawable.ic_menu_report_image)
    )
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
    }
}

// --- COMPONENTE DEL FEED SOCIAL ---
@Composable
fun PublicacionCard(publicacion: PublicacionFeed, navController: NavHostController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 1. CABECERA: Autor de la reseña
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(Screen.ExternalProfile(userId = publicacion.usuario_id))
                }
            ) {
                AsyncImage(
                    model = publicacion.usuario?.fotoPerfil,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.DarkGray),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = android.R.drawable.ic_menu_myplaces)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = "@${publicacion.usuario?.nickname ?: "Usuario Desconocido"}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "ha terminado de leer:", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. CONTENIDO: El Libro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable {
                        val rutaSegura = Uri.encode(publicacion.book_key)
                        navController.navigate(Screen.BookDetail(bookKey = rutaSegura))
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = "https://covers.openlibrary.org/b/id/${publicacion.cover_id}-M.jpg",
                    contentDescription = "Portada",
                    modifier = Modifier.size(50.dp, 75.dp).clip(RoundedCornerShape(4.dp)).background(Color.DarkGray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = publicacion.titulo_libro, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= publicacion.calificacion) Color(0xFFFFD700) else Color.DarkGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. LA RESEÑA
            if (publicacion.texto.isNotEmpty()) {
                Text(
                    text = "\"${publicacion.texto}\"",
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
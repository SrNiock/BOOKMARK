package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bookmark.data.remote.BookUiState
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.data.remote.dto.Book
import com.example.bookmark.ui.navigation.Screen
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.supaBase.Comentario
import com.example.bookmark.ui.supaBase.ComentarioFeed
import com.example.bookmark.ui.supaBase.PublicacionFeed
import com.example.bookmark.ui.utils.SessionManager
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// ─────────────────────────────────────────────
// PANTALLA PRINCIPAL
// ─────────────────────────────────────────────
@Composable
fun BooksScreen(viewModel: BookViewModel, navController: NavHostController, onLogout: () -> Unit) {
    val recomendadosState by viewModel.recommendadosState
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }

    var publicaciones by remember { mutableStateOf<List<PublicacionFeed>>(emptyList()) }
    var cargandoFeed by remember { mutableStateOf(true) }
    var usuarioActualId by remember { mutableStateOf<Long?>(null) }
    var idsSeguidos by remember { mutableStateOf<List<Long>>(emptyList()) }

    // EFECTO 1: Carga inicial — feed + identidad del usuario
    LaunchedEffect(Unit) {
        authRepository.obtenerFeedPublicaciones().onSuccess {
            publicaciones = it
            cargandoFeed = false
        }

        val correo = sessionManager.obtenerCorreoSesion() ?: ""
        if (correo.isNotEmpty()) {
            authRepository.obtenerUsuario(correo).onSuccess { usuario ->
                usuarioActualId = usuario.id
                usuario.id?.let { id ->
                    authRepository.obtenerIdsSeguidos(id).onSuccess { idsSeguidos = it }
                } ?: viewModel.obtenerLibrosDefault() // Prevención
            }.onFailure {
                // 👇 CLAVE: Si la base de datos tarda o falla, carga los recomendados por defecto
                viewModel.obtenerLibrosDefault()
            }
        } else {
            viewModel.obtenerLibrosDefault()
        }
    }

    // EFECTO 2: Solo se dispara cuando se resuelve el ID del usuario.
    // El ViewModel ignora llamadas repetidas gracias a ultimoUsuarioCargado.
    LaunchedEffect(usuarioActualId) {
        usuarioActualId?.let { viewModel.cargarRecomendaciones(it) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            NoteReaderTopBar(onLogout = onLogout)

            BookContent(
                uiState = recomendadosState,
                publicaciones = publicaciones,
                cargandoFeed = cargandoFeed,
                navController = navController,
                idsSeguidos = idsSeguidos,
                usuarioActualId = usuarioActualId
            )
        }
    }
}

// ─────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "BookMark",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(onClick = { /* TODO: Abrir pantalla de notificaciones */ }) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = { menuAbierto = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!fotoPerfilUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = fotoPerfilUrl,
                            contentDescription = "Mi perfil",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = menuAbierto,
                    onDismissRequest = { menuAbierto = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Cerrar Sesión",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        onClick = {
                            menuAbierto = false
                            sessionManager.cerrarSesion()
                            onLogout()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// CONTENIDO PRINCIPAL (TABS + FEED)
// ─────────────────────────────────────────────
@Composable
fun BookContent(
    uiState: BookUiState,
    publicaciones: List<PublicacionFeed>,
    cargandoFeed: Boolean,
    navController: NavHostController,
    idsSeguidos: List<Long>,
    usuarioActualId: Long?
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Explorar", "Siguiendo")
    val carouselListState = rememberLazyListState()

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val publicacionesFiltradas = if (selectedTabIndex == 0) {
        publicaciones
    } else {
        publicaciones.filter { it.usuario_id in idsSeguidos }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // SECCIÓN 1: CARRUSEL DE RECOMENDACIONES
        item(key = "seccion_descubrir") {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = if (usuarioActualId != null) "Recomendados para ti" else "Descubrir")
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier
                .fillMaxWidth()
                .height(225.dp)) {
                when (uiState) {
                    is BookUiState.Loading -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(5) {
                                BookSkeletonItem(alpha = alpha)
                            }
                        }
                    }

                    is BookUiState.Success -> {
                        if (uiState.books.isNotEmpty()) {
                            BookCarouselRow(
                                books = uiState.books,
                                navController = navController,
                                listState = carouselListState
                            )
                        } else {
                            // 👇 ESTO EVITA EL HUECO EN BLANCO SI TODO FALLA
                            Box(
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Vuelve a intentarlo deslizando hacia abajo", color = Color.Gray)
                            }
                        }
                    }

                    is BookUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No pudimos cargar recomendaciones",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // SECCIÓN 2: TABS
        item(key = "tabs_feed") {
            Spacer(modifier = Modifier.height(24.dp))
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.onBackground else Color.Gray
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // SECCIÓN 3: FEED SOCIAL
        if (cargandoFeed) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else if (publicacionesFiltradas.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedTabIndex == 0) "El feed está muy tranquilo hoy..."
                        else "No hay actividad de tus seguidos.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(
                items = publicacionesFiltradas,
                key = { pub -> pub.id ?: pub.hashCode() }
            ) { publicacion ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PublicacionCard(
                        publicacion = publicacion,
                        navController = navController,
                        usuarioActualId = usuarioActualId
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// SKELETON (SHIMMER)
// ─────────────────────────────────────────────
@Composable
fun BookSkeletonItem(alpha: Float) {
    Column(
        modifier = Modifier.width(110.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(18.dp)
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(14.dp)
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )
    }
}

// ─────────────────────────────────────────────
// CARRUSEL DE LIBROS
// ─────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCarouselRow(
    books: List<Book>,
    navController: NavHostController,
    listState: LazyListState
) {
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemWidth = 110.dp
    val spacing = 12.dp

    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(horizontal = (itemWidth / 2) + spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        itemsIndexed(
            items = books,
            key = { index, book -> book.key ?: index }
        ) { index, book ->

            val scale by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val viewportEnd = layoutInfo.viewportEndOffset

                    // 👇 BLOQUEO ANTI-CRASH: Si el ancho es 0, devolvemos un tamaño por defecto
                    if (viewportEnd <= 0) return@derivedStateOf 0.85f

                    val viewportCenter = viewportEnd / 2f
                    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

                    if (itemInfo != null) {
                        val itemCenter = itemInfo.offset + itemInfo.size / 2f
                        val distanceFromCenter = (viewportCenter - itemCenter).absoluteValue
                        val normalizedDistance =
                            (distanceFromCenter / (viewportCenter * 0.8f)).coerceIn(0f, 1f)
                        1f - (normalizedDistance * 0.15f)
                    } else {
                        0.85f
                    }
                }
            }

            val alphaScale = ((1f - scale) / 0.15f).coerceIn(0f, 1f)
            val textAlpha = 1f - (alphaScale * 0.8f)

            BookHorizontalItem(
                book = book,
                scale = scale,
                textAlpha = textAlpha,
                itemWidth = itemWidth,
                onClick = {
                    val keySegura = Uri.encode(book.key ?: "")
                    if (keySegura.isNotEmpty()) {
                        navController.navigate(Screen.BookDetail(bookKey = keySegura))
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────
// ITEM DEL CARRUSEL
// ─────────────────────────────────────────────
@Composable
fun BookHorizontalItem(
    book: Book,
    scale: Float,
    textAlpha: Float,
    itemWidth: Dp,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 150),
        label = "escala_libro"
    )

    Column(
        modifier = Modifier
            .width(itemWidth)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CORRECCIÓN: shadow ANTES de clip para que sea visible
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .shadow(
                    elevation = if (animatedScale > 0.95f) 16.dp else 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    RoundedCornerShape(12.dp)
                ),
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            BookCover(
                coverId = book.coverId,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = book.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = textAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = book.authorNames?.firstOrNull() ?: "Desconocido",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = textAlpha * 0.7f),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────
// PORTADA DEL LIBRO
// ─────────────────────────────────────────────
@Composable
fun BookCover(coverId: Int?, modifier: Modifier) {
    val url = if (coverId != null) "https://covers.openlibrary.org/b/id/$coverId-L.jpg" else null
    val context = LocalContext.current

    val imageRequest = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(300)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = "Book Cover",
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop,
        error = painterResource(id = android.R.drawable.ic_menu_report_image)
    )
}

// ─────────────────────────────────────────────
// CABECERA DE SECCIÓN
// ─────────────────────────────────────────────
@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────
// TARJETA DE PUBLICACIÓN
// ─────────────────────────────────────────────
@Composable
fun PublicacionCard(
    publicacion: PublicacionFeed,
    navController: NavHostController,
    usuarioActualId: Long?
) {
    val authRepository = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Estados de Like
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(0L) }

    // CORRECCIÓN: Estado de Bookmark con persistencia real
    var isSaved by remember { mutableStateOf(false) }

    // Estados de Comentarios
    var comentariosExpandidos by remember { mutableStateOf(false) }
    var listaComentarios by remember { mutableStateOf<List<ComentarioFeed>>(emptyList()) }
    var cargandoComentarios by remember { mutableStateOf(false) }
    var textoNuevoComentario by remember { mutableStateOf("") }

    // CORRECCIÓN: Solo cargamos likes y estado inicial de bookmark.
    // Los comentarios se cargan al abrir la sección para no desperdiciar llamadas.
    LaunchedEffect(publicacion.id, usuarioActualId) {
        if (publicacion.id != null) {
            authRepository.contarLikes(publicacion.id).onSuccess { likesCount = it }

            if (usuarioActualId != null) {
                authRepository.comprobarSiDioLike(usuarioActualId, publicacion.id)
                    .onSuccess { isLiked = it }

                // Cargamos si ya guardó esta publicación
                authRepository.comprobarSiGuardado(usuarioActualId, publicacion.id)
                    .onSuccess { isSaved = it }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // --- 1. CABECERA: Autor de la reseña ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate(
                            Screen.ExternalProfile(userId = publicacion.usuario_id)
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!publicacion.usuario?.fotoPerfil.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(publicacion.usuario?.fotoPerfil)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "@${publicacion.usuario?.nickname ?: "Usuario"}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Terminó de leer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. CONTENIDO: El libro ---
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val rutaSegura = Uri.encode(publicacion.book_key)
                        navController.navigate(Screen.BookDetail(bookKey = rutaSegura))
                    }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("https://covers.openlibrary.org/b/id/${publicacion.cover_id}-M.jpg")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Portada",
                        modifier = Modifier
                            .size(48.dp, 72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = publicacion.titulo_libro,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (i <= publicacion.calificacion) Color(0xFFFFC107)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. RESEÑA (Estilo cita) ---
            if (publicacion.texto.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = publicacion.texto,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. BOTONES DE INTERACCIÓN ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // LIKE (optimista con rollback)
                    IconButton(onClick = {
                        if (usuarioActualId != null && publicacion.id != null) {
                            isLiked = !isLiked
                            likesCount += if (isLiked) 1 else -1
                            coroutineScope.launch {
                                val res = if (isLiked)
                                    authRepository.darLike(usuarioActualId, publicacion.id)
                                else
                                    authRepository.quitarLike(usuarioActualId, publicacion.id)

                                if (res.isFailure) {
                                    isLiked = !isLiked
                                    likesCount += if (isLiked) 1 else -1
                                }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Me gusta",
                            tint = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = likesCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // COMENTAR — carga comentarios solo al abrir
                    IconButton(onClick = {
                        comentariosExpandidos = !comentariosExpandidos
                        // CORRECCIÓN: solo cargamos cuando se expande y la lista está vacía
                        if (comentariosExpandidos && listaComentarios.isEmpty() && publicacion.id != null) {
                            coroutineScope.launch {
                                cargandoComentarios = true
                                authRepository.obtenerComentarios(publicacion.id)
                                    .onSuccess { listaComentarios = it }
                                cargandoComentarios = false
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comentar",
                            tint = if (comentariosExpandidos) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = listaComentarios.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // CORRECCIÓN: Bookmark con persistencia real (optimista con rollback)
                IconButton(onClick = {
                    if (usuarioActualId != null && publicacion.id != null) {
                        isSaved = !isSaved
                        coroutineScope.launch {
                            val res = if (isSaved)
                                authRepository.guardarPublicacion(usuarioActualId, publicacion.id)
                            else
                                authRepository.eliminarPublicacionGuardada(usuarioActualId, publicacion.id)

                            if (res.isFailure) {
                                isSaved = !isSaved // Rollback si falla
                            }
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Guardar",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- 5. SECCIÓN DE COMENTARIOS ---
            AnimatedVisibility(
                visible = comentariosExpandidos,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // A) CAMPO PARA NUEVO COMENTARIO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textoNuevoComentario,
                            onValueChange = { textoNuevoComentario = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text("Añadir un comentario...", fontSize = 14.sp)
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.5f
                                ),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.3f
                                ),
                                focusedContainerColor = Color.Transparent
                            ),
                            maxLines = 3
                        )

                        IconButton(
                            onClick = {
                                if (textoNuevoComentario.isNotBlank()
                                    && usuarioActualId != null
                                    && publicacion.id != null
                                ) {
                                    val textoAEnviar = textoNuevoComentario
                                    textoNuevoComentario = ""

                                    coroutineScope.launch {
                                        val nuevoComentario = Comentario(
                                            usuario_id = usuarioActualId,
                                            publicacion_id = publicacion.id,
                                            texto = textoAEnviar
                                        )
                                        authRepository.agregarComentario(nuevoComentario)
                                            .onSuccess {
                                                authRepository.obtenerComentarios(publicacion.id)
                                                    .onSuccess { listaComentarios = it }
                                            }
                                    }
                                }
                            },
                            enabled = textoNuevoComentario.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Enviar",
                                tint = if (textoNuevoComentario.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // B) LISTA DE COMENTARIOS
                    if (cargandoComentarios) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (listaComentarios.isEmpty()) {
                        Text(
                            "Sé el primero en comentar.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        listaComentarios.forEach { comentario ->
                            Row(
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .fillMaxWidth()
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(comentario.usuario?.fotoPerfil)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = android.R.drawable.ic_menu_report_image)
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "@${comentario.usuario?.nickname ?: "Usuario"}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = comentario.texto,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
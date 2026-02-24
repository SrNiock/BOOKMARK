package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowForward
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

// Necesitamos pasarle el navController para que los clics del Feed funcionen
// En tu BooksScreen:
@Composable
fun BooksScreen(viewModel: BookViewModel, navController: NavHostController, onLogout: () -> Unit) {
    val uiState by viewModel.state
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }

    var publicaciones by remember { mutableStateOf<List<PublicacionFeed>>(emptyList()) }
    var cargandoFeed by remember { mutableStateOf(true) }

    // 👇 NUEVOS ESTADOS PARA EL USUARIO ACTUAL
    var usuarioActualId by remember { mutableStateOf<Long?>(null) }
    var idsSeguidos by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(Unit) {
        // 1. Cargamos el feed completo
        authRepository.obtenerFeedPublicaciones().onSuccess { publicaciones = it }
        cargandoFeed = false

        // 2. Cargamos los datos del usuario logueado y a quién sigue
        val correo = sessionManager.obtenerCorreoSesion() ?: ""
        if (correo.isNotEmpty()) {
            authRepository.obtenerUsuario(correo).onSuccess { usuario ->
                usuarioActualId = usuario.id
                if (usuario.id != null) {
                    authRepository.obtenerIdsSeguidos(usuario.id).onSuccess { seguidos ->
                        idsSeguidos = seguidos
                    }
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            NoteReaderTopBar(onLogout = onLogout)

            when (uiState) {
                is BookUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is BookUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error al cargar", color = Color.Red) }
                is BookUiState.Success -> {
                    val books = (uiState as BookUiState.Success).books
                    BookContent(
                        books = books,
                        publicaciones = publicaciones,
                        cargandoFeed = cargandoFeed,
                        navController = navController,
                        idsSeguidos = idsSeguidos,           // La lista de amigos que calculamos en el LaunchedEffect
                        usuarioActualId = usuarioActualId    // El ID de la sesión actual
                    )
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
            .padding(horizontal = 16.dp, vertical = 12.dp), // Padding un poco más ajustado
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Text(
            text = "BookMark",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary // Logo con el color principal de la app
            )
        )

        // Botones de la derecha (Notificaciones + Perfil)
        Row(verticalAlignment = Alignment.CenterVertically) {

            // 🔔 Icono de Notificaciones
            IconButton(onClick = { /* TODO: Abrir pantalla de notificaciones */ }) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 👤 Perfil con Dropdown (como lo mejoramos antes)
            Box(contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = { menuAbierto = true },
                    modifier = Modifier
                        .size(40.dp) // Un pelín más pequeño para que encaje mejor con la campana
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!fotoPerfilUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = fotoPerfilUrl,
                            contentDescription = "Mi perfil",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
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
}

@Composable
fun BookContent(
    books: List<Book>,
    publicaciones: List<PublicacionFeed>,
    cargandoFeed: Boolean,
    navController: NavHostController,
    idsSeguidos: List<Long>,
    usuarioActualId: Long?
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Explorar", "Siguiendo")

    // 👇 1. CREAMOS EL ESTADO DEL CARRUSEL AQUÍ ARRIBA
    val carouselListState = rememberLazyListState()

    val publicacionesFiltradas = if (selectedTabIndex == 0) publicaciones else publicaciones.filter { it.usuario_id in idsSeguidos }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 👇 2. LE PONEMOS UN "key" AL ITEM
        item(key = "seccion_descubrir") {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "Descubrir")
            Spacer(modifier = Modifier.height(12.dp))

            BookCarouselRow(
                books = books,
                navController = navController,
                listState = carouselListState // 👇 3. LE PASAMOS EL ESTADO AL CARRUSEL
            )
        }

        // --- PARTE 2: SELECTOR DE FEED (TABS) ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}, // Sin línea de fondo molesta
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- PARTE 3: EL FEED ---
        if (cargandoFeed) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (publicacionesFiltradas.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedTabIndex == 0)
                            "Aún no hay reseñas. ¡Termina un libro para ser el primero!"
                        else
                            "Aún no sigues a nadie o tus amigos no han publicado nada.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Renderizamos la lista filtrada
            items(
                items = publicacionesFiltradas,
                // 👇 AQUÍ ESTÁ EL ARREGLO 1: Identificador único por publicación
                key = { publicacion -> publicacion.id ?: publicacion.hashCode() }
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCarouselRow(
    books: List<Book>,
    navController: NavHostController,
    listState: LazyListState // 👇 RECIBIMOS EL ESTADO AQUÍ
) {
    // ELIMINAMOS esta línea porque ya recibimos listState desde arriba:
    // val listState = rememberLazyListState()

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
        // 👇 SOLUCIÓN: Usamos book.key (y si por algún motivo es nulo, usamos el index como salvavidas)
        itemsIndexed(
            items = books,
            key = { index, book -> book.key ?: index }
        ) { index, book ->

            // ... (Todo el cálculo de scale y textAlpha sigue igual aquí dentro) ...
            // Tu excelente cálculo matemático (lo mantenemos intacto)
            val scale by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val viewportCenter = layoutInfo.viewportEndOffset / 2f
                    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

                    if (itemInfo != null) {
                        val itemCenter = itemInfo.offset + itemInfo.size / 2f
                        val distanceFromCenter = (viewportCenter - itemCenter).absoluteValue
                        val normalizedDistance = (distanceFromCenter / (viewportCenter * 0.8f)).coerceIn(0f, 1f)
                        1f - (normalizedDistance * 0.15f)
                    } else {
                        0.85f
                    }
                }
            }

            // Calculamos el alpha para los textos (se desvanecen a los lados)
            val alphaScale = ((1f - scale) / 0.15f).coerceIn(0f, 1f)
            val textAlpha = 1f - (alphaScale * 0.8f) // Fade más agresivo para resaltar el centro

            BookHorizontalItem(
                book = book,
                scale = scale,
                textAlpha = textAlpha,
                itemWidth = itemWidth,
                onClick = {
                    // FUNCIONALIDAD: Navegamos al detalle del libro al hacer clic
                    // Asumo que tu objeto Book tiene una propiedad 'key' u 'id'
                    val keySegura = Uri.encode(book.key ?: "")
                    if (keySegura.isNotEmpty()) {
                        navController.navigate(Screen.BookDetail(bookKey = keySegura))
                    }
                }
            )
        }
    }
}

@Composable
fun BookHorizontalItem(
    book: Book,
    scale: Float,
    textAlpha: Float,
    itemWidth: Dp,
    onClick: () -> Unit // Recibimos la acción de clic
) {
    // Animamos la escala para que el movimiento de arrastre sea extra suave
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
        // Envolvemos la portada en un Surface para manejar mejor los clics y la estética
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f) // PROPORCIÓN PERFECTA DE LIBRO
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    RoundedCornerShape(12.dp)
                )
                .shadow(
                    elevation = if (animatedScale > 0.95f) 16.dp else 4.dp, // Sombra dinámica
                    shape = RoundedCornerShape(12.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) // Sombra con tono de tu tema
                ),
            onClick = onClick, // ¡Hacemos que todo el libro sea clickeable!
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            BookCover(
                coverId = book.coverId,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TEXTOS DINÁMICOS (Se ocultan si no es el libro central)
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

@Composable
fun BookCover(coverId: Int?, modifier: Modifier) {
    val url = if (coverId != null) "https://covers.openlibrary.org/b/id/$coverId-L.jpg" else null
    val context = LocalContext.current

    // 👇 ARREGLO 2: Recordar la petición de la imagen
    val imageRequest = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(300) // Lo bajamos a 300ms para que sea más ágil en el scroll
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
fun PublicacionCard(
    publicacion: PublicacionFeed,
    navController: NavHostController,
    usuarioActualId: Long?
) {
    val authRepository = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Estados de Like y Bookmark
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(0L) }
    var isSaved by remember { mutableStateOf(false) }

    // 👇 NUEVOS ESTADOS PARA COMENTARIOS
    var comentariosExpandidos by remember { mutableStateOf(false) }
    var listaComentarios by remember { mutableStateOf<List<ComentarioFeed>>(emptyList()) }
    var cargandoComentarios by remember { mutableStateOf(false) }
    var textoNuevoComentario by remember { mutableStateOf("") }

    // Cargar likes al iniciar
    LaunchedEffect(publicacion.id, usuarioActualId) {
        if (publicacion.id != null) {
            authRepository.contarLikes(publicacion.id).onSuccess { likesCount = it }
            if (usuarioActualId != null) {
                authRepository.comprobarSiDioLike(usuarioActualId, publicacion.id).onSuccess { isLiked = it }
            }
            // Cargamos de fondo la cantidad de comentarios inicial (opcional, para el contador)
            authRepository.obtenerComentarios(publicacion.id).onSuccess { listaComentarios = it }
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
                        navController.navigate(Screen.ExternalProfile(userId = publicacion.usuario_id))
                    }
            ) {
                // Foto de perfil con Crossfade
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

            // --- 2. CONTENIDO: El Libro (Estilo Chip expandido) ---
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
                    // Portada del libro con Crossfade
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

                        // Estrellas
                        Row {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (i <= publicacion.calificacion) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. LA RESEÑA (Estilo Cita) ---
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

                    // BOTÓN: LIKE (El que ya teníamos)
                    IconButton(onClick = {
                        if (usuarioActualId != null && publicacion.id != null) {
                            isLiked = !isLiked
                            likesCount += if (isLiked) 1 else -1
                            coroutineScope.launch {
                                val res = if (isLiked) authRepository.darLike(usuarioActualId, publicacion.id)
                                else authRepository.quitarLike(usuarioActualId, publicacion.id)
                                if (res.isFailure) { isLiked = !isLiked; likesCount += if (isLiked) 1 else -1 }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Me gusta",
                            tint = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(text = likesCount.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.width(16.dp))

                    // 👇 BOTÓN: COMENTAR (Ahora expande la sección)
                    IconButton(onClick = {
                        comentariosExpandidos = !comentariosExpandidos
                        // Si abrimos y no hay comentarios cargados, los pedimos
                        if (comentariosExpandidos && publicacion.id != null) {
                            coroutineScope.launch {
                                cargandoComentarios = true
                                authRepository.obtenerComentarios(publicacion.id).onSuccess { listaComentarios = it }
                                cargandoComentarios = false
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comentar",
                            tint = if (comentariosExpandidos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Mostramos la cantidad real de comentarios
                    Text(text = listaComentarios.size.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // BOTÓN: GUARDAR
                IconButton(onClick = { isSaved = !isSaved }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Guardar",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 👇 --- 5. SECCIÓN DE COMENTARIOS (ESTILO REDDIT) ---
            AnimatedVisibility(
                visible = comentariosExpandidos,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {

                    // Línea separadora sutil
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // A) CAJA PARA ESCRIBIR NUEVO COMENTARIO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textoNuevoComentario,
                            onValueChange = { textoNuevoComentario = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Añadir un comentario...", fontSize = 14.sp) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedContainerColor = Color.Transparent
                            ),
                            maxLines = 3 // Permite crecer un poco si el comentario es largo
                        )

                        IconButton(
                            onClick = {
                                if (textoNuevoComentario.isNotBlank() && usuarioActualId != null && publicacion.id != null) {
                                    val textoAEnviar = textoNuevoComentario
                                    textoNuevoComentario = "" // Limpiamos la caja al instante (UX fluida)

                                    coroutineScope.launch {
                                        val nuevoComentario = Comentario(
                                            usuario_id = usuarioActualId,
                                            publicacion_id = publicacion.id,
                                            texto = textoAEnviar
                                        )
                                        authRepository.agregarComentario(nuevoComentario).onSuccess {
                                            // Recargamos los comentarios para ver el nuestro en la lista
                                            authRepository.obtenerComentarios(publicacion.id).onSuccess { listaComentarios = it }
                                        }
                                    }
                                }
                            },
                            enabled = textoNuevoComentario.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Enviar",
                                tint = if (textoNuevoComentario.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // B) LISTA DE COMENTARIOS DE LA GENTE
                    if (cargandoComentarios) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp), strokeWidth = 2.dp)
                    } else if (listaComentarios.isEmpty()) {
                        Text("Sé el primero en comentar.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    } else {
                        // Usamos un simple Column con forEach porque estamos dentro de un LazyColumn padre
                        listaComentarios.forEach { comentario ->
                            Row(modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()) {
                                // Foto pequeñita del que comenta
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(comentario.usuario?.fotoPerfil).crossfade(true).build(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = android.R.drawable.ic_menu_report_image)
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                // Nombre y texto
                                Column {
                                    Text(
                                        text = "@${comentario.usuario?.nickname ?: "Usuario"}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
            } // Fin de AnimatedVisibility
        }
    }
}


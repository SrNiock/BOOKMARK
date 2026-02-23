package com.example.bookmark.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.fontscaling.MathUtils.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bookmark.data.remote.BookUiState
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.data.remote.dto.Book
import kotlin.math.absoluteValue
import androidx.compose.ui.util.lerp

@Composable
// 1. Añadimos el parámetro onLogout aquí para que la pantalla sepa que esto puede pasar
fun BooksScreen(viewModel: BookViewModel, onLogout: () -> Unit) {
    val uiState by viewModel.state

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F0F0F)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 2. ¡AQUÍ ESTÁ LO QUE FALTA! Le pasamos el onLogout a la barra superior
            NoteReaderTopBar(onLogout = onLogout)

            when (uiState) {
                is BookUiState.Loading -> LoadingView()
                is BookUiState.Error -> ErrorView((uiState as BookUiState.Error).message)
                is BookUiState.Success -> {
                    val books = (uiState as BookUiState.Success).books
                    BookContent(books)
                }
            }
        }
    }
}

@Composable
fun NoteReaderTopBar(onLogout: () -> Unit) { // Añadimos este parámetro para avisar al NavGraph
    // 1. Herramientas
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.example.bookmark.ui.utils.SessionManager(context) }
    val authRepository = remember { com.example.bookmark.ui.supaBase.AuthRepository() }
    val correoActual = sessionManager.obtenerCorreoSesion() ?: ""

    var fotoPerfilUrl by remember { mutableStateOf<String?>(null) }

    // 🔥 Estado para controlar si el menú está abierto
    var menuAbierto by remember { mutableStateOf(false) }

    // 2. Cargamos la foto
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
                color = Color.White
            )
        )

        // Contenedor del Perfil + Menú
        Box {
            // La foto de perfil ahora es un botón que abre el menú
            Surface(
                onClick = { menuAbierto = true },
                color = Color.Transparent,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray),
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
                        Text("U", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 🔥 El Dropdown Menu
            DropdownMenu(
                expanded = menuAbierto,
                onDismissRequest = { menuAbierto = false }, // Se cierra si tocas fuera
                modifier = Modifier.background(Color(0xFF1E1E1E)) // Color oscuro a juego
            ) {
                DropdownMenuItem(
                    text = { Text("Cerrar Sesión", color = Color.White) },
                    onClick = {
                        menuAbierto = false
                        sessionManager.cerrarSesion() // 1. Borra el correo del móvil
                        onLogout() // 2. Avisa para navegar al Login
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = Color.Red
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun BookContent(books: List<Book>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // SECCIÓN: "Continue Reading" (Se mantiene igual)
        if (books.isNotEmpty()) {
            item {
                ContinueReadingSection(books.first())
            }
        }

        // SECCIÓN: "For you" con EFECTO CARRUSEL CENTRADO
        item {
            SectionHeader(title = "For you")
            Spacer(modifier = Modifier.height(16.dp))
            // Pasamos la lista sin el primer libro (ya está arriba)
            BookCarouselRow(books = books.drop(1))
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCarouselRow(books: List<Book>) {
    // 1. Estado para controlar la posición del scroll
    val listState = rememberLazyListState()
    // 2. Comportamiento de "Snap" para que se detenga en el centro de un elemento
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Ancho base de cada ítem y espaciado
    val itemWidth = 140.dp
    val spacing = 16.dp

    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        // Añadimos padding horizontal grande para que el primer y último ítem puedan quedar centrados
        contentPadding = PaddingValues(horizontal = (itemWidth / 2) + spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier.fillMaxWidth().height(280.dp) // Altura fija para el contenedor
    ) {
        itemsIndexed(books) { index, book ->
            // 3. Cálculo de la escala y opacidad basado en la posición
            // ... dentro de itemsIndexed en el LazyRow ...
            val scale by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val viewportCenter = layoutInfo.viewportEndOffset / 2f
                    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

                    if (itemInfo != null) {
                        val itemCenter = itemInfo.offset + itemInfo.size / 2f
                        val distanceFromCenter = (viewportCenter - itemCenter).absoluteValue

                        // Factor de normalización (0.0 en el centro, 1.0 en los bordes)
                        val normalizedDistance = (distanceFromCenter / (viewportCenter * 0.8f)).coerceIn(0f, 1f)

                        // SOLUCIÓN: Usamos argumentos nombrados para eliminar la ambigüedad
                        // Si usas el import de compose ui util, el parámetro es 'fraction'
                        androidx.compose.ui.util.lerp(
                            start = 1f,
                            stop = 0.85f,
                            fraction = normalizedDistance
                        )
                    } else {
                        0.85f
                    }
                }
            }

// Aplicamos lo mismo para el Alpha
            val alphaScale = ((1f - scale) / 0.15f).coerceIn(0f, 1f)
            val alpha = androidx.compose.ui.util.lerp(
                start = 1f,
                stop = 0.6f,
                fraction = alphaScale
            )

            // 4. Pasamos la escala calculada al componente del libro
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
fun ContinueReadingSection(book: Book) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Portada del libro
            BookCover(
                coverId = book.coverId,
                modifier = Modifier
                    .size(width = 100.dp, height = 150.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2
                )
                Text(
                    text = "by ${book.authorNames?.firstOrNull() ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Barra de progreso ficticia como en la imagen
                LinearProgressIndicator(
                    progress = 0.78f,
                    modifier = Modifier.fillMaxWidth().clip(CircleShape),
                    color = Color(0xFFC2415E), // Color rojizo/rosa de la imagen
                    trackColor = Color.DarkGray
                )

                Text(
                    text = "78% • 20 min left",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2415E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue reading")
                }
            }
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
            // APLICAMOS LA TRANSFORMACIÓN GRÁFICA
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                // El pivote por defecto es el centro, que es lo que queremos,
                // para que crezca desde el medio.
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BookCover(
            coverId = book.coverId,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                // Sombra suave para el elemento destacado
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
            color = Color.White,
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
        modifier = modifier.background(Color.DarkGray),
        contentScale = ContentScale.Crop,
        error = painterResource(id = android.R.drawable.ic_menu_report_image) // Placeholder si falla
    )
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
    }
}

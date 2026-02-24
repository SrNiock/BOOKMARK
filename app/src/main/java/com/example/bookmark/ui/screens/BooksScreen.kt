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
fun BooksScreen(viewModel: BookViewModel, onLogout: () -> Unit) {
    val uiState by viewModel.state

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Fondo negro del tema
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NoteReaderTopBar(onLogout = onLogout)

            when (uiState) {
                is BookUiState.Loading -> LoadingView() // Asegúrate de tener estas funciones o importarlas
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
fun NoteReaderTopBar(onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.example.bookmark.ui.utils.SessionManager(context) }
    val authRepository = remember { com.example.bookmark.ui.supaBase.AuthRepository() }
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
                color = MaterialTheme.colorScheme.onBackground // Blanco del tema
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
                modifier = Modifier.background(MaterialTheme.colorScheme.surface) // Gris oscuro
            ) {
                DropdownMenuItem(
                    text = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.onBackground) },
                    onClick = {
                        menuAbierto = false
                        sessionManager.cerrarSesion()
                        onLogout()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = Color.Red // Lo mantenemos rojo para indicar peligro/salida
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
        if (books.isNotEmpty()) {
            item {
                ContinueReadingSection(books.first())
            }
        }

        item {
            SectionHeader(title = "For you")
            Spacer(modifier = Modifier.height(16.dp))
            BookCarouselRow(books = books.drop(1))
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

                        lerp(start = 1f, stop = 0.85f, fraction = normalizedDistance)
                    } else {
                        0.85f
                    }
                }
            }

            val alphaScale = ((1f - scale) / 0.15f).coerceIn(0f, 1f)
            val alpha = lerp(start = 1f, stop = 0.6f, fraction = alphaScale)

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Gris oscuro
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2
                )
                Text(
                    text = "by ${book.authorNames?.firstOrNull() ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { 0.78f },
                    modifier = Modifier.fillMaxWidth().clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary, // BARRA NARANJA
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, // BOTÓN NARANJA
                        contentColor = MaterialTheme.colorScheme.onPrimary  // TEXTO NEGRO
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue reading", fontWeight = FontWeight.Bold)
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
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
    }
}
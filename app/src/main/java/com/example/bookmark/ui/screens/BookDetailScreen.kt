package com.example.bookmark.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bookmark.data.remote.BookUiState
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.data.remote.dto.Book
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.supaBase.MiLibro
import com.example.bookmark.ui.utils.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookKey: String,
    viewModel: BookViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val sessionManager = remember { SessionManager(context) }
    val idActual = sessionManager.obtenerIdSesion() ?: 0L

    var menuAbierto by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var yaGuardado by remember { mutableStateOf(false) }
    var estadoGuardado by remember { mutableStateOf("") }

    // Progreso de lectura
    var libroEnBiblioteca by remember { mutableStateOf<MiLibro?>(null) }
    var sliderPagina by remember { mutableStateOf(0f) }
    var guardandoProgreso by remember { mutableStateOf(false) }
    var progresoGuardado by remember { mutableStateOf(false) }
    var cargandoDesdeBD by remember { mutableStateOf(true) }
    val recomendadosState by viewModel.recommendadosState
    val searchState by viewModel.searchState

    var snapshotBook by remember { mutableStateOf<Book?>(null) }
    if (snapshotBook == null) {
        val enRecomendados = (recomendadosState as? BookUiState.Success)
            ?.books?.find { it.key == bookKey }
        val enBusqueda = (searchState as? BookUiState.Success)
            ?.books?.find { it.key == bookKey }
        snapshotBook = enRecomendados ?: enBusqueda
    }
    val paginasReales by viewModel.paginasLibroActual
    LaunchedEffect(bookKey) {
        cargandoDesdeBD = true

        if (idActual != 0L && idActual != -1L) {
            authRepository.obtenerLibroDeBiblioteca(idActual, bookKey)
                .onSuccess { miLibro ->
                    libroEnBiblioteca = miLibro
                    if (snapshotBook == null) {
                        snapshotBook = Book(
                            key             = miLibro.bookKey,
                            title           = miLibro.titulo,
                            authorNames     = listOfNotNull(miLibro.autor),
                            firstPublishYear = null,
                            coverId         = miLibro.cover_id,
                            language        = null,
                            numeroPaginas   = miLibro.paginas_totales
                        )
                    }
                    val totalPaginas = (paginasReales ?: miLibro.paginas_totales ?: 0)
                    if (totalPaginas > 0) {
                        sliderPagina = ((miLibro.progreso_porcentaje / 100f) * totalPaginas)
                    }
                }
        }

        val libroActual = snapshotBook
        if (libroActual != null) {
            viewModel.cargarPaginasLibro(
                titulo = libroActual.title,
                autor  = libroActual.authorNames?.firstOrNull()
            )
        }

        cargandoDesdeBD = false
    }

    LaunchedEffect(snapshotBook?.key) {
        val libroActual = snapshotBook ?: return@LaunchedEffect
        if (paginasReales == null) {
            viewModel.cargarPaginasLibro(
                titulo = libroActual.title,
                autor  = libroActual.authorNames?.firstOrNull()
            )
        }
    }

    val book = snapshotBook

    val coverScale by animateFloatAsState(
        targetValue = if (book != null) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "coverScale"
    )

    val guardarLibro = { estadoElegido: String ->
        if (book != null && !guardando) {
            guardando = true
            coroutineScope.launch {
                val miLibro = MiLibro(
                    id_usuario          = idActual,
                    bookKey             = book.key,
                    titulo              = book.title,
                    autor               = book.authorNames?.firstOrNull(),
                    cover_id            = book.coverId,
                    estado              = estadoElegido,
                    progreso_porcentaje = 0,
                    paginas_totales     = paginasReales ?: book.numeroPaginas
                )

                authRepository.agregarLibroABiblioteca(miLibro).onSuccess {
                    val nombreLista = when (estadoElegido) {
                        "deseado" -> "Wishlist"
                        "leyendo" -> "Leyendo"
                        else      -> "Terminados"
                    }
                    Toast.makeText(context, "Añadido a $nombreLista ✓", Toast.LENGTH_SHORT).show()
                    yaGuardado     = true
                    estadoGuardado = nombreLista
                    viewModel.recargaTrasGuardar()
                }.onFailure {
                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
                guardando = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { padding ->

        if (book == null) {
            val isLoading = recomendadosState is BookUiState.Loading || searchState is BookUiState.Loading || cargandoDesdeBD

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No se pudo cargar la información", color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Puede que el libro ya no esté disponible",
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AsyncImage(
                        model = "https://covers.openlibrary.org/b/id/${book.coverId}-L.jpg",
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(28.dp)
                            .graphicsLayer { alpha = 0.35f },
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                    ),
                                    startY = 200f
                                )
                            )
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        AsyncImage(
                            model = "https://covers.openlibrary.org/b/id/${book.coverId}-L.jpg",
                            contentDescription = book.title,
                            modifier = Modifier
                                .height(260.dp)
                                .width(175.dp)
                                .graphicsLayer {
                                    scaleX = coverScale
                                    scaleY = coverScale
                                }
                                .shadow(
                                    elevation = 32.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    ambientColor = Color.Black.copy(alpha = 0.5f)
                                )
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = book.authorNames?.joinToString(", ") ?: "Autor desconocido",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 20.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookStatItem(
                        label = "Publicado",
                        value = "${book.firstPublishYear ?: "---"}"
                    )

                    VerticalDivider(
                        modifier = Modifier.height(36.dp).width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    BookStatItem(
                        label     = "Páginas",
                        value     = paginasReales?.toString()
                            ?: book.numeroPaginas?.toString()
                            ?: "---",
                        isLoading = paginasReales == null && book.numeroPaginas == null
                    )

                    VerticalDivider(
                        modifier = Modifier.height(36.dp).width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Botón guardar
                    Box {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { if (!guardando && !yaGuardado) menuAbierto = true },
                                enabled = idActual != 0L && !yaGuardado,
                                modifier = Modifier.size(44.dp)
                            ) {
                                when {
                                    guardando -> CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    yaGuardado -> Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Guardado",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = "Añadir",
                                        tint = if (idActual != 0L)
                                            MaterialTheme.colorScheme.primary
                                        else Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Text(
                                text = when {
                                    yaGuardado     -> estadoGuardado
                                    idActual == 0L -> "Inicia sesión"
                                    else           -> "Guardar"
                                },
                                fontSize = 11.sp,
                                color = when {
                                    yaGuardado -> Color(0xFF4CAF50)
                                    else       -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (yaGuardado) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        DropdownMenu(
                            expanded = menuAbierto,
                            onDismissRequest = { menuAbierto = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            listOf(
                                "deseado"   to "📚  Añadir a Wishlist",
                                "leyendo"   to "📖  Añadir a Leyendo",
                                "terminado" to "✅  Añadir a Terminados"
                            ).forEach { (estado, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        menuAbierto = false
                                        guardarLibro(estado)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                val totalPaginas = paginasReales ?: libroEnBiblioteca?.paginas_totales ?: 0
                val estaLeyendo  = libroEnBiblioteca?.estado == "leyendo"

                var inputPagina by remember { mutableStateOf("") }

                LaunchedEffect(libroEnBiblioteca, paginasReales) {
                    val lib = libroEnBiblioteca ?: return@LaunchedEffect
                    val total = paginasReales ?: lib.paginas_totales ?: 0
                    val paginaActual = if (total > 0)
                        ((lib.progreso_porcentaje / 100f) * total).toInt()
                    else
                        lib.progreso_porcentaje
                    sliderPagina = paginaActual.toFloat()
                    inputPagina  = paginaActual.toString()
                }

                if (estaLeyendo) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        // Cabecera
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp).height(22.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text       = "Mi progreso",
                                style      = MaterialTheme.typography.titleLarge,
                                color      = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(20.dp),
                            color    = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (totalPaginas > 0)
                                                "Página ${sliderPagina.toInt()} de $totalPaginas"
                                            else
                                                "${sliderPagina.toInt()}% completado",
                                            fontSize   = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (totalPaginas > 0) {
                                            Text(
                                                text     = "${((sliderPagina / totalPaginas) * 100).toInt()}% completado",
                                                fontSize = 12.sp,
                                                color    = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (totalPaginas > 0)
                                                "${((sliderPagina / totalPaginas) * 100).toInt()}%"
                                            else
                                                "${sliderPagina.toInt()}%",
                                            fontSize   = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color      = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                val rangoMax = if (totalPaginas > 0) totalPaginas.toFloat() else 100f
                                Slider(
                                    value       = sliderPagina.coerceIn(0f, rangoMax),
                                    onValueChange = { nuevo ->
                                        sliderPagina     = nuevo
                                        inputPagina      = nuevo.toInt().toString()
                                        progresoGuardado = false
                                    },
                                    valueRange  = 0f..rangoMax,
                                    modifier    = Modifier.fillMaxWidth(),
                                    colors      = SliderDefaults.colors(
                                        thumbColor         = MaterialTheme.colorScheme.primary,
                                        activeTrackColor   = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        activeTickColor    = Color.Transparent,
                                        inactiveTickColor  = Color.Transparent
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value       = inputPagina,
                                        onValueChange = { nuevo ->
                                            if (nuevo.all { it.isDigit() } && nuevo.length <= 4) {
                                                inputPagina = nuevo
                                                val num = nuevo.toIntOrNull() ?: 0
                                                val clamped = num.toFloat().coerceIn(0f, rangoMax)
                                                sliderPagina     = clamped
                                                progresoGuardado = false
                                            }
                                        },
                                        modifier    = Modifier.weight(1f),
                                        singleLine  = true,
                                        label       = {
                                            Text(
                                                if (totalPaginas > 0) "Página actual" else "Porcentaje (%)",
                                                fontSize = 12.sp
                                            )
                                        },
                                        suffix      = {
                                            if (totalPaginas > 0) {
                                                Text(
                                                    "/ $totalPaginas",
                                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        },
                                        shape       = RoundedCornerShape(14.dp),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction    = ImeAction.Done
                                        ),
                                        colors      = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            focusedLabelColor    = MaterialTheme.colorScheme.primary,
                                            cursorColor          = MaterialTheme.colorScheme.primary,
                                            focusedContainerColor   = Color.Transparent,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            val libro = libroEnBiblioteca ?: return@Button
                                            guardandoProgreso = true
                                            val nuevoPorcentaje = if (totalPaginas > 0)
                                                ((sliderPagina / totalPaginas) * 100).toInt().coerceIn(0, 100)
                                            else
                                                sliderPagina.toInt().coerceIn(0, 100)

                                            coroutineScope.launch {
                                                authRepository.actualizarLibroEnBiblioteca(
                                                    libro.copy(progreso_porcentaje = nuevoPorcentaje)
                                                ).onSuccess {
                                                    libroEnBiblioteca = libro.copy(progreso_porcentaje = nuevoPorcentaje)
                                                    progresoGuardado  = true
                                                    guardandoProgreso = false
                                                    Toast.makeText(context, "Progreso guardado ✓", Toast.LENGTH_SHORT).show()
                                                }.onFailure {
                                                    guardandoProgreso = false
                                                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        enabled = !guardandoProgreso && !progresoGuardado,
                                        shape   = RoundedCornerShape(14.dp),
                                        modifier = Modifier.height(56.dp),
                                        colors  = ButtonDefaults.buttonColors(
                                            containerColor        = if (progresoGuardado) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                            disabledContainerColor = if (progresoGuardado) Color(0xFF4CAF50).copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        if (guardandoProgreso) {
                                            CircularProgressIndicator(
                                                modifier    = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color       = Color.White
                                            )
                                        } else {
                                            Text(
                                                text       = if (progresoGuardado) "✓" else "Guardar",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize   = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sobre este libro",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Explora esta obra de ${book.authorNames?.firstOrNull() ?: "este autor"}. " +
                                    "Publicado originalmente en ${book.firstPublishYear ?: "fecha desconocida"}, " +
                                    "este libro sigue siendo una referencia en su género. " +
                                    "Añádelo a tu biblioteca para llevar el control de tu lectura.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun BookStatItem(
    label: String,
    value: String,
    isLoading: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        if (isLoading) {
            val infiniteTransition = rememberInfiniteTransition(label = "shimmer_pages")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue  = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(700, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha_pages"
            )
            Box(
                modifier = Modifier
                    .size(48.dp, 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        } else {
            Text(
                text       = value,
                color      = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = label,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
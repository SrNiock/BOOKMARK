package com.example.bookmark.ui.screens

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.bookmark.data.remote.dto.obtenerNumeroPaginasGoogleBooks
import com.example.bookmark.ui.navigation.Screen
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.utils.SessionManager
import com.example.bookmark.ui.supaBase.MiLibro
import com.example.bookmark.ui.supaBase.Publicacion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibliotecaScreen(navController: NavHostController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val authRepository = remember { AuthRepository() }
    val coroutineScope = rememberCoroutineScope()

    val idActual = sessionManager.obtenerIdSesion() ?: -1L
    val snackbarHostState = remember { SnackbarHostState() }

    var tabIndex by remember { mutableStateOf(0) }
    val pestañas = listOf("Leyendo", "Terminados", "Wishlist")

    var listaLibros by remember { mutableStateOf<List<MiLibro>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    var mostrarDialogo by remember { mutableStateOf(false) }
    var libroSeleccionado by remember { mutableStateOf<MiLibro?>(null) }
    var paginaInput by remember { mutableStateOf("") }

    var mostrarDialogoResena by remember { mutableStateOf(false) }
    var textoResena by remember { mutableStateOf("") }
    var calificacionResena by remember { mutableStateOf(5) }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_bib")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmer_alpha"
    )

    fun cargarDatos(tab: Int = tabIndex) {
        if (idActual == -1L) { cargando = false; return }
        cargando = true
        val estadoBuscado = when (tab) {
            0 -> "leyendo"
            1 -> "terminado"
            else -> "deseado"
        }
        coroutineScope.launch {
            val resultado = authRepository.obtenerLibrosPorEstado(idActual, estadoBuscado)
            listaLibros = resultado.getOrNull() ?: emptyList()
            cargando = false
        }
    }

    LaunchedEffect(tabIndex) { cargarDatos(tabIndex) }

    val onFavorito: (MiLibro) -> Unit = { libro ->
        if (libro.id != null) {
            coroutineScope.launch {
                val conteo = authRepository.contarFavoritos(idActual).getOrDefault(0L)
                if (conteo >= 4) {
                    snackbarHostState.showSnackbar("Máximo 4 favoritos permitidos")
                } else {
                    authRepository.agregarAFavoritos(idActual, libro.id).onSuccess {
                        snackbarHostState.showSnackbar("Añadido a favoritos ❤️")
                    }.onFailure {
                        snackbarHostState.showSnackbar("El libro ya está en favoritos")
                    }
                }
            }
        }
    }

    val onEliminar: (MiLibro) -> Unit = { libro ->
        if (libro.id != null) {
            coroutineScope.launch {
                authRepository.eliminarLibroDeBiblioteca(libro.id).onSuccess {
                    cargarDatos(tabIndex)
                    snackbarHostState.showSnackbar("Libro eliminado")
                }
            }
        }
    }

    val onMoverALeyendo: (MiLibro) -> Unit = { libro ->
        coroutineScope.launch {
            authRepository.actualizarLibroEnBiblioteca(libro.copy(estado = "leyendo")).onSuccess {
                cargarDatos(tabIndex)
                snackbarHostState.showSnackbar("Movido a Leyendo")
            }
        }
    }

    // ── DIÁLOGO PROGRESO ──
    if (mostrarDialogo && libroSeleccionado != null) {
        val libro = libroSeleccionado!!

        var totalPaginas     by remember(libro.id) { mutableStateOf(libro.paginas_totales) }
        var cargandoPaginas  by remember(libro.id) { mutableStateOf(libro.paginas_totales == null) }
        var inputTexto       by remember(libro.id) { mutableStateOf("") }
        var sliderValor      by remember(libro.id) { mutableStateOf(0f) }

        // Inicializa slider e input cuando ya tenemos el total (sea de BD o de Google)
        fun inicializarDesdeTotal(total: Int) {
            val paginaActual = ((libro.progreso_porcentaje / 100f) * total).toInt()
            sliderValor = paginaActual.toFloat()
            inputTexto  = paginaActual.toString()
        }

        // Si ya teníamos el total en BD, inicializamos directamente
        LaunchedEffect(libro.id) {
            val totalEnBD = libro.paginas_totales
            if (totalEnBD != null && totalEnBD > 0) {
                inicializarDesdeTotal(totalEnBD)
            } else {
                // Pedimos a Google Books
                val paginas = obtenerNumeroPaginasGoogleBooks(
                    titulo = libro.titulo,
                    autor  = libro.autor
                )
                cargandoPaginas = false
                if (paginas != null && paginas > 0) {
                    totalPaginas = paginas
                    inicializarDesdeTotal(paginas)
                    // Persistimos para no volver a pedirlo
                    authRepository.actualizarLibroEnBiblioteca(
                        libro.copy(paginas_totales = paginas)
                    )
                    libroSeleccionado = libro.copy(paginas_totales = paginas)
                } else {
                    // Google no respondió: modo porcentaje directo
                    sliderValor = libro.progreso_porcentaje.toFloat()
                    inputTexto  = libro.progreso_porcentaje.toString()
                }
            }
        }

        val total    = totalPaginas ?: 0
        val rangoMax = if (total > 0) total.toFloat() else 100f

        // Porcentaje calculado en tiempo real
        val porcentaje = if (total > 0)
            ((sliderValor / total) * 100).toInt().coerceIn(0, 100)
        else
            sliderValor.toInt().coerceIn(0, 100)

        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Text(
                        text       = libro.titulo,
                        color      = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    if (!libro.autor.isNullOrBlank()) {
                        Text(
                            text     = libro.autor,
                            color    = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // ── Badge porcentaje ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text       = "$porcentaje%",
                                fontSize   = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            if (cargandoPaginas) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color       = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Cargando páginas...",
                                        fontSize = 12.sp,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text  = if (total > 0) "Página ${sliderValor.toInt()} de $total"
                                    else "completado",
                                    fontSize = 13.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ── Slider ──
                    Slider(
                        value         = sliderValor.coerceIn(0f, rangoMax),
                        onValueChange = { v ->
                            sliderValor = v
                            inputTexto  = v.toInt().toString()
                        },
                        valueRange    = 0f..rangoMax,
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = SliderDefaults.colors(
                            thumbColor         = MaterialTheme.colorScheme.primary,
                            activeTrackColor   = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            activeTickColor    = Color.Transparent,
                            inactiveTickColor  = Color.Transparent
                        )
                    )

                    // ── Campo: número de página ──
                    OutlinedTextField(
                        value         = inputTexto,
                        onValueChange = { v ->
                            if (v.all { it.isDigit() } && v.length <= 4) {
                                inputTexto  = v
                                val num     = v.toIntOrNull() ?: 0
                                sliderValor = num.toFloat().coerceIn(0f, rangoMax)
                            }
                        },
                        modifier        = Modifier.fillMaxWidth(),
                        singleLine      = true,
                        label           = { Text(if (total > 0) "Página actual" else "Porcentaje (%)") },
                        suffix          = {
                            if (cargandoPaginas) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color       = MaterialTheme.colorScheme.primary
                                )
                            } else if (total > 0) {
                                Text("/ $total", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        },
                        shape           = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction    = ImeAction.Done
                        ),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedLabelColor       = MaterialTheme.colorScheme.primary,
                            cursorColor             = MaterialTheme.colorScheme.primary,
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            listaLibros = listaLibros.map {
                                if (it.id == libro.id) it.copy(progreso_porcentaje = porcentaje) else it
                            }
                            coroutineScope.launch {
                                authRepository.actualizarLibroEnBiblioteca(
                                    libro.copy(progreso_porcentaje = porcentaje)
                                )
                            }
                            mostrarDialogo = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Guardar progreso", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick  = { mostrarDialogo = false; mostrarDialogoResena = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Text("¡Ya lo terminé! 🎉", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }

                    TextButton(onClick = { mostrarDialogo = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            dismissButton = null
        )
    }

    // ── DIÁLOGO RESEÑA ──
    if (mostrarDialogoResena && libroSeleccionado != null) {
        val libro = libroSeleccionado!!
        AlertDialog(
            onDismissRequest = { mostrarDialogoResena = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    libro.titulo,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "¿Cómo puntuarías este libro?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Estrella $i",
                                tint = if (i <= calificacionResena) Color(0xFFFFD700)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp).padding(4.dp).clickable { calificacionResena = i }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = textoResena,
                        onValueChange = { textoResena = it },
                        label = { Text("¿Qué te ha parecido?") },
                        placeholder = { Text("Escribe tu reseña...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                var publicando by remember { mutableStateOf(false) }
                Button(
                    enabled = !publicando,
                    onClick = {
                        publicando = true
                        coroutineScope.launch {
                            val nuevaPublicacion = Publicacion(
                                usuario_id = idActual,
                                book_key = libro.bookKey,
                                titulo_libro = libro.titulo,
                                cover_id = libro.cover_id,
                                texto = textoResena,
                                calificacion = calificacionResena
                            )
                            authRepository.crearPublicacion(nuevaPublicacion)
                            authRepository.actualizarLibroEnBiblioteca(libro.copy(estado = "terminado", progreso_porcentaje = 100))
                            mostrarDialogoResena = false
                            textoResena = ""
                            calificacionResena = 5
                            publicando = false
                            cargarDatos(tabIndex)
                            snackbarHostState.showSnackbar("¡Enhorabuena! Libro terminado 🎉")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(50)
                ) {
                    if (publicando) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Publicar y Terminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoResena = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (idActual == -1L) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📚", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Inicia sesión para ver tu biblioteca", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(paddingValues).fillMaxSize(), verticalArrangement = Arrangement.Top) {

            // ── CABECERA ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp) // altura fija — los tabs nunca se mueven
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mi Biblioteca",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Contador inline como badge — no añade altura
                if (!cargando && listaLibros.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${listaLibros.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── TABS — estilo coherente con BooksScreen ──
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                pestañas.forEachIndexed { index, titulo ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = {
                            Text(
                                titulo,
                                fontWeight = if (tabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (tabIndex == index) MaterialTheme.colorScheme.onBackground else Color.Gray
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── CONTENIDO — weight(1f) para que no empuje los tabs al cambiar layout ──
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (cargando) {
                    if (tabIndex == 0) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(4) { LibroBibliotecaSkeletonItem(shimmerAlpha) }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(9) { LibroGridSkeletonItem(shimmerAlpha) }
                        }
                    }
                } else if (listaLibros.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Text(
                                text = when (tabIndex) { 0 -> "📖"; 1 -> "🏆"; else -> "🔖" },
                                fontSize = 48.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = when (tabIndex) {
                                    0 -> "No estás leyendo nada ahora mismo"
                                    1 -> "Tu biblioteca de terminados está vacía"
                                    else -> "Tu lista de pendientes está vacía"
                                },
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Busca un libro y añádelo desde sus detalles",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    if (tabIndex == 0) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(listaLibros, key = { it.id ?: it.bookKey }) { libro ->
                                LibroBibliotecaItem(
                                    libro = libro,
                                    tabIndex = tabIndex,
                                    navController = navController,
                                    onClick = {
                                        libroSeleccionado = libro
                                        paginaInput = ""
                                        mostrarDialogo = true
                                    },
                                    onEliminar = { onEliminar(libro) },
                                    onFavorito = { onFavorito(libro) },
                                    onMoverALeyendo = { onMoverALeyendo(libro) }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(listaLibros, key = { it.id ?: it.bookKey }) { libro ->
                                LibroGridItem(
                                    libro = libro,
                                    tabIndex = tabIndex,
                                    navController = navController,
                                    onEliminar = { onEliminar(libro) },
                                    onFavorito = { onFavorito(libro) },
                                    onMoverALeyendo = { onMoverALeyendo(libro) }
                                )
                            }
                        }
                    }
                }
            } // cierre Box weight(1f)
        }
    }
}

// ─────────────────────────────────────────────
// ITEM LEYENDO — rediseñado
// ─────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibroBibliotecaItem(
    libro: MiLibro,
    tabIndex: Int,
    navController: NavHostController,
    onClick: () -> Unit,
    onEliminar: () -> Unit,
    onFavorito: () -> Unit,
    onMoverALeyendo: () -> Unit
) {
    var mostrarMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        val key = libro.bookKey
                        if (key.isNotEmpty()) navController.navigate(Screen.BookDetail(bookKey = Uri.encode(key)))
                    },
                    onLongClick = { onClick(); mostrarMenu = true }
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                // Portada con sombra de color primario
                AsyncImage(
                    model = "https://covers.openlibrary.org/b/id/${libro.cover_id}-M.jpg",
                    contentDescription = libro.titulo,
                    modifier = Modifier
                        .size(60.dp, 88.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(10.dp),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                    Text(
                        text = libro.titulo,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = libro.autor ?: "Autor desconocido",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Barra de progreso
                    LinearProgressIndicator(
                        progress = { libro.progreso_porcentaje / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge de porcentaje
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${libro.progreso_porcentaje}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "Mantén pulsado",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = mostrarMenu,
            onDismissRequest = { mostrarMenu = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .clip(RoundedCornerShape(16.dp))
        ) {
            if (tabIndex == 2) {
                DropdownMenuItem(
                    text = { Text("Empezar a leer", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { mostrarMenu = false; onMoverALeyendo() }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Añadir a favoritos ❤️", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { mostrarMenu = false; onFavorito() }
                )
            }
            DropdownMenuItem(
                text = { Text("Eliminar de biblioteca", color = MaterialTheme.colorScheme.error) },
                onClick = { mostrarMenu = false; onEliminar() }
            )
        }
    }
}

// ─────────────────────────────────────────────
// ITEM CUADRÍCULA — rediseñado
// ─────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibroGridItem(
    libro: MiLibro,
    tabIndex: Int,
    navController: NavHostController,
    onEliminar: () -> Unit,
    onFavorito: () -> Unit,
    onMoverALeyendo: () -> Unit
) {
    var mostrarMenu by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        val key = libro.bookKey
                        if (key.isNotEmpty()) navController.navigate(Screen.BookDetail(bookKey = Uri.encode(key)))
                    },
                    onLongClick = { mostrarMenu = true }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = "https://covers.openlibrary.org/b/id/${libro.cover_id}-M.jpg",
                contentDescription = libro.titulo,
                modifier = Modifier
                    .height(140.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Text(
                text = libro.titulo,
                fontSize = 11.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        DropdownMenu(
            expanded = mostrarMenu,
            onDismissRequest = { mostrarMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            if (tabIndex == 2) {
                DropdownMenuItem(
                    text = { Text("Empezar a leer", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { mostrarMenu = false; onMoverALeyendo() }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Añadir a favoritos ❤️", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { mostrarMenu = false; onFavorito() }
                )
            }
            DropdownMenuItem(
                text = { Text("Eliminar de biblioteca", color = MaterialTheme.colorScheme.error) },
                onClick = { mostrarMenu = false; onEliminar() }
            )
        }
    }
}

// ─────────────────────────────────────────────
// ESQUELETOS
// ─────────────────────────────────────────────
@Composable
fun LibroBibliotecaSkeletonItem(alpha: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp, 88.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
                Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
            }
        }
    }
}

@Composable
fun LibroGridSkeletonItem(alpha: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )
        Box(Modifier.fillMaxWidth(0.8f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
        Box(Modifier.fillMaxWidth(0.5f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha * 0.6f)))
    }
}
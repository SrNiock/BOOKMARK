package com.example.bookmark.data.remote

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmark.data.remote.dto.Book
import com.example.bookmark.data.remote.dto.obtenerNumeroPaginasGoogleBooks
import com.example.bookmark.ui.supaBase.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

sealed class BookUiState {
    object Loading : BookUiState()
    data class Success(val books: List<Book>) : BookUiState()
    data class Error(val message: String) : BookUiState()
}

class BookViewModel : ViewModel() {
    private val repository = BookRepository()
    private val authRepository = AuthRepository()

    // Estado pantalla  Búsqueda
    private var _searchState: MutableState<BookUiState> =
        mutableStateOf(BookUiState.Success(emptyList()))
    val searchState: State<BookUiState> = _searchState

    // Estado Carrusel
    private var _recommendadosState: MutableState<BookUiState> =
        mutableStateOf(BookUiState.Loading)
    val recommendadosState: State<BookUiState> = _recommendadosState

    private var searchJob: Job? = null
    private var keysEnBiblioteca: List<String> = emptyList()
    private var autoresRelevantes: List<String> = emptyList()
    @Volatile
    private var isCurrentlyLoading = false

    private val consultasPorDefecto = listOf("bestseller", "fantasy", "fiction", "mystery", "adventure", "tolkien")
    private fun obtenerQueryPorDefecto(): String = consultasPorDefecto.random()

    private var usuarioActualId: Long? = null
    private var ultimoUsuarioCargado: Long? = null
    private var currentPage = 1
    private var currentSearchQuery = ""
    private var _isLastPage = mutableStateOf(false)
    val isLastPage: State<Boolean> = _isLastPage
    private val accumulatedSearchBooks = mutableListOf<Book>()


    fun cargarRecomendaciones(idUsuario: Long) {
        val librosCargados = (_recommendadosState.value as? BookUiState.Success)?.books
        if (ultimoUsuarioCargado == idUsuario && !librosCargados.isNullOrEmpty()) return

        usuarioActualId = idUsuario
        ultimoUsuarioCargado = idUsuario

        viewModelScope.launch {
            authRepository.obtenerDatosParaRecomendaciones(idUsuario).onSuccess { datos ->
                keysEnBiblioteca = datos.keysEnBiblioteca
                autoresRelevantes = datos.autores
                val query = construirQueryRecomendacion(datos.autores, datos.titulosActivos)
                ejecutarBusquedaRecomendados(query)
            }.onFailure {
                ejecutarBusquedaRecomendados("the most popular books")
            }
        }
    }
    private var _paginasLibroActual = mutableStateOf<Int?>(null)
    val paginasLibroActual: State<Int?> = _paginasLibroActual

    fun cargarPaginasLibro(titulo: String, autor: String?) {
        _paginasLibroActual.value = null
        viewModelScope.launch {
            _paginasLibroActual.value = obtenerNumeroPaginasGoogleBooks(titulo, autor)
        }
    }
    private fun construirQueryRecomendacion(
        autores: List<String>,
        titulosActivos: List<String>
    ): String {
        if (autores.isEmpty() && titulosActivos.isEmpty()) return "the most popular books"

        if (autores.isEmpty() && titulosActivos.isNotEmpty()) return titulosActivos.first()

        val apellidoPrincipal = autores.first().trim().split(" ").last()

        return if (titulosActivos.isNotEmpty()) {
            val tituloCorto = titulosActivos.first()
                .split(" ")
                .filter { it.length > 3 }
                .take(2)
                .joinToString(" ")
                .ifBlank { null }

            if (tituloCorto != null) "$apellidoPrincipal $tituloCorto"
            else apellidoPrincipal
        } else {
            apellidoPrincipal
        }
    }

    fun obtenerLibrosDefault() {
        if (ultimoUsuarioCargado == null &&
            _recommendadosState.value is BookUiState.Success &&
            (_recommendadosState.value as BookUiState.Success).books.isNotEmpty()
        ) return

        ejecutarBusquedaRecomendados("the most popular books")
    }

    fun recargaTrasGuardar() {
        val id = usuarioActualId ?: return
        ultimoUsuarioCargado = null
        cargarRecomendaciones(id)
    }

    fun forzarRecargaRecomendaciones(idUsuario: Long) {
        usuarioActualId = idUsuario
        ultimoUsuarioCargado = null
        cargarRecomendaciones(idUsuario)
    }

    private fun ejecutarBusquedaRecomendados(query: String) {
        viewModelScope.launch {
            if (_recommendadosState.value !is BookUiState.Success) {
                _recommendadosState.value = BookUiState.Loading
            }

            try {
                val results = withTimeout(10000L) {
                    repository.searchBooks(query, 1)
                }

                val filtrados = results.filter { book ->
                    !keysEnBiblioteca.contains(book.key) && book.coverId != null
                }

                if (filtrados.size < 10 && query !in consultasPorDefecto) {
                    ejecutarBusquedaRecomendados(obtenerQueryPorDefecto())
                    return@launch
                }

                val ordenados = if (autoresRelevantes.isNotEmpty()) {
                    val apellidosPorPrioridad = autoresRelevantes
                        .map { it.trim().split(" ").last().lowercase() }

                    filtrados.sortedWith(Comparator { a, b ->
                        val prioA = apellidosPorPrioridad.indexOfFirst { apellido ->
                            a.authorNames?.any { autor -> autor.lowercase().contains(apellido) } == true
                        }.let { if (it == -1) Int.MAX_VALUE else it }

                        val prioB = apellidosPorPrioridad.indexOfFirst { apellido ->
                            b.authorNames?.any { autor -> autor.lowercase().contains(apellido) } == true
                        }.let { if (it == -1) Int.MAX_VALUE else it }

                        prioA.compareTo(prioB)
                    })
                } else {
                    filtrados
                }

                _recommendadosState.value = BookUiState.Success(ordenados.take(30))
            } catch (e: Exception) {
                _recommendadosState.value = BookUiState.Error("Error de red")
            }
        }
    }



    fun onQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.length < 3) {

            if (query.isEmpty()) _searchState.value = BookUiState.Success(emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(600)
            searchBooks(query, isNextPage = false)
        }
    }

    fun searchBooks(query: String, isNextPage: Boolean = false) {
        if (isCurrentlyLoading || (isNextPage && _isLastPage.value)) return

        if (!isNextPage) {
            currentPage = 1
            _isLastPage.value = false
            accumulatedSearchBooks.clear()
            currentSearchQuery = query
            _searchState.value = BookUiState.Loading
        }

        viewModelScope.launch {
            isCurrentlyLoading = true
            try {
                val queryText = currentSearchQuery.trim().lowercase()
                val effectiveQuery = if (!queryText.contains(" ") &&
                    !queryText.contains("*") &&
                    !queryText.contains(":")
                ) {
                    "$queryText*"
                } else {
                    queryText
                }

                val regexPattern = effectiveQuery.replace("*", ".*").toRegex()

                val rawResponse = withTimeout(15000L) {
                    repository.searchBooks(effectiveQuery, currentPage)
                }

                if (rawResponse.isEmpty()) {
                    _isLastPage.value = true
                    if (currentPage == 1) _searchState.value = BookUiState.Success(emptyList())
                    return@launch
                }

                val rankedBooks: List<Book> = withContext(Dispatchers.Default) {
                    rawResponse
                        .filter { !it.title.isNullOrBlank() }
                        .map { book -> Pair(book, calculateBookScore(book, queryText, regexPattern)) }
                        .filter { it.second >= 50 }
                        .sortedByDescending { it.second }
                        .map { it.first }
                }

                accumulatedSearchBooks.addAll(rankedBooks)
                _searchState.value =
                    BookUiState.Success(accumulatedSearchBooks.distinctBy { it.key }.toList())
                currentPage++

            } catch (e: Exception) {
                _searchState.value = BookUiState.Error("Error: ${e.message}")
            } finally {
                isCurrentlyLoading = false
            }
        }
    }

    private fun calculateBookScore(book: Book, query: String, regex: Regex): Int {
        val rawTitle = book.title ?: ""
        val title = rawTitle.lowercase().replace(":", "").trim()
        if (title.isBlank()) return -500
        var score = 0
        val cleanQuery = query.replace("*", "").trim().lowercase()

        if (title == cleanQuery) score += 500
        if (title.contains(cleanQuery)) score += 200
        if (book.coverId != null) score += 200
        if (!book.authorNames.isNullOrEmpty() && book.authorNames.first() != "Autor desconocido") {
            score += 150
        } else {
            score -= 300
        }

        val noiseWords = setOf("guide", "coloring", "summary", "workbook")
        noiseWords.forEach { word ->
            if (title.contains(word) && !cleanQuery.contains(word)) score -= 400
        }

        val diff = (title.length - cleanQuery.length).coerceAtLeast(0)
        return score - (diff * 5)
    }
}
package com.example.bookmark.data.remote

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmark.data.remote.dto.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException

sealed class BookUiState {
    object Loading : BookUiState()
    data class Success(val books: List<Book>) : BookUiState()
    data class Error(val message: String) : BookUiState()
}

class BookViewModel : ViewModel() {
    private val repository = BookRepository()

    private var _state: MutableState<BookUiState> = mutableStateOf(BookUiState.Loading)
    val state: State<BookUiState> = _state

    private var searchJob: Job? = null

    // Variables de control de paginación
    private var currentPage = 1
    private var currentQuery = ""
    private var _isLastPage = mutableStateOf(false)
    val isLastPage: State<Boolean> = _isLastPage
    private val accumulatedBooks = mutableListOf<Book>()
    private var isCurrentlyLoading = false

    // Búsqueda automática cuando dejas de escribir
    fun onQueryChanged(query: String) {
        searchJob?.cancel()

        if (query.length < 3) return

        searchJob = viewModelScope.launch {
            delay(600)
            searchBooks(query, isNextPage = false)
        }
    }

    init {
        searchBooks("the lord of the rings", isNextPage = false)
    }

    private fun calcultateBookScore(book: Book, query: String, regex: Regex): Int {
        val rawTitle = book.title ?: ""
        val title = rawTitle.lowercase()
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace(":", "")
            .trim()

        if (title.isBlank()) return -500
        var score = 0

        val cleanQuery = query.replace("*", "").trim().lowercase()

        // 1. COINCIDENCIA DE TEXTO
        if (title == cleanQuery) score += 500
        if (title.contains(cleanQuery)) score += 200

        // 2. PALABRAS CLAVE
        val stopWords = setOf("the", "a", "an", "el", "la", "de", "of", "in", "and")
        val searchWords = cleanQuery.split("\\s+".toRegex()).filter { it.length > 2 && !stopWords.contains(it) }
        score += searchWords.count { title.contains(it) } * 80

        // 3. CALIDAD DE DATOS
        if (book.coverId != null) {
            score += 200
        }

        if (!book.authorNames.isNullOrEmpty() && book.authorNames.first() != "Autor desconocido") {
            score += 150
        } else {
            score -= 300
        }

        // 4. FILTRO DE RUIDO
        val noiseWords = setOf("guide", "coloring", "summary", "workbook", "study", "notes")
        noiseWords.forEach { word ->
            if (title.contains(word) && !cleanQuery.contains(word)) {
                score -= 400
            }
        }

        // 5. PENALIZACIÓN POR LONGITUD
        val diff = (title.length - cleanQuery.length).coerceAtLeast(0)
        return score - (diff * 5)
    }


    fun searchBooks(query: String, isNextPage: Boolean = false) {
        if (isCurrentlyLoading || (isNextPage && _isLastPage.value)) return

        // Si es una búsqueda nueva, reseteamos todo
        if (!isNextPage) {
            currentPage = 1
            _isLastPage.value = false
            accumulatedBooks.clear()
            currentQuery = query
            _state.value = BookUiState.Loading
        }

        viewModelScope.launch {
            isCurrentlyLoading = true // Bloqueamos

            try {
                val queryText: String = currentQuery.trim().lowercase()

                val effectiveQuery = if (!queryText.contains(" ") && !queryText.contains("*")) {
                    "$queryText*"
                } else {
                    queryText
                }
                val regexPattern = effectiveQuery.replace("*", ".*").toRegex()

                // 👇 AÑADIMOS EL TIMEOUT: Si en 15 segundos no hay respuesta, lanza error
                val rawResponse = withTimeout(15000L) {
                    repository.searchBooks(effectiveQuery, currentPage)
                }

                val validRawResponse = rawResponse.filter { !it.title.isNullOrBlank() }

                if (validRawResponse.isEmpty()) {
                    _isLastPage.value = true
                    if (currentPage == 1) _state.value = BookUiState.Success(emptyList())
                    return@launch
                }

                // Filtrado y Scoring en hilo de cómputo
                val rankedBooks: List<Book> = withContext(Dispatchers.Default) {
                    val scoredList: List<Pair<Book, Int>> = rawResponse
                        .filter { !it.title.isNullOrBlank() }
                        .map { book ->
                            val score = calcultateBookScore(book, queryText, regexPattern)
                            Pair(book, score)
                        }

                    scoredList
                        .filter { it.second >= 50 }
                        .sortedByDescending { it.second }
                        .map { it.first }
                }

                accumulatedBooks.addAll(rankedBooks)
                val finalResult = accumulatedBooks.distinctBy { it.key }

                _state.value = BookUiState.Success(finalResult.toList())
                currentPage++

            } catch (e: TimeoutCancellationException) {
                // 👇 CAPTURAMOS EL TIMEOUT (La API tardó demasiado)
                _state.value = BookUiState.Error("La conexión ha tardado demasiado. Inténtalo de nuevo.")
            } catch (e: IOException) {
                // 👇 CAPTURAMOS ERRORES DE RED (No hay internet o servidor caído)
                _state.value = BookUiState.Error("Error de red. Comprueba tu conexión a internet.")
            } catch (e: Exception) {
                // 👇 CAPTURAMOS CUALQUIER OTRO ERROR (Asegurándonos de que siempre salga del Loading)
                _state.value = BookUiState.Error("Error inesperado: ${e.message}")
            } finally {
                isCurrentlyLoading = false // Desbloqueamos siempre
            }
        }
    }
}
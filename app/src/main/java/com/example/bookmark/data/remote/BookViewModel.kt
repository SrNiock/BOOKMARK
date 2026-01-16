package com.example.bookmark.data.remote

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmark.data.remote.dto.Book
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
sealed class BookUiState {
    object Loading : BookUiState()
    data class Success(val books: List<Book>) : BookUiState()
    data class Error(val message: String) : BookUiState()
}

class BookViewModel : ViewModel() {
    // Cambiamos al nuevo repositorio de libros
    private val repository = BookRepository()

    private var _state: MutableState<BookUiState> = mutableStateOf(BookUiState.Loading)
    val state: State<BookUiState> = _state
    // Guardamos el trabajo de búsqueda para poder cancelarlo
    private var searchJob: Job? = null

    // Variables de control de paginación
    private var currentPage = 1
    private var currentQuery = ""
    private var _isLastPage = mutableStateOf(false)
    val isLastPage: State<Boolean> = _isLastPage
    private val accumulatedBooks = mutableListOf<Book>()
    private var isCurrentlyLoading = false

    // Busqueda automatica cuando dejas de escribir
    fun onQueryChanged(query: String) {
        searchJob?.cancel() // Cancelamos el temporizador anterior

        if (query.length < 3) return // No buscamos si hay menos de 3 letras

        searchJob = viewModelScope.launch {
            delay(600) // Esperamos a que el usuario deje de escribir
            searchBooks(query, isNextPage = false) // Llamamos a la función que ya teníamos
        }
    }
    init {
        // Podemos hacer una búsqueda inicial por defecto
        searchBooks("the lord of the rings", isNextPage = false)
    }

    private fun calcultateBookScore(book: Book, query: String, regex: Regex): Int {
        val rawTitle = book.title ?: ""
        val title = rawTitle.lowercase()
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace(":", "") // Eliminamos dos puntos para mejorar coincidencias
            .trim()

        if (title.isBlank()) return -500 // Seguridad extra
        var score = 0

        val cleanQuery = query.replace("*", "").trim().lowercase()

        // 1. COINCIDENCIA DE TEXTO
        if (title == cleanQuery) score += 500 // Exacto
        if (title.contains(cleanQuery)) score += 200 // Contiene la frase

        // 2. PALABRAS CLAVE (Importancia de términos individuales)
        val stopWords = setOf("the", "a", "an", "el", "la", "de", "of", "in", "and")
        val searchWords = cleanQuery.split("\\s+".toRegex()).filter { it.length > 2 && !stopWords.contains(it) }
        score += searchWords.count { title.contains(it) } * 80

        // 3. CALIDAD DE DATOS (Aumentamos los puntos para que sean prioritarios)
        // Si tiene portada, le damos un bono que supera casi cualquier match parcial
        if (book.coverId != null) {
            score += 200 // Antes era 50
        }

        // Si tiene autor real, sumamos; si no, penalizamos fuerte
        if (!book.authorNames.isNullOrEmpty() && book.authorNames.first() != "Autor desconocido") {
            score += 150 // Antes era 40
        } else {
            score -= 300 // Penalización mayor para libros "fantasma"
        }

        // 4. FILTRO DE RUIDO (Para evitar guías o libros de colorear)
        val noiseWords = setOf("guide", "coloring", "summary", "workbook", "study", "notes")
        noiseWords.forEach { word ->
            if (title.contains(word) && !cleanQuery.contains(word)) {
                score -= 400 // Penalización masiva si es una guía y no se buscó una guía
            }
        }

        // 5. PENALIZACIÓN POR LONGITUD
        val diff = (title.length - cleanQuery.length).coerceAtLeast(0)
        return score - (diff * 5)
    }


    fun searchBooks(query: String,isNextPage:Boolean = false) {

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
                //Nuevo busqueda con *
                val queryText: String = currentQuery.trim().lowercase()

                val effectiveQuery = if (!queryText.contains(" ") && !queryText.contains("*")) {
                    "$queryText*"
                } else {
                    queryText
                }
                val regexPattern = effectiveQuery.replace("*", ".*").toRegex()
                val rawResponse = repository.searchBooks(effectiveQuery, currentPage)

                // 1. Obtenemos los resultados "crudos" de la API

                val validRawResponse = rawResponse.filter { !it.title.isNullOrBlank() }

                if (validRawResponse.isEmpty()) {
                    _isLastPage.value = true
                    // Si es la primera página y está vacía, avisamos
                    if (currentPage == 1) _state.value = BookUiState.Success(emptyList())
                    return@launch
                }
                // 2. Filtrado y Scoring en hilo de cómputo (Optimización de CPU)
                // 1. Especificamos el tipo List<Book> en la variable
                val rankedBooks: List<Book> = withContext(Dispatchers.Default) {
                    // 2. Creamos una lista intermedia de pares (Libro, Puntuación)
                    // Esto ayuda al compilador a saber exactamente qué es cada cosa
                    val scoredList: List<Pair<Book, Int>> = rawResponse
                        .filter { !it.title.isNullOrBlank() }
                        .map { book ->
                            val score = calcultateBookScore(book, queryText, regexPattern)
                            Pair(book, score)
                        }

                    // 3. Filtramos y devolvemos solo los libros
                    scoredList
                        .filter { it.second >= 50 }
                        .sortedByDescending { it.second }
                        .map { it.first }
                }


                accumulatedBooks.addAll(rankedBooks)


                val finalResult = accumulatedBooks.distinctBy { it.key }
                _state.value = BookUiState.Success(finalResult.toList())

                currentPage++


            } catch (e: Exception) {
                if (isNextPage) {
                    _state.value = BookUiState.Error("Error de búsqueda")
                }
            }finally {
                isCurrentlyLoading = false // Desbloqueamos
            }
        }
    }
}
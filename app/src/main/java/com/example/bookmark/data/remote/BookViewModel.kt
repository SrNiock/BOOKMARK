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
    private var isLastPage = false
    private val accumulatedBooks = mutableListOf<Book>()


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


    fun searchBooks(query: String,isNextPage:Boolean = false) {


        // Si es una búsqueda nueva, reseteamos todo
        if (!isNextPage) {
            currentPage = 1
            isLastPage = false
            accumulatedBooks.clear()
            currentQuery = query
            _state.value = BookUiState.Loading
        }


        if (isLastPage) return


        viewModelScope.launch {
            try {
                val searchWords = currentQuery.trim().lowercase().split("\\s+".toRegex())

                // 1. Obtenemos los resultados "crudos" de la API
                val rawResponse = repository.searchBooks(currentQuery.trim(),currentPage)

                if (rawResponse.isEmpty()) {
                    isLastPage = true
                    // Si es la primera página y está vacía, avisamos
                    if (currentPage == 1) _state.value = BookUiState.Success(emptyList())
                    return@launch
                }
                // 2. Calculamos el Score de cada libro
                val rankedBooks = rawResponse.map { book ->
                    var score = 0
                    val title = book.title.lowercase()

                    // CRITERIO 1: Coincidencia Exacta (Prioridad Máxima)
                    if (title == query.lowercase()) score += 100

                    // CRITERIO 2: Empieza por la búsqueda
                    if (title.startsWith(query.lowercase())) score += 50

                    // CRITERIO 3: ¿Cuántas palabras de la búsqueda están en el título?
                    val matches = searchWords.count { word -> title.contains(word) }
                    score += matches * 30 // 30 puntos por cada palabra encontrada

                    // CRITERIO 4: Penalización si NO tiene portada o autor
                    if (book.coverId != null) score += 40
                    if (!book.authorNames.isNullOrEmpty()) score += 20

                    // Asociamos el libro con su puntuación
                    Pair(book, score)
                }

                // 3. FILTRADO AGRESIVO:
                // Si un libro no tiene al menos una palabra del título o un score mínimo, fuera.
                val filteredBooks = rankedBooks
                    .filter { it.second >= 50 } // Umbral de calidad: Ajusta este número
                    .sortedByDescending { it.second } // Los mejores primero
                    .map { it.first } // Nos quedamos solo con el objeto Book

                accumulatedBooks.addAll(filteredBooks)


                val finalResult = accumulatedBooks.distinctBy { it.title.lowercase() }
                _state.value = BookUiState.Success(finalResult.toList())

                currentPage++


            } catch (e: Exception) {
                if (isNextPage) {
                    _state.value = BookUiState.Error("Error de búsqueda")
                }            }
        }
    }
}
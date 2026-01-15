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

    // ESTA ES LA FUNCIÓN QUE PREGUNTABAS
    fun onQueryChanged(query: String) {
        searchJob?.cancel() // Cancelamos el temporizador anterior

        if (query.length < 3) return // No buscamos si hay menos de 3 letras

        searchJob = viewModelScope.launch {
            delay(600) // Esperamos a que el usuario deje de escribir
            searchBooks(query) // Llamamos a la función que ya teníamos
        }
    }
    init {
        // Podemos hacer una búsqueda inicial por defecto
        searchBooks("the lord of the rings")
    }


    fun searchBooks(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _state.value = BookUiState.Loading
            try {
                // 1. Limpiamos y separamos las palabras
                val words = query.trim().lowercase().split("\\s+".toRegex())

                // 2. Creamos la "Query Flexible":
                // Ponemos ~1 a cada palabra (permite 1 error por palabra)
                val flexibleQuery = words.joinToString(" ") { "$it~1" }

                // 3. Llamamos a la API con el mode=everything
                var books = repository.searchBooks(query) // Intento 1: Exacto

                if (books.isEmpty()) {
                    // Intento 2: Si falló, aplicamos Fuzzy
                    val fuzzyQuery = query.split(" ").joinToString(" ") { "$it~1" }
                    books = repository.searchBooks(fuzzyQuery)
                }

                _state.value = BookUiState.Success(books.distinctBy { it.title })
            } catch (e: Exception) {
                _state.value = BookUiState.Error("Error de red")
            }
        }
    }
}
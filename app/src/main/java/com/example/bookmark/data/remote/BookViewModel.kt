package com.example.bookmark.data.remote

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmark.data.remote.dto.Book
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

    init {
        // Podemos hacer una búsqueda inicial por defecto
        searchBooks("the lord of the rings")
    }

    fun searchBooks(query: String) {
        viewModelScope.launch {
            _state.value = BookUiState.Loading
            try {
                // El repositorio ahora devuelve la lista de objetos Book
                val books = repository.searchBooks(query)
                _state.value = BookUiState.Success(books)
            } catch (e: Exception) {
                _state.value = BookUiState.Error("Error al cargar libros: ${e.localizedMessage}")
            }
        }
    }
}
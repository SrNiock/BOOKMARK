package com.example.bookmark.data.remote

import com.example.bookmark.data.remote.RetrofitInstance
import com.example.bookmark.data.remote.RetrofitInstance.api
import com.example.bookmark.data.remote.dto.Book
import com.example.bookmark.data.remote.dto.BookSearchResponse


class BookRepository {
    private val api = RetrofitInstance.api

    suspend fun searchBooks(query: String): List<Book> {
        val response = api.searchBooks(query)
        // Devolvemos solo la lista de libros "docs" que está dentro del objeto de respuesta
        return response.books
    }
}
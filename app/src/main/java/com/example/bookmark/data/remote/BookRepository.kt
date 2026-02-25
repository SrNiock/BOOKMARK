package com.example.bookmark.data.remote

import com.example.bookmark.data.remote.dto.Book


class BookRepository {
    private val api = RetrofitInstance.api

    suspend fun searchBooks(query: String, currentPage: Int): List<Book> {
        val response = api.searchBooks(query, page = currentPage)

        return response.books
    }
}
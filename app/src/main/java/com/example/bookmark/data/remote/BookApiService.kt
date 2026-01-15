package com.example.bookmark.data.remote

import com.example.bookmark.data.remote.dto.BookSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BookApiService{
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("mode") mode: String = "everything", // <--- El truco está aquí
        @Query("page") page: Int = 1, // <--- Solo añade esto con valor por defecto 1
        @Query("fields") fields: String = "key,title,author_name,cover_i,first_publish_year",
        @Query("limit") limit: Int = 20 //Hacer pruebas de velocidad
    ) : BookSearchResponse
}
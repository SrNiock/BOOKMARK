package com.example.bookmark.data.remote.dto
import com.google.gson.annotations.SerializedName

//Objeto principal de la respuesta
data class BookSearchResponse(
    val numFound: Int,
    val start: Int,
    @SerializedName("docs")
    val books: List<Book>
)

//Objeto que representa cada libro
data class Book(
    val key: String,

    val title: String,

    @SerializedName("author_name")
    val authorNames: List<String>?,

    @SerializedName("first_publish_year")
    val firstPublishYear: Int?,

    @SerializedName("cover_i")
    val coverId: Int?,

    val language: List<String>?,

    @SerializedName("number_of_pages_median")
    val numeroPaginas: Int? = null
)
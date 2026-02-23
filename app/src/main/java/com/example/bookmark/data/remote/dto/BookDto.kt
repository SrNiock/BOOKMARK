package com.example.bookmark.data.remote.dto
import com.google.gson.annotations.SerializedName

// Objeto principal de la respuesta
data class BookSearchResponse(
    val numFound: Int,
    val start: Int,
    @SerializedName("docs")
    val books: List<Book> // Mapeamos el array "docs" a nuestra lista de libros
)

// Objeto que representa cada libro
data class Book(
    val key: String, // ID único del libro (ej: "/works/OL27448W")

    val title: String,

    @SerializedName("author_name")
    val authorNames: List<String>?, // Los autores vienen en una lista

    @SerializedName("first_publish_year")
    val firstPublishYear: Int?,

    @SerializedName("cover_i")
    val coverId: Int?, // ID numérico para construir la URL de la imagen

    val language: List<String>?
)
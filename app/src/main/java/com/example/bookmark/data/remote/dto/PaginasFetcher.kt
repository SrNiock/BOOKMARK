package com.example.bookmark.data.remote.dto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URL
import java.net.URLEncoder

// ─────────────────────────────────────────────
// DTOs Open Library
// ─────────────────────────────────────────────

@Serializable
private data class OLEditionsResponse(
    @SerialName("entries") val entries: List<OLEdition>? = null
)

@Serializable
private data class OLEdition(
    @SerialName("number_of_pages") val numberOfPages: Int? = null
)

@Serializable
private data class OLSearchResponse(
    @SerialName("docs") val docs: List<OLDoc>? = null
)

@Serializable
private data class OLDoc(
    @SerialName("key") val key: String? = null,
    @SerialName("number_of_pages_median") val numberOfPagesMedian: Int? = null
)

// ─────────────────────────────────────────────
// DTOs Google Books (fallback)
// ─────────────────────────────────────────────

@Serializable
private data class GoogleBooksResponse(
    @SerialName("items") val items: List<GoogleBookItem>? = null
)

@Serializable
private data class GoogleBookItem(
    @SerialName("volumeInfo") val volumeInfo: VolumeInfo
)

@Serializable
private data class VolumeInfo(
    @SerialName("pageCount") val pageCount: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("authors") val authors: List<String>? = null
)

// ─────────────────────────────────────────────

private val json = Json { ignoreUnknownKeys = true }

/**
 * Obtiene el número de páginas con 3 estrategias en cascada:
 *   1. Open Library Works API por bookKey  (más fiable, usa el libro exacto)
 *   2. Open Library Search API por título  (si no hay bookKey o falla)
 *   3. Google Books API                    (último recurso)
 *
 * @param titulo   Título del libro
 * @param autor    Autor (mejora precisión en estrategias 2 y 3)
 * @param bookKey  Clave Open Library, ej: "/works/OL27448W" (opcional pero muy recomendado)
 */
suspend fun obtenerNumeroPaginasGoogleBooks(
    titulo: String,
    autor: String? = null,
    bookKey: String? = null
): Int? = withContext(Dispatchers.IO) {

    // ── Estrategia 1: Open Library Works API via bookKey ──────────────────
    if (!bookKey.isNullOrBlank()) {
        try {
            // Normalizamos: "/works/OL27448W" → "OL27448W"
            val workId = bookKey.trimStart('/').removePrefix("works/")
            val url = "https://openlibrary.org/works/$workId/editions.json?limit=20&fields=number_of_pages"
            val raw  = URL(url).readText()
            val resp = json.decodeFromString<OLEditionsResponse>(raw)

            val paginas = resp.entries
                ?.mapNotNull { it.numberOfPages }
                ?.filter { it in 50..2000 }
                // Usamos la mediana para ignorar ediciones raras (condensadas, ilustradas, etc.)
                ?.sorted()
                ?.let { list -> list[list.size / 2] }

            if (paginas != null) return@withContext paginas
        } catch (_: Exception) { /* siguiente estrategia */ }
    }

    // ── Estrategia 2: Open Library Search API por título ──────────────────
    try {
        val queryParts = mutableListOf("title=${URLEncoder.encode(titulo.trim(), "UTF-8")}")
        if (!autor.isNullOrBlank()) {
            val apellido = autor.trim().split(" ").last()
            queryParts.add("author=${URLEncoder.encode(apellido, "UTF-8")}")
        }
        val url = "https://openlibrary.org/search.json?${queryParts.joinToString("&")}&fields=key,number_of_pages_median&limit=5"
        val raw  = URL(url).readText()
        val resp = json.decodeFromString<OLSearchResponse>(raw)

        val paginas = resp.docs
            ?.mapNotNull { it.numberOfPagesMedian }
            ?.firstOrNull { it in 50..2000 }

        if (paginas != null) return@withContext paginas
    } catch (_: Exception) { /* siguiente estrategia */ }

    // ── Estrategia 3: Google Books API (último recurso) ───────────────────
    try {
        val tituloLimpio = titulo.trim()
        val apellido     = autor?.trim()?.split(" ")?.last()?.takeIf { it.length > 2 }

        val queries = listOfNotNull(
            if (apellido != null) "intitle:${tituloLimpio}+inauthor:$apellido" else null,
            "intitle:${tituloLimpio}"
        )

        for (query in queries) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/books/v1/volumes" +
                    "?q=$encoded&maxResults=10&fields=items(volumeInfo(title,authors,pageCount))"
            val raw  = URL(url).readText()
            val resp = json.decodeFromString<GoogleBooksResponse>(raw)

            val tituloNorm = tituloLimpio.lowercase()
            val paginas = resp.items
                ?.filter { (it.volumeInfo.pageCount ?: 0) in 50..2000 }
                ?.maxByOrNull { item ->
                    val vTitle = item.volumeInfo.title?.lowercase() ?: ""
                    when {
                        vTitle == tituloNorm            -> 100
                        vTitle.contains(tituloNorm)     -> 60
                        tituloNorm.contains(vTitle)     -> 40
                        else                            -> 0
                    }
                }
                ?.volumeInfo?.pageCount

            if (paginas != null) return@withContext paginas
        }
    } catch (_: Exception) { /* sin resultado */ }

    null
}
package com.example.bookmark.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable data object Login : Screen // <--- NUEVA RUTA DE LOGIN
    @Serializable data object Register : Screen
    @Serializable data object Books : Screen
    @Serializable data object Search : Screen
    @Serializable data object Library : Screen
    @Serializable data object Profile : Screen
    @Serializable data class BookDetail(val bookKey: String) : Screen // <--- Nueva ruta con parámetro
    @Serializable data class ExternalProfile(val userId: Long)
}


data class TopLevelRoute<T :Any>(
    val name:String,
    val route: T,
    val icon : ImageVector
)

val bottomNavItems = listOf(
    TopLevelRoute("Libros", Screen.Books, Icons.Default.Book),
    TopLevelRoute("Buscar", Screen.Search, Icons.Default.Search),
    TopLevelRoute("Biblioteca", Screen.Library, Icons.Default.LibraryAdd),
    TopLevelRoute("Perfil", Screen.Profile, Icons.Default.Person)

)
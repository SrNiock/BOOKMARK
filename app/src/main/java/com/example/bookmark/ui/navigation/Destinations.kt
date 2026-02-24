package com.example.bookmark.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
// Importamos los iconos Rellenos (Filled)
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryBooks // <-- Sugerencia
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoStories
// Importamos los iconos Contorneados (Outlined)
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.LibraryBooks // <-- Sugerencia
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable data object Login : Screen
    @Serializable data object Register : Screen
    @Serializable data object Books : Screen
    @Serializable data object Search : Screen
    @Serializable data object Library : Screen
    @Serializable data object Profile : Screen
    @Serializable data class BookDetail(val bookKey: String) : Screen
    @Serializable data class ExternalProfile(val userId: Long)
}

// 1. Modificamos TopLevelRoute para aceptar dos iconos
data class TopLevelRoute<T :Any>(
    val name: String,
    val route: T,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// 2. Actualizamos la lista con las versiones Filled y Outlined
val bottomNavItems = listOf(
    TopLevelRoute(
        name = "Libros",
        route = Screen.Books,
        selectedIcon = Icons.Filled.AutoStories,
        unselectedIcon = Icons.Outlined.AutoStories
    ),
    TopLevelRoute(
        name = "Buscar",
        route = Screen.Search,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    TopLevelRoute(
        name = "Biblioteca",
        route = Screen.Library,
        selectedIcon = Icons.Filled.Bookmarks,
        unselectedIcon = Icons.Outlined.Bookmarks
    ),
    TopLevelRoute(
        name = "Perfil",
        route = Screen.Profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)
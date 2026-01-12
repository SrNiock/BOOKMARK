package com.example.bookmark.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.bookmark.ui.screens.BooksScreen
import com.example.bookmark.ui.screens.SearchScreen
import com.example.bookmark.ui.screens.UserScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // El NavHost es el contenedor que intercambia las pantallas
    NavHost(
        navController = navController,
        startDestination = Screen.Books, // Definimos la pantalla de inicio
        modifier = modifier
    ) {

        // --- PANTALLA DE LIBROS ---
        composable<Screen.Books> {
            BooksScreen()
        }

        // --- PANTALLA DE BÚSQUEDA ---
        composable<Screen.Search> {
            SearchScreen()
        }

        // --- PANTALLA DE PERFIL ---
        composable<Screen.Profile> {
            UserScreen()
        }
    }
}
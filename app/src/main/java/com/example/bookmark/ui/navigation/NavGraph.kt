package com.example.bookmark.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.ui.screens.BooksScreen
import com.example.bookmark.ui.screens.LoginScreen // <-- Asegúrate de importar esto
import com.example.bookmark.ui.screens.SearchScreen
import com.example.bookmark.ui.screens.UserScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    bookViewModel: BookViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login, // <--- 1. AHORA EMPIEZA EN LOGIN
        modifier = modifier
    ) {

        // --- PANTALLA DE LOGIN ---
        composable<Screen.Login> {
            LoginScreen(
                onLoginSuccess = {
                    // Navegamos a la pantalla principal y destruimos el Login del historial
                    navController.navigate(Screen.Books) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // Más adelante navegaremos a Screen.Register
                }
            )
        }

        // --- PANTALLA DE LIBROS ---
        composable<Screen.Books> {
            BooksScreen(bookViewModel)
        }

        // --- PANTALLA DE BÚSQUEDA ---
        composable<Screen.Search> {
            SearchScreen(bookViewModel)
        }

        // --- PANTALLA DE PERFIL ---
        composable<Screen.Profile> {
            UserScreen()
        }
    }
}
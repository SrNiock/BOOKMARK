package com.example.bookmark.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.ui.screens.BibliotecaScreen
import com.example.bookmark.ui.screens.BookDetailScreen
import com.example.bookmark.ui.screens.BooksScreen
import com.example.bookmark.ui.screens.LoginScreen
import com.example.bookmark.ui.screens.RegisterScreen
import com.example.bookmark.ui.screens.SearchScreen
import com.example.bookmark.ui.screens.UserScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    bookViewModel: BookViewModel,
    isLoggedIn: Boolean
) {
    // Determina si empezamos en el Login o en la pantalla principal de Libros
    val startDest = if (isLoggedIn) Screen.Books else Screen.Login

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier
    ) {
        // --- RUTA: LOGIN ---
        composable<Screen.Login> {
            LoginScreen(
                onLoginSuccess = {
                    // Al entrar con éxito, vamos a Books y borramos el Login del historial
                    navController.navigate(Screen.Books) { popUpTo(Screen.Login) { inclusive = true } }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register) }
            )
        }

        // --- RUTA: REGISTRO ---
        composable<Screen.Register> {
            RegisterScreen(
                onRegisterSuccess = {
                    // Al registrarse, vamos directo a la app principal
                    navController.navigate(Screen.Books) { popUpTo(Screen.Login) { inclusive = true } }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- RUTA: HOME / BOOKS (Feed principal) ---
        composable<Screen.Books> {
            BooksScreen(
                viewModel = bookViewModel,
                onLogout = {
                    // Al cerrar sesión, volvemos al Login y limpiamos toda la pila de navegación
                    navController.navigate(Screen.Login) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // --- RUTA: BUSCADOR ---
        composable<Screen.Search> {
            // Pasamos el navController para que al clicar un resultado vaya a detalles
            SearchScreen(bookViewModel, navController)
        }

        // --- RUTA: BIBLIOTECA (Tus libros guardados) ---
        composable<Screen.Library> {
            // ¡CAMBIO CLAVE!: Ahora le pasamos el navController para que los clics funcionen
            BibliotecaScreen(navController = navController)
        }

        // --- RUTA: PERFIL DE USUARIO ---
        composable<Screen.Profile> {
            UserScreen()
        }

        // --- RUTA: DETALLES DEL LIBRO ---
        composable<Screen.BookDetail> { backStackEntry ->
            // Extraemos la información de la ruta (incluyendo el bookKey)
            val detail: Screen.BookDetail = backStackEntry.toRoute()

            BookDetailScreen(
                bookKey = detail.bookKey, // Usamos la key para cargar el libro específico
                viewModel = bookViewModel,
                onBack = { navController.popBackStack() } // Botón para volver atrás
            )
        }
    }
}
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
    val startDest = if (isLoggedIn) Screen.Books else Screen.Login

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = modifier
    ) {
        composable<Screen.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Books) { popUpTo(Screen.Login) { inclusive = true } }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register) }
            )
        }

        composable<Screen.Register> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Books) { popUpTo(Screen.Login) { inclusive = true } }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Books> {
            BooksScreen(
                viewModel = bookViewModel,
                onLogout = {
                    navController.navigate(Screen.Login) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // --- SOLUCIÓN ERROR SEARCH ---
        composable<Screen.Search> {
            SearchScreen(bookViewModel, navController) // <-- Solo le faltaba la palabra navController
        }

        composable<Screen.Library> {
            BibliotecaScreen()
        }

        composable<Screen.Profile> {
            UserScreen()
        }

        // --- NUEVA RUTA PARA DETALLES ---
        composable<Screen.BookDetail> { backStackEntry ->
            val detail: Screen.BookDetail = backStackEntry.toRoute()
            BookDetailScreen(
                bookKey = detail.bookKey,
                viewModel = bookViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
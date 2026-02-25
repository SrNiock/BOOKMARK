package com.example.bookmark.ui.navigation

import android.net.Uri
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
import com.example.bookmark.ui.screens.ExternalUserScreen
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
        // LOGIN
        composable<Screen.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Books) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register) }
            )
        }

        // REGISTRO
        composable<Screen.Register> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Books) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // HOME
        composable<Screen.Books> {
            BooksScreen(
                viewModel = bookViewModel,
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.Login) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // RUTA
        composable<Screen.Search> {
            SearchScreen(bookViewModel, navController)
        }

        // BIBLIOTECA
        composable<Screen.Library> {
            BibliotecaScreen(navController = navController)
        }

        // PERFIL DE USUARIO
        composable<Screen.Profile> {
            UserScreen()
        }

        // DETALLES DEL LIBRO
        composable<Screen.BookDetail> { backStackEntry ->
            val detail: Screen.BookDetail = backStackEntry.toRoute()
            val keyLimpia = Uri.decode(detail.bookKey)

            BookDetailScreen(
                bookKey = keyLimpia,
                viewModel = bookViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // PERFIL EXTERNO
        composable<Screen.ExternalProfile> { backStackEntry ->
            val externalProfile: Screen.ExternalProfile = backStackEntry.toRoute()
            ExternalUserScreen(
                userId = externalProfile.userId,
                navController = navController
            )
        }
    }
}
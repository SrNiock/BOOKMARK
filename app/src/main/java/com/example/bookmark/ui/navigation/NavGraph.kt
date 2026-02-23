package com.example.bookmark.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.bookmark.data.remote.BookViewModel
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
    // MAGIA AQUÍ: Decide dónde arrancar basado en si hay sesión
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
                onNavigateToRegister = {
                    navController.navigate(Screen.Register)
                }
            )
        }

        composable<Screen.Register> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Books) { popUpTo(Screen.Login) { inclusive = true } }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.Books> {
            BooksScreen(bookViewModel)
        }

        composable<Screen.Search> {
            SearchScreen(bookViewModel)
        }

        composable<Screen.Profile> {
            UserScreen()
        }
    }
}
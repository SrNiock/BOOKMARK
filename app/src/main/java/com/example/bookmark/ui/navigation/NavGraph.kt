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
    bookViewModel: BookViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login, // <-- Vuelve a arrancar aquí por defecto
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
            BooksScreen(bookViewModel) // o BooksScreen(bookViewModel)
        }

        composable<Screen.Search> {
            SearchScreen(bookViewModel)
        }

        composable<Screen.Profile> {
            UserScreen()
        }
    }
}
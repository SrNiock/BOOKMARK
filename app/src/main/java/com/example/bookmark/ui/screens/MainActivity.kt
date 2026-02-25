package com.example.bookmark.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.ui.navigation.NavGraph
import com.example.bookmark.ui.navigation.Screen
import com.example.bookmark.ui.navigation.bottomNavItems
import com.example.bookmark.ui.theme.BOOKMARKTheme
import com.example.bookmark.ui.utils.SessionManager // <-- Importamos tu bloc de notas

class MainActivity : ComponentActivity() {

    private val bookViewModel: BookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BOOKMARKTheme {
                // 1. Leemos el SessionManager al abrir la app
                val context = LocalContext.current
                val sessionManager = remember { SessionManager(context) }

                // 2. Comprobamos si hay un correo guardado
                val isLoggedIn = sessionManager.obtenerCorreoSesion() != null

                // 3. Se lo pasamos al andamio principal
                CustomScaffold(bookViewModel, isLoggedIn)
            }
        }
    }
}

@Composable
fun CustomScaffold(bookViewModel: BookViewModel, isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.hasRoute(Screen.Login::class) == false &&
            currentDestination?.hasRoute(Screen.Register::class) == false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface, // Fondo limpio
                    tonalElevation = 0.dp // Quitamos las sombras pesadas
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.hasRoute(item.route::class)
                        } == true

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.name,
                                    // Hacemos el icono un poco más grande al no haber texto
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            // 1. Quitamos el texto completamente
                            label = null,
                            alwaysShowLabel = false,

                            // 2. Quitamos la píldora y ajustamos los colores para máximo minimalismo
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent, // Adiós a la píldora de fondo
                                selectedIconColor = MaterialTheme.colorScheme.primary, // Color vibrante al seleccionar
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) // Gris apagado sin seleccionar
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            bookViewModel = bookViewModel,
            isLoggedIn = isLoggedIn
        )
    }
}
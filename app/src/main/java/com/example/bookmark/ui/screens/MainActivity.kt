package com.example.bookmark.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bookmark.data.remote.BookViewModel
import com.example.bookmark.ui.navigation.NavGraph
import com.example.bookmark.ui.navigation.bottomNavItems
import com.example.bookmark.ui.theme.BOOKMARKTheme

class MainActivity : ComponentActivity() {

    private val bookViewModel: BookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BOOKMARKTheme {
               CustomScaffold(bookViewModel)
            }
        }
    }
}


@Composable
fun CustomScaffold( bookViewModel: BookViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
           NavigationBar{
               bottomNavItems.forEach{item ->
                   val isSelected = currentDestination?.hierarchy?.any{
                       it.hasRoute(item.route::class)
                   } == true

                   NavigationBarItem(
                       selected = isSelected,
                       onClick = {
                           navController.navigate(item.route){
                               popUpTo(navController.graph.findStartDestination().id){
                                   saveState = true
                               }
                               launchSingleTop=true
                               restoreState = true
                           }
                       },
                       icon = {Icon(item.icon, contentDescription = item.name)},
                       label = {Text(item.name)}
                   )


               }

           }
        }


    ) { innerPadding ->
        NavGraph(navController = navController,
            modifier = Modifier.padding(innerPadding),bookViewModel
        )
    }
}





@Composable
fun Greeting(innerPadding: PaddingValues) {
    Text(
        text = "Hello name!",
        modifier = Modifier.padding(innerPadding)
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BOOKMARKTheme {
    }
}
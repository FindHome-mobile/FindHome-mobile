package com.example.devmobile;

class LegacyComposeMain {
    // Intentionally left blank to avoid conflicts with Kotlin MainActivity
}
/*
package com.example.devmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
fun AppRoot() {
    val navController: NavHostController = rememberNavController()
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") { HomeScreen(onNavigate = { navController.navigate("details") }) }
                composable("details") { DetailsScreen(onBack = { navController.popBackStack() }) }
            }
        }
    }
}

@Composable
fun HomeScreen(onNavigate: () -> Unit) {
Text(text = "Welcome to FinHome")
}

@Composable
fun DetailsScreen(onBack: () -> Unit) {
Text(text = "Details Screen")
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    HomeScreen(onNavigate = {})
}
*/

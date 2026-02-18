package com.example.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.application.bottom_navigation.BottomNavigation
import com.example.application.reg_auth.AuthorizationScreen
import com.example.application.reg_auth.RegistrationScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Main() }
    }
}

@Composable
fun Main ( ) {
    navigation()
}

@Composable
fun navigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf("home", "plus", "statistics")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigation(
                    navController = navController,
                    onAddClick = {}
                )
            }
        }

    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            NavHost(navController = navController, startDestination = "authorization") {
                composable("authorization") {
                    AuthorizationScreen(
                        onNavigateToRegistration = {
                            navController.navigate("registration")
                        },
                        onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("authorization") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("registration") {
                    RegistrationScreen(
                        onNavigateToLogin = {
                            navController.popBackStack()
                        },
                        onRegisterSuccess = {
                            navController.navigate("authorization") {
                                popUpTo("registration") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("home") {
                    Home(
                        onLogout = {
                            navController.navigate("authorization") {
                                popUpTo(0)
                            }
                        }
                    )
                }
                composable("plus") {
                    Plus()
                }
                composable("statistics") {
                    Statistics()
                }
            }
        }
    }
}

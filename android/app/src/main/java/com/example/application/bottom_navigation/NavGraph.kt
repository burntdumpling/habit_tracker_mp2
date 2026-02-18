package com.example.application.bottom_navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.application.Home
import com.example.application.Plus
import com.example.application.Statistics

@Composable
fun NavGraph (
    navHostController: NavHostController,
    onLogout: () -> Unit
) {
    NavHost(navHostController, startDestination = "home") {
        composable("home") {
            Home(onLogout = onLogout)
        }
        composable("plus") {
            Plus()
        }
        composable("statistics") {
            Statistics()
        }
    }
}
package com.example.application.bottom_navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.application.Screen1
import com.example.application.Screen2
import com.example.application.Screen3
import com.example.application.Screen4

@Composable
fun NavGraph (
    navHostController: NavHostController
) {
    NavHost(navHostController, startDestination = "screen_1") {
        composable("screen_1") {
            Screen1()
        }
        composable("screen_2") {
            Screen2()
        }
        composable("screen_3") {
            Screen3()
        }
        composable("screen_4") {
            Screen4()
        }
    }
}
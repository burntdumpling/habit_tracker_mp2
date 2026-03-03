package com.example.application.bottom_navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.application.Home
import com.example.application.plus.Plus
import com.example.application.Statistics
import com.example.application.edit.EditHabitScreen

@Composable
fun NavGraph (
    navHostController: NavHostController,
    onLogout: () -> Unit,
    onAddHabit: () -> Unit,
    onHabitCreated: () -> Unit
) {
    NavHost(navHostController, startDestination = "home") {
        composable("home") {
            Home(
                onLogout = onLogout, 
                onAddHabit = onAddHabit,
                onEditHabit = { id -> 
                    navHostController.navigate("edit/$id")
                }
            )
        }
        composable(
            route = "edit/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: return@composable
            EditHabitScreen(habitId = habitId, onBack = { navHostController.popBackStack() })
        }
        composable(
            route = "plus?habitId={habitId}",
            arguments = listOf(
                navArgument("habitId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: -1L
            Plus(
                habitIdToEdit = if (habitId != -1L) habitId else null,
                onHabitCreated = onHabitCreated
            )
        }
        composable("statistics") {
            Statistics()
        }
    }
}

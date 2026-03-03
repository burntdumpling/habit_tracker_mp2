package com.example.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.application.bottom_navigation.BottomNavigation
import com.example.application.reg_auth.AuthorizationScreen
import com.example.application.reg_auth.RegistrationScreen
import com.example.application.data.session.AuthState
import com.example.application.plus.Plus
import com.example.application.edit.EditHabitScreen
import android.content.Context
import android.content.res.Configuration
import java.util.Locale


class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("ru", "RU")
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Оставляем и AppCompat-локаль тоже (не мешает, полезно для других компонентов)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags("ru-RU")
        )

        super.onCreate(savedInstanceState)

        setContent { Main() }
    }
}


@Composable
fun Main ( ) {
    navigation()
}

@Composable
fun navigation() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as MyApp).appContainer
    val sessionState by appContainer.sessionManager.state.collectAsState()

    LaunchedEffect(Unit) {
        appContainer.authRepository.bootstrap()
    }

    if (sessionState is AuthState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDest = if (sessionState is AuthState.Authenticated) "home" else "authorization"

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
            NavHost(navController = navController, startDestination = startDest) {
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
                        },
                        onAddHabit = {
                            navController.navigate("plus")
                        },
                        onEditHabit = { id -> 
                            navController.navigate("edit/$id")
                        }
                    )
                }
                composable(
                    route = "edit/{habitId}",
                    arguments = listOf(navArgument("habitId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val habitId = backStackEntry.arguments?.getLong("habitId") ?: return@composable
                    EditHabitScreen(habitId = habitId, onBack = { navController.popBackStack() })
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
                        onHabitCreated = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
                composable("statistics") {
                    Statistics()
                }
            }
        }
    }
}

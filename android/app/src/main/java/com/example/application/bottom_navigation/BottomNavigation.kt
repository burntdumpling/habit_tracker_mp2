package com.example.application.bottom_navigation

import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.application.R

@Composable
fun BottomNavigation(
    navController: NavController,
    onAddClick: () -> Unit
) {
    val items = listOf(
        BottomItem.Screen1,
        BottomItem.Screen2,
        null, // центр: плюсик
        BottomItem.Screen3,
        BottomItem.Screen4
    )

    NavigationBar(
        modifier = Modifier.background(Color.White)
    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        items.forEach { itemOrNull ->
            if (itemOrNull == null) {
                // Центральная кнопка "+"
                NavigationBarItem(
                    selected = false,
                    onClick = onAddClick,
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.plus),
                            contentDescription = "Add"
                        )
                    },
                    label = null,
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = Color.Black,
                        selectedTextColor = Color.Black,
                        unselectedTextColor = Color.Gray
                    )
                )
            } else {
                val item = itemOrNull
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            launchSingleTop = true
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = item.iconId),
                            contentDescription = item.title
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 9.sp
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = Color.Black,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    }
}

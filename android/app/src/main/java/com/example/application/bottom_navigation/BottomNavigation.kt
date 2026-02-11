package com.example.application.bottom_navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigation(
    navController: NavController
) {
    val listItems = listOf (
        BottomItem.Home,
        BottomItem.Plus,
        BottomItem.Statistics
    )

    NavigationBar (
        modifier = Modifier
            .fillMaxWidth().
            height(height = 80.dp),

    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRout = backStackEntry?.destination?.route
        listItems.forEach {
            item-> NavigationBarItem(
                selected = currentRout == item.route,

                onClick = {
                    navController.navigate(item.route)
                },

                icon = {
                    Icon(
                        painter = painterResource(id = item.iconId),
                        contentDescription = "Icon",
                        modifier = Modifier.size(32.dp)
                    )
                },


                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.DarkGray,
                    unselectedIconColor = Color.LightGray,
                    selectedTextColor = Color.Black,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
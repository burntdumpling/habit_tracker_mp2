package com.example.application.bottom_navigation

import com.example.application.R

sealed class BottomItem (val title: String, val iconId: Int, val route: String) {
    // title - отвечает за название объекта
    // iconId - индификатоор картинки
    // route - название экрана, точнее его путь виде строки
    object  Home: BottomItem("Home", R.drawable.home, "home")
    object  Plus: BottomItem("Plus", R.drawable.plus, "plus")
    object  Statistics: BottomItem("Statistics", R.drawable.statistics, "statistics")
}

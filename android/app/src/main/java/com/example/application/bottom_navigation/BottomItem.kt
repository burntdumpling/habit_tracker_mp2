package com.example.application.bottom_navigation

import com.example.application.R


sealed class BottomItem (val title: String, val iconId: Int, val route: String) {
    // title - отвечает за название объекта
    // iconId - индификатоор картинки
    // route - название экрана, точнее его путь виде строки

    object  Screen1: BottomItem("ЖУРНАЛ", R.drawable.icon_screen1, "screen_1")
    object  Screen2: BottomItem("ПРОГРЕСС", R.drawable.icon_screen2, "screen_2")
    object  Screen3: BottomItem("СТАТИСТИКА", R.drawable.icon_screen3, "screen_3")
    object  Screen4: BottomItem("НАСТРОЙКИ", R.drawable.icon_screen4, "screen_4")
}

package com.hyperosunfcker.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object HyperOS : Screen("hyperos")
    data object Logs : Screen("logs")
    data object Settings : Screen("settings")
    data object Presets : Screen("presets")
}

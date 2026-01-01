package com.example.garmin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.garmin.ui.screen.main.MainScreen
import com.example.garmin.ui.screen.setting.SettingScreen

var startDestination = Graph.HOME

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        route = Graph.ROOT,
        startDestination = startDestination
    ) {
        authNavGraph(navController = navController)
    }

}

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    navigation(
        route = Graph.HOME,
        startDestination = HomeRoute.Main.route
    ) {

        composable(route = HomeRoute.Main.route) {
            MainScreen(navController)
        }

        composable(route = HomeRoute.Setting.route) {
            SettingScreen(navController)
        }
    }
}

sealed class HomeRoute(val route: String) {
    object Main : HomeRoute(route = "Main")
    object Setting : HomeRoute(route = "AuthSimple")
}

object Graph {
    const val ROOT = "root_graph"
    const val HOME = "home_graph"

}
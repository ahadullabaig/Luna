package com.luna.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.luna.app.feature.calendar.CalendarScreen
import com.luna.app.feature.home.HomeScreen

@Composable
fun LunaNavHost() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            HomeScreen()
        }
        composable<CalendarRoute> {
            CalendarScreen()
        }
    }
}

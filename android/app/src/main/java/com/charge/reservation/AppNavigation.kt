package com.charge.reservation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


//use navcontroller to navigate to different ui displays (similar to how lab 7 works)
//this is the only function called in MainActivity
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {HomeScreen(navController)} //home screen ui
    }
}
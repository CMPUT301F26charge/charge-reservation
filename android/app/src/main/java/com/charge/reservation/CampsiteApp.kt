package com.charge.reservation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


//use navcontroller to navigate to different ui displays (similar to how lab 7 works)
//this is the only function called in MainActivity
@Composable
fun CampsiteApp() {
    val navcontroller = rememberNavController()

    NavHost(navController = navcontroller, startDestination = "home") {
        composable("home") {HomeScreen(navcontroller)} //home screen ui
    }
}
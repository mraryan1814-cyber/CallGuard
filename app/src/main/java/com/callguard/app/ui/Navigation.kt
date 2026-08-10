package com.callguard.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.callguard.app.data.Prefs

object Routes {
    const val SET_PIN = "set_pin"
    const val ENTER_PIN = "enter_pin"
    const val FORGOT_PIN = "forgot_pin"
    const val HOME = "home"
    const val NUMBERS = "numbers"
    const val WHITELIST = "whitelist"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@Composable
fun CallGuardNavHost(prefs: Prefs) {
    val navController: NavHostController = rememberNavController()
    val pinSet by prefs.isPinSet.collectAsState(initial = null)

    // Wait for the initial value before deciding start destination
    if (pinSet == null) return

    val startDestination = if (pinSet == true) Routes.ENTER_PIN else Routes.SET_PIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SET_PIN) {
            SetPinScreen(prefs) {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SET_PIN) { inclusive = true }
                }
            }
        }
        composable(Routes.ENTER_PIN) {
            EnterPinScreen(
                prefs = prefs,
                onUnlocked = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ENTER_PIN) { inclusive = true }
                    }
                },
                onForgotPin = { navController.navigate(Routes.FORGOT_PIN) }
            )
        }
        composable(Routes.FORGOT_PIN) {
            ForgotPinScreen(
                prefs = prefs,
                onReset = {
                    navController.navigate(Routes.SET_PIN) {
                        popUpTo(Routes.FORGOT_PIN) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                prefs = prefs,
                onOpenNumbers = { navController.navigate(Routes.NUMBERS) },
                onOpenWhitelist = { navController.navigate(Routes.WHITELIST) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.NUMBERS) {
            NumbersScreen(prefs) { navController.popBackStack() }
        }
        composable(Routes.WHITELIST) {
            WhitelistScreen(prefs) { navController.popBackStack() }
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(prefs) { navController.popBackStack() }
        }
        composable(Routes.HISTORY) {
            HistoryScreen { navController.popBackStack() }
        }
    }
}

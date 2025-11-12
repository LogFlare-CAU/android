package com.example.logflare_android

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.logflare_android.ui.screens.AuthScreen
import com.example.logflare_android.ui.screens.HomeScaffold
import com.example.logflare_android.ui.theme.LogflareandroidTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.logflare_android.ui.navigation.Route
import com.example.logflare_android.viewmodel.AppViewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // no-op; optional: show rationale or toast
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            LogflareandroidTheme {
                App()
            }
        }
    }
}

@Composable
fun App(vm: AppViewModel = hiltViewModel()) {
    val token by vm.token.collectAsState()
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route
    val start = if (token.isNullOrBlank()) Route.Auth.path else Route.Home.path

    LaunchedEffect(token) {
        if (token.isNullOrBlank() && currentRoute != Route.Auth.path) {
            navController.navigate(Route.Auth.path) {
                popUpTo(Route.Home.path) { inclusive = true }
                launchSingleTop = true
            }
        } else if (!token.isNullOrBlank() && currentRoute == Route.Auth.path) {
            navController.navigate(Route.Home.path) {
                popUpTo(Route.Auth.path) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    NavHost(navController = navController, startDestination = start) {
        composable(Route.Auth.path) {
            AuthScreen(onAuthed = {
                navController.navigate(Route.Home.path) {
                    popUpTo(Route.Auth.path) { inclusive = true }
                }
            })
        }
        composable(Route.Home.path) { HomeScaffold() }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LogflareandroidTheme {
        HomeScaffold()
    }
}
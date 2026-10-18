package com.logflare.android

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.logflare.android.data.ThemePreference
import com.logflare.android.data.session.SessionState
import com.logflare.android.feature.auth.LoginScreen
import com.logflare.android.feature.main.MainScaffold
import com.logflare.android.ui.VisualQaAppRoot
import com.logflare.android.ui.navigation.Route
import com.logflare.android.ui.theme.LogflareandroidTheme
import com.logflare.android.viewmodel.AppViewModel
import com.logflare.android.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : Hilt_MainActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // no-op; optional: show rationale or toast
        }

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash up until the stored session is read, so the start destination is decided once.
        installSplashScreen().setKeepOnScreenCondition {
            appViewModel.session.value is SessionState.Loading
        }
        super.onCreate(savedInstanceState)
        val projectId = intent.getIntExtra("projectid", -1).takeIf { it != -1 }
        val errorId = intent.getIntExtra("errorid", -1).takeIf { it != -1 }
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
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val preference by themeViewModel.preference.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (preference) {
                ThemePreference.SYSTEM -> systemDark
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            LogflareandroidTheme(darkTheme = darkTheme) {
                VisualQaAppRoot {
                    App(
                        vm = appViewModel,
                        intentProjectId = projectId,
                        intentErrorId = errorId,
                        themeViewModel = themeViewModel,
                    )
                }
            }
        }
    }
}

@Composable
fun App(
    vm: AppViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    intentProjectId: Int? = null,
    intentErrorId: Int? = null
) {
    val session by vm.session.collectAsState()
    // The splash screen covers Loading; build nothing until the stored session is known.
    if (session is SessionState.Loading) return
    val signedIn = session is SessionState.SignedIn
    val navController = rememberNavController()
    // Decided once from the state read off disk — no redirect on first composition.
    val start = remember { if (signedIn) Route.Main.path else Route.Auth.path }

    // Later transitions only (logout, 401 clearing the token, login from another path).
    LaunchedEffect(signedIn) {
        val currentRoute = navController.currentDestination?.route ?: return@LaunchedEffect
        if (!signedIn && currentRoute != Route.Auth.path) {
            navController.navigate(Route.Auth.path) {
                popUpTo(Route.Main.path) { inclusive = true }
                launchSingleTop = true
            }
        } else if (signedIn && currentRoute == Route.Auth.path) {
            navController.navigate(Route.Main.path) {
                popUpTo(Route.Auth.path) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    NavHost(navController = navController, startDestination = start) {
        composable(Route.Auth.path) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Auth.path) { inclusive = true }
                    }
                },
                themeViewModel = themeViewModel,
            )
        }
        composable(Route.Main.path) {
            MainScaffold(
                onLogout = {
                    vm.logout()
                    navController.navigate(Route.Auth.path) {
                        popUpTo(Route.Main.path) { inclusive = true }
                    }
                },
                intentProjectId = intentProjectId,
                intentErrorId = intentErrorId,
                themeViewModel = themeViewModel,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LogflareandroidTheme {
        MainScaffold(onLogout = {})
    }
}

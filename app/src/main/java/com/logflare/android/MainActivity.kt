package com.logflare.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.logflare.android.data.ThemePreference
import com.logflare.android.data.session.SessionState
import com.logflare.android.ui.VisualQaAppRoot
import com.logflare.android.ui.nav.AppNavHost
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

    /** Error id of a tapped push notification, until the nav host has handled it. */
    private var deepLinkErrorId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash up until the stored session is read, so the start destination is decided once.
        installSplashScreen().setKeepOnScreenCondition {
            appViewModel.session.value is SessionState.Loading
        }
        super.onCreate(savedInstanceState)
        // A recreated activity restores its back stack, which already holds the link if it was opened.
        if (savedInstanceState == null) deepLinkErrorId = intent.deepLinkErrorId()
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
            val preference by themeViewModel.preference.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (preference) {
                ThemePreference.SYSTEM -> systemDark
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            // The legacy theme stays inside until the last old screen is gone (Task 12).
            LogFlareTheme(darkTheme = darkTheme) {
                LogflareandroidTheme(darkTheme = darkTheme) {
                    VisualQaAppRoot {
                        App(
                            vm = appViewModel,
                            deepLinkErrorId = deepLinkErrorId,
                            onDeepLinkConsumed = { deepLinkErrorId = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.deepLinkErrorId()?.let { deepLinkErrorId = it }
    }
}

/** The `errorid` extra of a push tap; none when relaunched from Recents (stale extras). */
private fun Intent.deepLinkErrorId(): Int? {
    if (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
    return getIntExtra(EXTRA_ERROR_ID, 0).takeIf { it > 0 }
}

private const val EXTRA_ERROR_ID = "errorid"

@Composable
fun App(
    vm: AppViewModel,
    deepLinkErrorId: Int?,
    onDeepLinkConsumed: () -> Unit,
) {
    val session by vm.session.collectAsStateWithLifecycle()
    // The splash screen covers Loading; build nothing until the stored session is known.
    if (session is SessionState.Loading) return
    AppNavHost(
        session = session,
        deepLinkErrorId = deepLinkErrorId,
        onDeepLinkConsumed = onDeepLinkConsumed,
    )
}

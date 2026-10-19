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
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.logflare.core.designsystem.v2.Lf
import com.example.logflare.core.designsystem.v2.LogFlareTheme
import com.logflare.android.data.ThemePreference
import com.logflare.android.data.session.SessionState
import com.logflare.android.ui.VisualQaAppRoot
import com.logflare.android.ui.nav.AppNavHost
import com.logflare.android.ui.theme.ThemeController
import com.logflare.android.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : Hilt_MainActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // no-op; optional: show rationale or toast
        }

    private val appViewModel: AppViewModel by viewModels()

    /** The theme choice; My › Appearance changes it and it applies at once. */
    @Inject lateinit var themeController: ThemeController

    /** Error id of a tapped push notification, until the nav host has handled it. */
    private var deepLinkErrorId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash up until the stored session is read, so the start destination is decided once.
        installSplashScreen().setKeepOnScreenCondition {
            appViewModel.session.value is SessionState.Loading
        }
        super.onCreate(savedInstanceState)
        deepLinkErrorId = initialDeepLinkErrorId(savedInstanceState, intent)
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
            val preference by themeController.preference.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (preference) {
                ThemePreference.SYSTEM -> systemDark
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            // System bar icons follow the app theme, not the system one.
            SideEffect {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            LogFlareTheme(darkTheme = darkTheme) {
                // Root background and default text colour, as the old theme's root Surface gave.
                CompositionLocalProvider(LocalContentColor provides Lf.colors.textPrimary) {
                    VisualQaAppRoot(Modifier.background(Lf.colors.background)) {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveDeepLinkErrorId(outState, deepLinkErrorId)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.deepLinkErrorId()?.let { deepLinkErrorId = it }
    }
}

/** The `errorid` extra of a push tap; none when relaunched from Recents (stale extras). */
internal fun Intent.deepLinkErrorId(): Int? {
    if (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
    return getIntExtra(EXTRA_ERROR_ID, 0).takeIf { it > 0 }
}

/**
 * A fresh launch reads the push extra. A recreated activity restores its back stack, which already
 * holds the link if it was opened; one not opened yet (e.g. rotated during the splash) comes back
 * from [saveDeepLinkErrorId].
 */
internal fun initialDeepLinkErrorId(savedInstanceState: Bundle?, intent: Intent): Int? =
    if (savedInstanceState == null) {
        intent.deepLinkErrorId()
    } else {
        savedInstanceState.getInt(STATE_DEEP_LINK_ERROR_ID, 0).takeIf { it > 0 }
    }

internal fun saveDeepLinkErrorId(outState: Bundle, errorId: Int?) {
    errorId?.let { outState.putInt(STATE_DEEP_LINK_ERROR_ID, it) }
}

internal const val EXTRA_ERROR_ID = "errorid"
private const val STATE_DEEP_LINK_ERROR_ID = "deepLinkErrorId"

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

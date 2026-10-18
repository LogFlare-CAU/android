package com.logflare.android.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.R

/** The level's display name ("Error"), from strings.xml. */
@Composable
fun levelLabel(level: LfLevel): String = stringResource(
    when (level) {
        LfLevel.Debug -> R.string.level_debug
        LfLevel.Info -> R.string.level_info
        LfLevel.Warning -> R.string.level_warning
        LfLevel.Error -> R.string.level_error
        LfLevel.Critical -> R.string.level_critical
    },
)

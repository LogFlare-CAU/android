package com.logflare.android.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.logflare.android.R
import java.time.Duration
import java.time.Instant

/** How long ago something happened, in the largest whole unit (future times count as now). */
sealed interface Age {
    data object Now : Age
    data class Minutes(val n: Long) : Age
    data class Hours(val n: Long) : Age
    data class Days(val n: Long) : Age
}

fun ageOf(at: Instant, now: Instant): Age {
    val d = Duration.between(at, now)
    return when {
        d.toMinutes() < 1 -> Age.Now
        d.toHours() < 1 -> Age.Minutes(d.toMinutes())
        d.toDays() < 1 -> Age.Hours(d.toHours())
        else -> Age.Days(d.toDays())
    }
}

/** "just now", "3 min ago", "2 h ago", "4 days ago". */
@Composable
fun relativeTime(at: Instant, now: Instant): String = when (val age = ageOf(at, now)) {
    Age.Now -> stringResource(R.string.time_just_now)
    is Age.Minutes -> stringResource(R.string.time_minutes_ago, age.n)
    is Age.Hours -> stringResource(R.string.time_hours_ago, age.n)
    is Age.Days -> pluralStringResource(R.plurals.time_days_ago, age.n.toInt(), age.n)
}

/** "now", "3m", "2h", "4d": for dense rows. */
@Composable
fun shortAge(at: Instant, now: Instant): String = when (val age = ageOf(at, now)) {
    Age.Now -> stringResource(R.string.time_short_now)
    is Age.Minutes -> stringResource(R.string.time_short_minutes, age.n)
    is Age.Hours -> stringResource(R.string.time_short_hours, age.n)
    is Age.Days -> stringResource(R.string.time_short_days, age.n)
}

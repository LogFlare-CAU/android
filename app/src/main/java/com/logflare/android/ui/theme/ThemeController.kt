package com.logflare.android.ui.theme

import com.logflare.android.data.ThemePreference
import com.logflare.android.data.ThemeRepository
import com.logflare.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The app's theme choice, in memory. [MainActivity][com.logflare.android.MainActivity] draws with
 * [preference]; [set] applies a choice at once and saves it in the background.
 *
 * Saves run one at a time on a single writer, latest choice wins (quick taps skip stale ones).
 * While a choice is not saved yet, stored values are ignored, so an older save landing late
 * can't switch the theme back.
 */
@Singleton
class ThemeController internal constructor(
    stored: Flow<ThemePreference>,
    private val save: suspend (ThemePreference) -> Unit,
    scope: CoroutineScope,
) {
    @Inject
    constructor(repository: ThemeRepository, @ApplicationScope scope: CoroutineScope) :
        this(repository.preference, repository::setPreference, scope)

    private val _preference = MutableStateFlow(ThemePreference.SYSTEM)
    val preference: StateFlow<ThemePreference> = _preference.asStateFlow()

    /** The latest choice not yet seen back from the store; null once it is (or none was made). */
    @Volatile
    private var unsaved: ThemePreference? = null

    private val writes = Channel<ThemePreference>(Channel.CONFLATED)

    init {
        scope.launch {
            stored.collect { value ->
                val pending = unsaved
                if (pending == null) {
                    _preference.value = value
                } else if (value == pending) {
                    unsaved = null
                }
            }
        }
        scope.launch {
            for (choice in writes) {
                try {
                    save(choice)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Not saved: the choice still applies for this run.
                }
                // The latest choice is written (or given up): follow the store again.
                if (unsaved == choice) unsaved = null
            }
        }
    }

    fun set(preference: ThemePreference) {
        unsaved = preference
        _preference.value = preference
        writes.trySend(preference)
    }
}

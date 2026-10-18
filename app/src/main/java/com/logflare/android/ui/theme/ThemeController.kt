package com.logflare.android.ui.theme

import com.logflare.android.data.ThemePreference
import com.logflare.android.data.ThemeRepository
import com.logflare.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The app's theme choice, in memory. [MainActivity][com.logflare.android.MainActivity] draws with
 * [preference]; [set] applies a choice at once and saves it in the background.
 */
@Singleton
class ThemeController internal constructor(
    stored: Flow<ThemePreference>,
    private val save: suspend (ThemePreference) -> Unit,
    private val scope: CoroutineScope,
) {
    @Inject
    constructor(repository: ThemeRepository, @ApplicationScope scope: CoroutineScope) :
        this(repository.preference, repository::setPreference, scope)

    private val _preference = MutableStateFlow(ThemePreference.SYSTEM)
    val preference: StateFlow<ThemePreference> = _preference.asStateFlow()

    init {
        scope.launch { stored.collect { _preference.value = it } }
    }

    fun set(preference: ThemePreference) {
        _preference.value = preference
        scope.launch { save(preference) }
    }
}

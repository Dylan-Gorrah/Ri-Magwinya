package com.rimagwinya.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rimagwinya.app.core.designsystem.theme.ThemeChoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** What the settings screen controls, all of it stored on the phone. */
data class Settings(
    val theme: ThemeChoice = ThemeChoice.System,
    val language: String = "en",
    val orderNotifications: Boolean = true,
    val biometricUnlock: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

/**
 * Preferences, as a Flow.
 *
 * Deliberately not the place for anything the server owns. The language is
 * mirrored to `profiles.language` in Phase 13 so a push arrives in the right
 * language, but the phone's copy is what the UI reads.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val settings: Flow<Settings> = context.dataStore.data
        // A corrupt or unreadable file must not crash the app into a loop.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            Settings(
                theme = runCatching { ThemeChoice.valueOf(prefs[THEME] ?: "System") }
                    .getOrDefault(ThemeChoice.System),
                language = prefs[LANGUAGE] ?: "en",
                orderNotifications = prefs[NOTIFICATIONS] ?: true,
                biometricUnlock = prefs[BIOMETRIC] ?: false,
            )
        }

    suspend fun setTheme(choice: ThemeChoice) = edit { it[THEME] = choice.name }

    suspend fun setLanguage(code: String) = edit { it[LANGUAGE] = code }

    suspend fun setOrderNotifications(on: Boolean) = edit { it[NOTIFICATIONS] = on }

    suspend fun setBiometricUnlock(on: Boolean) = edit { it[BIOMETRIC] = on }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS = booleanPreferencesKey("order_notifications")
        val BIOMETRIC = booleanPreferencesKey("biometric_unlock")
    }
}

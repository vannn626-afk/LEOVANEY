package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.DateFilterPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.leovaneyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "leovaney_user_preferences"
)

data class UserPreferences(
    val userName: String = "van",
    val isOnboardingCompleted: Boolean = false,
    val currencyCode: String = "IDR",
    val defaultDashboardPeriod: DateFilterPeriod = DateFilterPeriod.THIS_MONTH,
    val showQuickAccessHub: Boolean = true,
    val isLoaded: Boolean = false
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
        val KEY_DEFAULT_PERIOD = stringPreferencesKey("default_dashboard_period")
        val KEY_SHOW_QUICK_ACCESS = booleanPreferencesKey("show_quick_access_hub")
        const val DEFAULT_INITIAL_USER_NAME = "van"
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val savedName = prefs[KEY_USER_NAME]
            val onboardingDone = prefs[KEY_ONBOARDING_COMPLETED] ?: false
            val periodName = prefs[KEY_DEFAULT_PERIOD] ?: DateFilterPeriod.THIS_MONTH.name
            val period = runCatching { DateFilterPeriod.valueOf(periodName) }
                .getOrDefault(DateFilterPeriod.THIS_MONTH)
            UserPreferences(
                userName = if (savedName.isNullOrBlank()) DEFAULT_INITIAL_USER_NAME else savedName,
                isOnboardingCompleted = onboardingDone && !savedName.isNullOrBlank(),
                currencyCode = prefs[KEY_CURRENCY_CODE] ?: "IDR",
                defaultDashboardPeriod = period,
                showQuickAccessHub = prefs[KEY_SHOW_QUICK_ACCESS] ?: true,
                isLoaded = true
            )
        }

    suspend fun getSnapshot(): UserPreferences = preferencesFlow.first()

    /**
     * Completes onboarding or updates user name.
     * Rejects blank names after trim.
     */
    suspend fun saveUserNameAndCompleteOnboarding(rawName: String): Result<String> {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Nama tidak boleh kosong."))
        }
        dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = trimmed
            prefs[KEY_ONBOARDING_COMPLETED] = true
        }
        return Result.success(trimmed)
    }

    suspend fun updateUserName(rawName: String): Result<String> {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Nama tidak boleh kosong."))
        }
        dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = trimmed
            prefs[KEY_ONBOARDING_COMPLETED] = true
        }
        return Result.success(trimmed)
    }

    suspend fun updateCurrencyCode(currencyCode: String) {
        val clean = currencyCode.trim().uppercase().ifEmpty { "IDR" }
        dataStore.edit { prefs ->
            prefs[KEY_CURRENCY_CODE] = clean
        }
    }

    suspend fun updateDefaultDashboardPeriod(period: DateFilterPeriod) {
        dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_PERIOD] = period.name
        }
    }

    suspend fun updateShowQuickAccessHub(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_QUICK_ACCESS] = show
        }
    }
}

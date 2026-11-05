package com.gameboost.pro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gameboost_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val AUTO_BOOST = booleanPreferencesKey("auto_boost")
        val PERSISTENT_MONITOR = booleanPreferencesKey("persistent_monitor")
        val THERMAL_ALERTS = booleanPreferencesKey("thermal_alerts")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val BOOST_SCORE = intPreferencesKey("boost_score")
        val TOTAL_BOOSTS = intPreferencesKey("total_boosts")
        val RAM_FREED_MB = intPreferencesKey("ram_freed_mb")
        val LAST_BOOST_TIME = longPreferencesKey("last_boost_time")
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val autoBoostOnLaunch: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.AUTO_BOOST] ?: true
    }

    val persistentMonitor: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.PERSISTENT_MONITOR] ?: true
    }

    val thermalAlerts: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.THERMAL_ALERTS] ?: true
    }

    val hapticFeedback: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.HAPTIC_FEEDBACK] ?: true
    }

    val boostScore: Flow<Int> = context.dataStore.data.map {
        it[Keys.BOOST_SCORE] ?: 72
    }

    val totalBoosts: Flow<Int> = context.dataStore.data.map {
        it[Keys.TOTAL_BOOSTS] ?: 0
    }

    val ramFreedMb: Flow<Int> = context.dataStore.data.map {
        it[Keys.RAM_FREED_MB] ?: 0
    }

    val lastBoostTime: Flow<Long> = context.dataStore.data.map {
        it[Keys.LAST_BOOST_TIME] ?: 0L
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setAutoBoost(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BOOST] = enabled }
    }

    suspend fun setPersistentMonitor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PERSISTENT_MONITOR] = enabled }
    }

    suspend fun setThermalAlerts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.THERMAL_ALERTS] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun recordBoost(scoreGain: Int, ramFreed: Int) {
        context.dataStore.edit { prefs ->
            val currentScore = prefs[Keys.BOOST_SCORE] ?: 72
            prefs[Keys.BOOST_SCORE] = (currentScore + scoreGain).coerceAtMost(100)
            prefs[Keys.TOTAL_BOOSTS] = (prefs[Keys.TOTAL_BOOSTS] ?: 0) + 1
            prefs[Keys.RAM_FREED_MB] = (prefs[Keys.RAM_FREED_MB] ?: 0) + ramFreed
            prefs[Keys.LAST_BOOST_TIME] = System.currentTimeMillis()
        }
    }
}

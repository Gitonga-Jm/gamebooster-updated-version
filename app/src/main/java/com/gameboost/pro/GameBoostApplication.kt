package com.gameboost.pro

import android.app.Application
import android.util.Log
import com.gameboost.pro.data.UserPreferencesRepository

class GameBoostApplication : Application() {
    
    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        // Ultra-fast startup - defer heavy loading
        try {
            preferencesRepository = UserPreferencesRepository(applicationContext)
        } catch (e: Exception) {
            Log.e("GameBoost", "Init failed", e)
        }
    }
}

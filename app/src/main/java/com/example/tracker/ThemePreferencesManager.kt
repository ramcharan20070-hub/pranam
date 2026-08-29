package com.example.tracker

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pulse_theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private fun loadThemeMode(): AppThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.CYBER_BLACK.name)
        return try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.CYBER_BLACK.name)
        } catch (e: Exception) {
            AppThemeMode.CYBER_BLACK
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun toggleTheme() {
        val next = when (_themeMode.value) {
            AppThemeMode.CYBER_BLACK -> AppThemeMode.HIGH_TECH_WHITE
            AppThemeMode.HIGH_TECH_WHITE -> AppThemeMode.CYBER_BLACK
            AppThemeMode.SYSTEM -> AppThemeMode.HIGH_TECH_WHITE
        }
        setThemeMode(next)
    }

    companion object {
        private const val KEY_THEME_MODE = "key_app_theme_mode"
    }
}

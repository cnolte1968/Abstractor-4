package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

object SessionStorage {
    private const val PREFS_NAME = "relevantor_session_prefs"
    private const val KEY_USERNAME = "active_username"
    private const val KEY_TOKEN = "active_token"
    private const val KEY_FAVORITES = "user_favorites_list"

    private val DEFAULT_FAVORITES = listOf(
        "WEB_SUMMARY",
        "KEY_TAKEAWAYS",
        "FACTS_VS_OPINIONS",
        "FRESHNESS_CHECK",
        "RISK_ANALYSIS",
        "GOOGLE_MAPS_ANALYZER"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getFavorites(context: Context): List<String> {
        val raw = getPrefs(context).getString(KEY_FAVORITES, null) ?: return DEFAULT_FAVORITES
        if (raw.isBlank()) return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    fun saveFavorites(context: Context, favorites: List<String>) {
        val raw = favorites.joinToString(",")
        getPrefs(context).edit()
            .putString(KEY_FAVORITES, raw)
            .apply()
    }

    fun saveSession(context: Context, username: String, token: String) {
        getPrefs(context).edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit()
            .clear()
            .apply()
    }

    fun getActiveUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USERNAME, null)
    }

    fun getActiveToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }
}

package com.mallmini.android.data

import android.content.Context
import com.google.gson.Gson
import com.mallmini.android.BuildConfig

class Preferences(context: Context) {

    private val prefs = context.getSharedPreferences("mallmini_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, null).orEmpty().ifBlank { BuildConfig.DEFAULT_API_BASE_URL }
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var token: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var openId: String
        get() = prefs.getString(KEY_OPEN_ID, "android-dev-user").orEmpty()
        set(value) = prefs.edit().putString(KEY_OPEN_ID, value).apply()

    var nickname: String
        get() = prefs.getString(KEY_NICKNAME, "Android 用户").orEmpty()
        set(value) = prefs.edit().putString(KEY_NICKNAME, value).apply()

    fun user(): User? {
        val raw = prefs.getString(KEY_USER, null) ?: return null
        return runCatching { gson.fromJson(raw, User::class.java) }.getOrNull()
    }

    fun saveSession(token: String, user: User) {
        this.token = token
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
    }

    fun clearSession() {
        token = ""
        prefs.edit().remove(KEY_USER).apply()
    }

    private companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_TOKEN = "token"
        const val KEY_USER = "user"
        const val KEY_OPEN_ID = "open_id"
        const val KEY_NICKNAME = "nickname"
    }
}

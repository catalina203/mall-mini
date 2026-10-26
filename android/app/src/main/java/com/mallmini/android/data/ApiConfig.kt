package com.mallmini.android.data

import com.mallmini.android.BuildConfig

object ApiConfig {
    @Volatile
    var baseUrl: String = BuildConfig.DEFAULT_API_BASE_URL

    val origin: String
        get() = baseUrl.substringBefore("/api/v1").trimEnd('/')

    fun resolveImage(url: String): String {
        if (url.isBlank()) {
            return url
        }
        if (url.startsWith("/")) {
            return origin + url
        }
        val localhostPattern = Regex("^https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?")
        return if (localhostPattern.containsMatchIn(url)) {
            localhostPattern.replace(url, origin)
        } else {
            url
        }
    }
}

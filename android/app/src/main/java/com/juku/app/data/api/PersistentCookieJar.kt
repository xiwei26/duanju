package com.juku.app.data.api

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

internal class PersistentCookieJar(context: Context) : CookieJar {
    private val preferences = context.getSharedPreferences("juku_http_cookies", Context.MODE_PRIVATE)

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        val saved = read(url).toMutableList()
        cookies.forEach { incoming ->
            saved.removeAll {
                it.name == incoming.name && it.domain == incoming.domain && it.path == incoming.path
            }
            if (incoming.expiresAt > now) saved += incoming
        }
        write(url, saved)
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val all = read(url)
        val valid = all.filter { it.expiresAt > now }
        if (valid.size != all.size) write(url, valid)
        return valid.filter { it.matches(url) }
    }

    private fun key(url: HttpUrl): String = "${url.scheme}://${url.host}:${url.port}"

    private fun read(url: HttpUrl): List<Cookie> = preferences
        .getStringSet(key(url), emptySet())
        .orEmpty()
        .mapNotNull { Cookie.parse(url, it) }

    private fun write(url: HttpUrl, cookies: List<Cookie>) {
        preferences.edit().putStringSet(key(url), cookies.mapTo(mutableSetOf()) { it.toString() }).apply()
    }
}

package com.dirhamx.display

import android.content.Context

object Prefs {
    private const val FILE = "display_prefs"
    private const val KEY_URL = "agency_url"

    fun getUrl(ctx: Context): String? =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_URL, null)
            .takeIf { !it.isNullOrBlank() }

    fun saveUrl(ctx: Context, url: String) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_URL, url.trim()).apply()

    fun clear(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().clear().apply()
}

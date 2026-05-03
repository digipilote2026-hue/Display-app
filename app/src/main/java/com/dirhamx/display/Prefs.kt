package com.dirhamx.display

import android.content.Context

object Prefs {
    private const val FILE    = "display_prefs"
    private const val KEY_CLIENT = "agency_client"
    private const val BASE_URL   = "https://change-display-demo.web.app/display-cashplus/?client="

    fun getUrl(ctx: Context): String? {
        val client = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_CLIENT, null)
            .takeIf { !it.isNullOrBlank() } ?: return null
        return BASE_URL + client.trim()
    }

    fun getClient(ctx: Context): String? =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_CLIENT, null)
            .takeIf { !it.isNullOrBlank() }

    fun saveClient(ctx: Context, client: String) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_CLIENT, client.trim()).apply()

    fun clear(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().clear().apply()
}

package com.dirhamx.display

import android.content.Context

object Prefs {
    // URL fixe — la page /tv/ gère le login nom + mot de passe
    private const val TV_URL = "https://change-display-demo.web.app/tv/"

    fun getUrl(ctx: Context): String = TV_URL

    // Conservés pour compatibilité mais non utilisés
    fun saveClient(ctx: Context, client: String) {}
    fun getClient(ctx: Context): String? = null
    fun clear(ctx: Context) {}
}

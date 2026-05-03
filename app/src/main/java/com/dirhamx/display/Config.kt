package com.dirhamx.display

object Config {
    // ⚙️ CHANGER UNIQUEMENT CETTE URL POUR CHAQUE AGENCE
    const val START_URL = "https://change-display-demo.web.app/display-cashplus/?client=agence-ziraoui"

    const val BLANK_SCREEN_TIMEOUT_MS = 15_000L   // reload si écran blanc après 15s
    const val ERROR_RETRY_DELAY_MS    = 5_000L    // attente avant retry sur erreur
    const val WATCHDOG_INTERVAL_MS    = 30_000L   // vérification santé toutes les 30s
}

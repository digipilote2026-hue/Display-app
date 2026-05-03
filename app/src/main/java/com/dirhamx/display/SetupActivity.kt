package com.dirhamx.display

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullscreen()

        val root = buildUI()
        setContentView(root)

        // Pré-remplir l'URL existante si reconfiguration
        Prefs.getUrl(this)?.let { urlInput.setText(it) }
    }

    private fun buildUI(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(dp(80), dp(60), dp(80), dp(60))
        }

        // Titre
        root.addView(TextView(this).apply {
            text      = "Configuration de l'affichage"
            textSize  = 28f
            setTextColor(Color.WHITE)
            typeface  = Typeface.DEFAULT_BOLD
            gravity   = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        })

        // Sous-titre
        root.addView(TextView(this).apply {
            text      = "Entrez l'URL de l'agence puis appuyez sur Enregistrer"
            textSize  = 16f
            setTextColor(Color.parseColor("#8B9BB4"))
            gravity   = Gravity.CENTER
            setPadding(0, 0, 0, dp(40))
        })

        // Champ URL
        urlInput = EditText(this).apply {
            hint        = "https://change-display-demo.web.app/display-cashplus/?client=..."
            textSize    = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#4A5568"))
            setBackgroundColor(Color.parseColor("#1A2332"))
            setPadding(dp(20), dp(18), dp(20), dp(18))
            inputType   = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions  = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            isFocusable = true
            isFocusableInTouchMode = true
            maxLines    = 2
        }
        val urlParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.bottomMargin = dp(24) }
        root.addView(urlInput, urlParams)

        // Message de statut
        statusText = TextView(this).apply {
            text      = ""
            textSize  = 14f
            gravity   = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        }
        root.addView(statusText)

        // Bouton Enregistrer
        val btn = Button(this).apply {
            text      = "Enregistrer et démarrer"
            textSize  = 18f
            typeface  = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#00B4CC"))
            setPadding(dp(40), dp(16), dp(40), dp(16))
            isFocusable = true
            setOnClickListener { onSave() }
        }
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).also { it.gravity = Gravity.CENTER_HORIZONTAL }
        root.addView(btn, btnParams)

        // Note bas de page
        root.addView(TextView(this).apply {
            text      = "Appui long 5s sur l'écran pour reconfigurer"
            textSize  = 12f
            setTextColor(Color.parseColor("#4A5568"))
            gravity   = Gravity.CENTER
            setPadding(0, dp(32), 0, 0)
        })

        return root
    }

    private fun onSave() {
        val url = urlInput.text.toString().trim()

        if (url.isEmpty()) {
            showError("L'URL ne peut pas être vide")
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            showError("L'URL doit commencer par https://")
            return
        }

        Prefs.saveUrl(this, url)
        showSuccess("URL enregistrée — démarrage...")

        urlInput.postDelayed({
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }, 800)
    }

    private fun showError(msg: String) {
        statusText.text = "⚠ $msg"
        statusText.setTextColor(Color.parseColor("#FF6B6B"))
    }

    private fun showSuccess(msg: String) {
        statusText.text = "✓ $msg"
        statusText.setTextColor(Color.parseColor("#8DC63F"))
    }

    private fun setFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
              or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setFullscreen()
    }

    override fun onBackPressed() { /* bloquer */ }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

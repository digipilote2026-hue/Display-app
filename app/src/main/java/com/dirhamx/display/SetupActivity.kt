package com.dirhamx.display

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*

class SetupActivity : Activity() {

    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(buildUI())
            setFullscreen()
            Prefs.getUrl(this)?.let { urlInput.setText(it) }
        } catch (e: Exception) {
            // fallback minimal si buildUI échoue
            val tv = TextView(this)
            tv.text = "Erreur: ${e.message}"
            tv.setTextColor(Color.WHITE)
            tv.setBackgroundColor(Color.BLACK)
            setContentView(tv)
        }
    }

    private fun buildUI(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity     = Gravity.CENTER
        root.setBackgroundColor(Color.parseColor("#0D1117"))
        root.setPadding(dp(80), dp(60), dp(80), dp(60))

        val title = TextView(this)
        title.text      = "DG Pilot — Configuration"
        title.textSize  = 24f
        title.setTextColor(Color.WHITE)
        title.typeface  = Typeface.DEFAULT_BOLD
        title.gravity   = Gravity.CENTER
        title.setPadding(0, 0, 0, dp(8))
        root.addView(title)

        val sub = TextView(this)
        sub.text      = "Entrez l'URL de l'agence"
        sub.textSize  = 15f
        sub.setTextColor(Color.parseColor("#8B9BB4"))
        sub.gravity   = Gravity.CENTER
        sub.setPadding(0, 0, 0, dp(32))
        root.addView(sub)

        urlInput = EditText(this)
        urlInput.hint        = "https://change-display-demo.web.app/display-cashplus/?client=..."
        urlInput.textSize    = 14f
        urlInput.setTextColor(Color.WHITE)
        urlInput.setHintTextColor(Color.parseColor("#4A5568"))
        urlInput.setBackgroundColor(Color.parseColor("#1A2332"))
        urlInput.setPadding(dp(20), dp(16), dp(20), dp(16))
        urlInput.inputType   = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        urlInput.maxLines    = 2
        urlInput.isFocusable = true
        urlInput.isFocusableInTouchMode = true
        val urlParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        urlParams.bottomMargin = dp(20)
        root.addView(urlInput, urlParams)

        statusText = TextView(this)
        statusText.text    = ""
        statusText.textSize = 13f
        statusText.gravity = Gravity.CENTER
        statusText.setPadding(0, 0, 0, dp(20))
        root.addView(statusText)

        val btn = Button(this)
        btn.text     = "Enregistrer et démarrer"
        btn.textSize = 16f
        btn.typeface = Typeface.DEFAULT_BOLD
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(Color.parseColor("#00B4CC"))
        btn.setPadding(dp(32), dp(14), dp(32), dp(14))
        btn.setOnClickListener { onSave() }
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnParams.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(btn, btnParams)

        return root
    }

    private fun onSave() {
        val url = urlInput.text.toString().trim()
        if (url.isEmpty()) { showError("URL vide"); return }
        if (!url.startsWith("http")) { showError("URL invalide"); return }
        Prefs.saveUrl(this, url)
        showSuccess("Enregistré — démarrage...")
        urlInput.postDelayed({
            startActivity(Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
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

    override fun onBackPressed() {}

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

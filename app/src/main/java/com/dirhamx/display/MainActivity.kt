package com.dirhamx.display

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private var pageLoaded  = false
    private var isReloading = false

    // ── Accès admin caché ──────────────────────────────────────────────────
    // Méthode 1 : 5 appuis consécutifs sur OK (DPAD_CENTER) en moins de 3s
    private var okPressCount  = 0
    private var lastOkPress   = 0L
    private val OK_COMBO      = 5
    private val OK_WINDOW_MS  = 3_000L

    // Méthode 2 : appui long 5s n'importe où sur l'écran
    private var touchDownTime = 0L
    private val LONG_PRESS_MS = 5_000L

    // ── Watchdog ───────────────────────────────────────────────────────────
    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (!pageLoaded && !isReloading) reloadPage()
            handler.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    private val blankScreenRunnable = Runnable { if (!pageLoaded) reloadPage() }

    companion object {
        private const val BLANK_TIMEOUT_MS   = 15_000L
        private const val ERROR_RETRY_MS     = 5_000L
        private const val WATCHDOG_INTERVAL_MS = 30_000L
    }

    // ── onCreate ───────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pas d'URL configurée → écran de setup
        val url = Prefs.getUrl(this)
        if (url == null) {
            goToSetup()
            return
        }

        keepScreenOn()
        setFullscreen()

        val container = FrameLayout(this)
        setContentView(container)

        webView = WebView(this)
        container.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Détection appui long sur l'écran (télécommande / tactile)
        container.setOnTouchListener { _, event -> handleTouch(event); false }

        configureWebView()
        setWebClients()
        webView.loadUrl(url)

        handler.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS)
    }

    // ── Accès admin caché ──────────────────────────────────────────────────

    /** 5x OK sur la télécommande en moins de 3 secondes */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            val now = System.currentTimeMillis()
            if (now - lastOkPress > OK_WINDOW_MS) okPressCount = 0
            lastOkPress = now
            okPressCount++
            if (okPressCount >= OK_COMBO) {
                okPressCount = 0
                goToSetup()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /** Appui long 5s sur l'écran */
    private fun handleTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchDownTime = System.currentTimeMillis()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (System.currentTimeMillis() - touchDownTime >= LONG_PRESS_MS) {
                    goToSetup()
                }
                touchDownTime = 0L
            }
        }
    }

    private fun goToSetup() {
        startActivity(Intent(this, SetupActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }

    // ── WebView ────────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled                    = true
            domStorageEnabled                    = true
            databaseEnabled                      = true
            mediaPlaybackRequiresUserGesture     = false
            allowFileAccess                      = false
            allowContentAccess                   = false
            setSupportZoom(false)
            builtInZoomControls                  = false
            displayZoomControls                  = false
            loadsImagesAutomatically             = true
            cacheMode                            = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 11; AFMU0) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
        }
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.overScrollMode          = View.OVER_SCROLL_NEVER
        webView.isScrollbarFadingEnabled = true
        webView.scrollBarStyle          = View.SCROLLBARS_INSIDE_OVERLAY
    }

    private fun setWebClients() {
        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                pageLoaded  = false
                isReloading = false
                handler.removeCallbacks(blankScreenRunnable)
                handler.postDelayed(blankScreenRunnable, BLANK_TIMEOUT_MS)
            }

            override fun onPageFinished(view: WebView, url: String) {
                pageLoaded = true
                handler.removeCallbacks(blankScreenRunnable)
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    pageLoaded = false
                    scheduleReload()
                }
            }

            override fun onRenderProcessGone(
                view: WebView, detail: RenderProcessGoneDetail
            ): Boolean {
                pageLoaded = false
                recreateWebView()
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage) = true
        }
    }

    // ── Reload / recovery ─────────────────────────────────────────────────

    private fun reloadPage() {
        if (isReloading) return
        isReloading = true
        val url = Prefs.getUrl(this) ?: return
        handler.post {
            if (isNetworkAvailable()) webView.loadUrl(url)
            else handler.postDelayed({ isReloading = false; reloadPage() }, ERROR_RETRY_MS)
        }
    }

    private fun scheduleReload() {
        if (isReloading) return
        isReloading = true
        handler.postDelayed({ isReloading = false; reloadPage() }, ERROR_RETRY_MS)
    }

    private fun recreateWebView() {
        val parent = webView.parent as? FrameLayout ?: return
        webView.destroy()
        webView = WebView(this)
        parent.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        configureWebView()
        setWebClients()
        Prefs.getUrl(this)?.let { webView.loadUrl(it) }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val net  = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    private fun keepScreenOn() =
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    private fun setFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
              or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
              or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setFullscreen()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        setFullscreen()
        if (!pageLoaded) scheduleReload()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() { /* bloquer */ }
}

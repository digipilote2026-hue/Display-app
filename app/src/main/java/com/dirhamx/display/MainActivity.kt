package com.dirhamx.display

import android.annotation.SuppressLint
import android.app.Activity
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

class MainActivity : Activity() {

    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pageLoaded  = false
    private var isReloading = false

    private var okPressCount = 0
    private var lastOkPress  = 0L

    companion object {
        private const val BLANK_TIMEOUT_MS    = 15_000L
        private const val ERROR_RETRY_MS      = 5_000L
        private const val WATCHDOG_INTERVAL_MS = 30_000L
        private const val OK_COMBO            = 5
        private const val OK_WINDOW_MS        = 3_000L
    }

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (!pageLoaded && !isReloading) reloadPage()
            handler.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    private val blankRunnable = Runnable { if (!pageLoaded) reloadPage() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            keepScreenOn()
            setFullscreen()

            val url = Prefs.getUrl(this)
            if (url == null) {
                goToSetup()
                return
            }

            val container = FrameLayout(this)
            setContentView(container)

            val wv = WebView(this)
            webView = wv
            container.addView(wv, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))

            configureWebView(wv)
            setWebClients(wv)
            wv.loadUrl(url)

            handler.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS)

        } catch (e: Exception) {
            goToSetup()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            val now = System.currentTimeMillis()
            if (now - lastOkPress > OK_WINDOW_MS) okPressCount = 0
            lastOkPress = now
            if (++okPressCount >= OK_COMBO) { okPressCount = 0; goToSetup(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun goToSetup() {
        startActivity(Intent(this, SetupActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled                = true
            domStorageEnabled                = true
            databaseEnabled                  = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess                  = false
            setSupportZoom(false)
            builtInZoomControls              = false
            cacheMode                        = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 11; TV) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
        }
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        wv.overScrollMode = View.OVER_SCROLL_NEVER
    }

    private fun setWebClients(wv: WebView) {
        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                pageLoaded = false; isReloading = false
                handler.removeCallbacks(blankRunnable)
                handler.postDelayed(blankRunnable, BLANK_TIMEOUT_MS)
            }
            override fun onPageFinished(view: WebView, url: String) {
                pageLoaded = true
                handler.removeCallbacks(blankRunnable)
            }
            override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                if (req.isForMainFrame) { pageLoaded = false; scheduleReload() }
            }
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                pageLoaded = false; recreateWebView(); return true
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage) = true
        }
    }

    private fun reloadPage() {
        if (isReloading) return
        isReloading = true
        val url = Prefs.getUrl(this) ?: return
        handler.post {
            if (isNetworkAvailable()) webView?.loadUrl(url)
            else handler.postDelayed({ isReloading = false; reloadPage() }, ERROR_RETRY_MS)
        }
    }

    private fun scheduleReload() {
        if (isReloading) return
        isReloading = true
        handler.postDelayed({ isReloading = false; reloadPage() }, ERROR_RETRY_MS)
    }

    private fun recreateWebView() {
        val wv = webView ?: return
        val parent = wv.parent as? FrameLayout ?: return
        wv.destroy()
        val newWv = WebView(this)
        webView = newWv
        parent.addView(newWv, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        configureWebView(newWv)
        setWebClients(newWv)
        Prefs.getUrl(this)?.let { newWv.loadUrl(it) }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    private fun keepScreenOn() =
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setFullscreen()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        setFullscreen()
        if (!pageLoaded) scheduleReload()
    }

    override fun onPause() { super.onPause(); webView?.onPause() }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webView?.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {}
}

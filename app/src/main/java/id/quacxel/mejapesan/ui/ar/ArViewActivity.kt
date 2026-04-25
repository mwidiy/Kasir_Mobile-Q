package id.quacxel.mejapesan.ui.ar

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class ArViewActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Strategy 1: Edge-to-Edge WITHOUT hiding bars
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Explicit Dark Status Bar Color
        window.statusBarColor = android.graphics.Color.parseColor("#1E1E1E")
        
        val webView = WebView(this)
        setContentView(webView)
        
        // Force WHITE icons (Dark BG = Light Icons OFF)
        androidx.core.view.WindowCompat.getInsetsController(window, webView)?.apply {
            isAppearanceLightStatusBars = false
        }

        // Settings for 3D/AR
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
        }

        // Hardware Acceleration
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                
                // Intercept Deep Link to close activity
                if (url.startsWith("kasir://return")) {
                    finish()
                    return true
                }
                
                return false
            }
        }

        // Load URL
        val url = intent.getStringExtra("KEY_URL")
        if (url != null) {
            webView.loadUrl(url)
        } else {
            finish()
        }
    }
}

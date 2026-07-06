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
                
                // Block intent:// and AR schemes to prevent ERR_UNKNOWN_URL_SCHEME crash during preview
                if (url.startsWith("intent://") || url.contains("arvr.google.com") || url.contains("scene-viewer") || url.startsWith("market://") || url.contains("google.ar.core")) {
                    android.widget.Toast.makeText(this@ArViewActivity, "Mode AR Kamera dinonaktifkan saat pengujian/preview aset 3D", android.widget.Toast.LENGTH_SHORT).show()
                    return true
                }
                
                // Prevent any other non-HTTP/HTTPS/FILE schemes from crashing the WebView
                if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file:///")) {
                    return true
                }
                
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject JS/CSS to disable and hide the AR 3D cube button in preview mode so it cannot be pressed
                view?.evaluateJavascript("""
                    (function() {
                        var style = document.createElement('style');
                        style.innerHTML = 'model-viewer::part(default-ar-button), [slot="ar-button"], .ar-btn { pointer-events: none !important; opacity: 0.3 !important; display: none !important; }';
                        document.head.appendChild(style);
                        var mv = document.querySelector('model-viewer');
                        if (mv) {
                            mv.removeAttribute('ar');
                        }
                    })();
                """.trimIndent(), null)
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

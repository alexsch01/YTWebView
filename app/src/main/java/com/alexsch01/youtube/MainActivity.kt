package com.alexsch01.youtube

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: WebView
    private lateinit var foregroundServiceIntent: Intent

    @SuppressLint("SourceLockedOrientationActivity", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = SCREEN_ORIENTATION_PORTRAIT

        foregroundServiceIntent = Intent(this, ForegroundService::class.java)

        myWebView = findViewById(R.id.webView)
        myWebView.overScrollMode = WebView.OVER_SCROLL_NEVER
        myWebView.isVerticalScrollBarEnabled = false
        myWebView.settings.javaScriptEnabled = true

        myWebView.webViewClient = object : WebViewClient() {
            private val invalids = arrayOf(
                "www.googleadservices.com",
                "support.google.com",
                "wa.me",
                "api.whatsapp.com",
                "www.facebook.com",
                "m.facebook.com",
                "twitter.com",
                "reddit.com",
            )

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                var website = request?.url.toString()
                if (!website.startsWith("https://")) {
                    return true
                }
                website = website.removePrefix("https://")

                if (website.startsWith("www.youtube.com/redirect?")) {
                    val redirectUrl = "https://" + website.split("%3A%2F%2F")[1].split("&v=")[0]
                    view?.context?.startActivity(Intent(
                        Intent.ACTION_VIEW,
                        URLDecoder.decode(redirectUrl, "UTF8").toUri()
                    ))
                    return true
                }

                for (invalidSite in invalids) {
                    if (website.startsWith(invalidSite)) {
                        return true
                    }
                }

                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                runJavascript("""
                    document.querySelector('ad-slot-renderer')?.remove();
                    document.querySelector('ytm-companion-ad-renderer')?.remove();
                    document.querySelector('ytm-watch-metadata-app-promo-renderer')?.remove();
                    document.querySelector('.slim-owner-purchase-button')?.remove();

                    if (document.querySelector('.bottom-sheet-share-item input')) {
                        document.querySelector('.bottom-sheet-share-item input').value =
                            document.querySelector('.bottom-sheet-share-item input').value.split('?si=')[0];
                    }

                    if (document.querySelector('.ad-showing video') && !isNaN(document.querySelector('.ad-showing video').duration)) {
                        document.querySelector('.ad-showing video').currentTime =
                            document.querySelector('.ad-showing video').duration;
                    }
                """)

                return null
            }
        }

        // Workaround for fullscreen videos
        myWebView.webChromeClient = object : WebChromeClient() {
            private val frameLayout = findViewById<CustomFrameLayout>(R.id.customFrameLayout)
            private val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                // get into proper fullscreen mode
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())

                requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE
                frameLayout.addView(view, 1)
            }

            override fun onHideCustomView() {
                requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
                frameLayout.removeViewAt(1)

                // get out of proper fullscreen mode
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        if (intent.dataString == null) {
            myWebView.loadUrl("https://m.youtube.com")
        } else {
            myWebView.loadUrl(intent.dataString!!)
        }
    }

    override fun onPause() {
        super.onPause()
        runJavascript("document.querySelector('video')?.paused") { isPaused ->
            if (isPaused == "paused") {
                startService(foregroundServiceIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stopService(foregroundServiceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(foregroundServiceIntent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.dataString != null) {
            myWebView.loadUrl(intent.dataString!!)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && myWebView.canGoBack()) {
            myWebView.goBack()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    fun runJavascript(script: String, resultCallback: ValueCallback<String>? = null) {
        myWebView.post {
            myWebView.evaluateJavascript(script, resultCallback)
        }
    }
}
